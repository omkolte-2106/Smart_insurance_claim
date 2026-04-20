"""
Diagnostic script to understand why the model gives the same output for different images.
Tests multiple aspects: model architecture, preprocessing, layer behavior, and inference.
"""
import os
import sys
import numpy as np

os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'  # Suppress TF info logs

import tensorflow as tf
from tensorflow.keras.models import load_model
from tensorflow.keras.layers import Layer
import h5py
import json

MODEL_PATH = "models/car_damage_severity_model.h5"

# ===== Step 1: Inspect the h5 file structure =====
print("=" * 70)
print("STEP 1: Inspecting model h5 file structure")
print("=" * 70)

with h5py.File(MODEL_PATH, 'r') as f:
    config_raw = f.attrs.get('model_config')
    if config_raw is not None:
        if isinstance(config_raw, bytes):
            config_raw = config_raw.decode('utf-8')
        config = json.loads(config_raw)
        
        # Find all layers
        layers = config.get('config', {}).get('layers', [])
        print(f"Total layers in model: {len(layers)}")
        
        # Find custom/lambda layers
        for i, layer in enumerate(layers):
            cls = layer.get('class_name', '')
            name = layer.get('config', {}).get('name', '')
            if cls in ('TrueDivide', 'Subtract', 'Lambda'):
                print(f"  Layer {i}: class={cls}, name={name}")
                inbound = layer.get('inbound_nodes', [])
                print(f"    inbound_nodes: {json.dumps(inbound, indent=2)}")
    else:
        print("WARNING: No model_config found in h5 file!")

# ===== Step 2: Load the model =====
print("\n" + "=" * 70)
print("STEP 2: Loading model with custom objects")
print("=" * 70)

class TrueDivide(Layer):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
    def call(self, x, *args, **kwargs):
        divisor = 127.5
        if args: divisor = args[0]
        elif 'y' in kwargs: divisor = kwargs['y']
        result = x / divisor
        return result
    @classmethod
    def from_config(cls, config):
        return cls(**config)

class Subtract(Layer):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
    def call(self, x, *args, **kwargs):
        val = 1.0
        if args: val = args[0]
        elif 'y' in kwargs: val = kwargs['y']
        return x - val
    @classmethod
    def from_config(cls, config):
        return cls(**config)

custom_objs = {'TrueDivide': TrueDivide, 'Subtract': Subtract}
model = load_model(MODEL_PATH, custom_objects=custom_objs, compile=False)
print(f"Model loaded successfully!")
print(f"Input shape: {model.input_shape}")
print(f"Output shape: {model.output_shape}")

# ===== Step 3: Check layer graph / intermediate outputs =====
print("\n" + "=" * 70)
print("STEP 3: Checking model layers (first 15 and last 5)")
print("=" * 70)

for i, layer in enumerate(model.layers[:15]):
    try:
        oshape = layer.output_shape
    except AttributeError:
        oshape = "N/A"
    print(f"  [{i}] {layer.__class__.__name__:20s}  name={layer.name:30s}  output_shape={oshape}")
print("  ...")
for i, layer in enumerate(model.layers[-5:]):
    idx = len(model.layers) - 5 + i
    try:
        oshape = layer.output_shape
    except AttributeError:
        oshape = "N/A"
    print(f"  [{idx}] {layer.__class__.__name__:20s}  name={layer.name:30s}  output_shape={oshape}")

# ===== Step 4: Test with VERY different inputs =====
print("\n" + "=" * 70)
print("STEP 4: Testing inference with dramatically different inputs")
print("=" * 70)

h, w = model.input_shape[1], model.input_shape[2]

# Create test images with very different pixel values
test_images = {
    "All BLACK (0s)":       np.zeros((1, h, w, 3), dtype=np.float32),
    "All WHITE (255s)":     np.full((1, h, w, 3), 255.0, dtype=np.float32),
    "All MID-GRAY (128s)":  np.full((1, h, w, 3), 128.0, dtype=np.float32),
    "Random NOISE":         np.random.rand(1, h, w, 3).astype(np.float32) * 255.0,
    "Random NOISE (diff)":  np.random.rand(1, h, w, 3).astype(np.float32) * 255.0,
    "Red image":            np.zeros((1, h, w, 3), dtype=np.float32),
    "Blue image":           np.zeros((1, h, w, 3), dtype=np.float32),
}
# Fill red and blue
test_images["Red image"][0, :, :, 0] = 255.0
test_images["Blue image"][0, :, :, 2] = 255.0

print(f"\nAll inputs shape: (1, {h}, {w}, 3), dtype=float32, RAW pixel values [0-255]")
print("-" * 70)

for name, img in test_images.items():
    pred = model.predict(img, verbose=0)
    print(f"\n  {name:25s} -> prediction: {pred}")
    print(f"  {'':25s}    input mean={np.mean(img):.2f}, std={np.std(img):.2f}")

# ===== Step 5: Test with MobileNetV2 preprocessing applied BEFORE the model =====
print("\n" + "=" * 70)
print("STEP 5: Testing with MobileNetV2 preprocess_input applied BEFORE model")
print("=" * 70)
print("(If the model already has preprocessing layers, this might double-process)")

from tensorflow.keras.applications.mobilenet_v2 import preprocess_input

for name, img in test_images.items():
    preprocessed = preprocess_input(img.copy())  # Normalizes to [-1, 1]
    pred = model.predict(preprocessed, verbose=0)
    print(f"\n  {name:25s} -> prediction: {pred}")
    print(f"  {'':25s}    preprocessed mean={np.mean(preprocessed):.2f}, std={np.std(preprocessed):.2f}")

# ===== Step 6: Check intermediate layer output after TrueDivide/Subtract =====
print("\n" + "=" * 70)
print("STEP 6: Checking intermediate outputs after preprocessing layers")
print("=" * 70)

# Find TrueDivide and Subtract layers
preprocessing_layers = [l for l in model.layers if isinstance(l, (TrueDivide, Subtract))]
if preprocessing_layers:
    for pl in preprocessing_layers:
        print(f"  Found preprocessing layer: {pl.name} ({pl.__class__.__name__})")
    
    # Build a sub-model to inspect outputs after preprocessing
    last_preprocess = preprocessing_layers[-1]
    from tensorflow.keras.models import Model
    debug_model = Model(inputs=model.input, outputs=last_preprocess.output)
    
    white_img = np.full((1, h, w, 3), 255.0, dtype=np.float32)
    black_img = np.zeros((1, h, w, 3), dtype=np.float32)
    
    white_out = debug_model.predict(white_img, verbose=0)
    black_out = debug_model.predict(black_img, verbose=0)
    
    print(f"\n  After preprocessing layers:")
    print(f"    WHITE input -> mean={np.mean(white_out):.4f}, std={np.std(white_out):.4f}, min={np.min(white_out):.4f}, max={np.max(white_out):.4f}")
    print(f"    BLACK input -> mean={np.mean(black_out):.4f}, std={np.std(black_out):.4f}, min={np.min(black_out):.4f}, max={np.max(black_out):.4f}")
    
    if np.allclose(white_out, black_out, atol=1e-3):
        print("\n  *** PROBLEM DETECTED: Preprocessing layers produce IDENTICAL output for different inputs! ***")
        print("  *** The TrueDivide/Subtract layers are likely NOT connected correctly in the graph. ***")
    else:
        print("\n  OK: Preprocessing layers produce DIFFERENT outputs for different inputs.")
else:
    print("  No TrueDivide/Subtract layers found in the model.")

print("\n" + "=" * 70)
print("DIAGNOSIS COMPLETE")
print("=" * 70)
