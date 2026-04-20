"""
Deep inspection: check what the Sequential layer does and test with real images.
"""
import os, sys, json
import numpy as np
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'

import tensorflow as tf
from tensorflow.keras.models import load_model, Model
from tensorflow.keras.layers import Layer
import h5py

MODEL_PATH = "models/car_damage_severity_model.h5"

class TrueDivide(Layer):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
    def call(self, x, *args, **kwargs):
        return x / 127.5
    @classmethod
    def from_config(cls, config):
        return cls(**config)

class Subtract(Layer):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
    def call(self, x, *args, **kwargs):
        return x - 1.0
    @classmethod
    def from_config(cls, config):
        return cls(**config)

model = load_model(MODEL_PATH, custom_objects={'TrueDivide': TrueDivide, 'Subtract': Subtract}, compile=False)

# ===== Inspect "Sequential" layer (layer 1) — it's embedding a sub-model =====
print("=" * 70)
print("Inspecting 'sequential' sub-model (layer 1)")
print("=" * 70)
seq = model.layers[1]
print(f"Type: {type(seq)}")
print(f"Name: {seq.name}")

if hasattr(seq, 'layers'):
    print(f"Sub-layers ({len(seq.layers)}):")
    for i, sl in enumerate(seq.layers):
        print(f"  [{i}] {sl.__class__.__name__:25s} name={sl.name}")
        if hasattr(sl, 'get_config'):
            cfg = sl.get_config()
            # Print relevant config (not all)
            for key in ['height', 'width', 'scale', 'offset', 'factor', 'mode']:
                if key in cfg:
                    print(f"       {key}={cfg[key]}")

# ===== Check the h5 file for Sequential sub-layer details =====
print("\n" + "=" * 70)
print("Inspecting h5 config for 'sequential' sub-model")
print("=" * 70)

with h5py.File(MODEL_PATH, 'r') as f:
    config_raw = f.attrs.get('model_config')
    if isinstance(config_raw, bytes):
        config_raw = config_raw.decode('utf-8')
    config = json.loads(config_raw)
    
    layers = config.get('config', {}).get('layers', [])
    for layer in layers:
        if layer.get('config', {}).get('name') == 'sequential':
            sub_config = layer.get('config', {})
            sub_layers = sub_config.get('layers', [])
            print(f"Sequential has {len(sub_layers)} sub-layers:")
            for sl in sub_layers:
                print(f"  class={sl.get('class_name')}, name={sl.get('config',{}).get('name')}")
                cfg = sl.get('config', {})
                for key in ['height', 'width', 'scale', 'offset', 'factor', 'mode', 'rate',
                            'seed', 'fill_mode', 'fill_value', 'interpolation']:
                    if key in cfg:
                        print(f"    {key}={cfg[key]}")

# ===== Compare model output classes =====
print("\n" + "=" * 70)
print("Inspecting Dense (output) layer")
print("=" * 70)
dense_layer = model.layers[-1]
print(f"Name: {dense_layer.name}")
print(f"Units: {dense_layer.get_config().get('units')}")
print(f"Activation: {dense_layer.get_config().get('activation')}")
weights = dense_layer.get_weights()
if weights:
    print(f"Kernel shape: {weights[0].shape}")
    print(f"Bias: {weights[1]}")

# ===== Test: feed images through the Sequential pre-processing first =====
print("\n" + "=" * 70)
print("Testing: Output of Sequential layer for different inputs")
print("=" * 70)

seq_model = Model(inputs=model.input, outputs=seq.output)

black = np.zeros((1, 224, 224, 3), dtype=np.float32)
white = np.full((1, 224, 224, 3), 255.0, dtype=np.float32)
rand1 = np.random.rand(1, 224, 224, 3).astype(np.float32) * 255.0
rand2 = np.random.rand(1, 224, 224, 3).astype(np.float32) * 255.0

for name, img in [("BLACK", black), ("WHITE", white), ("RANDOM1", rand1), ("RANDOM2", rand2)]:
    out = seq_model.predict(img, verbose=0)
    print(f"  {name:10s} -> seq output mean={np.mean(out):.4f}, std={np.std(out):.4f}, min={np.min(out):.4f}, max={np.max(out):.4f}")

# ===== THE KEY TEST: Is Sequential applying random augmentation? =====
print("\n" + "=" * 70)
print("KEY TEST: Does Sequential give SAME output for SAME input? (random augmentation check)")
print("=" * 70)

fixed_input = np.full((1, 224, 224, 3), 100.0, dtype=np.float32)
results = []
for i in range(3):
    out = seq_model.predict(fixed_input, verbose=0)
    results.append(np.mean(out))
    print(f"  Run {i+1}: mean={np.mean(out):.6f}")

if abs(results[0] - results[1]) < 1e-5 and abs(results[1] - results[2]) < 1e-5:
    print("  -> Sequential is DETERMINISTIC (no random augmentation during inference)")
else:
    print("  -> Sequential has RANDOM augmentation! Results differ per run.")
    print("     This could cause issues but shouldn't make all outputs identical.")

print("\nDone.")
