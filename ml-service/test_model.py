import os
import numpy as np
import tensorflow as tf
from tensorflow.keras.models import load_model

# Custom object handler for 'TrueDivide' (as discovered during integration)
def TrueDivide(x, y=127.5, **kwargs):
    return x / y

MODEL_PATH = "models/car_damage_severity_model.h5"

def test_model_inference():
    if not os.path.exists(MODEL_PATH):
        print(f"Error: Model not found at {MODEL_PATH}")
        return

    print(f"Loading model from {MODEL_PATH}...")
    try:
        # Load without compiling for faster inference test
        model = load_model(
            MODEL_PATH, 
            custom_objects={'TrueDivide': TrueDivide}, 
            compile=False
        )
        print("Model loaded successfully!")
        
        # Determine input shape
        input_shape = model.input_shape
        print(f"Model input shape: {input_shape}")
        
        # Generate dummy input (batch size 1)
        # Replace None with 1 if batch size is dynamic
        shape = tuple([1 if d is None else d for d in input_shape])
        dummy_input = np.random.rand(*shape).astype(np.float32)
        
        print(f"Running inference on dummy input of shape {shape}...")
        prediction = model.predict(dummy_input)
        
        print("\n--- Inference Results ---")
        print(f"Raw Prediction Shape: {prediction.shape}")
        print(f"Prediction Output: {prediction}")
        
        # Example interpretation (assuming softmax/classification)
        if prediction.shape[1] > 1:
            predicted_class = np.argmax(prediction)
            print(f"Predicted Class Index: {predicted_class}")
        
        print("\nModel is functional!")

    except Exception as e:
        print(f"\nFailed to test model: {e}")
        print("\nTip: This often happens if the Keras/TensorFlow versions differ.")

if __name__ == "__main__":
    test_model_inference()
