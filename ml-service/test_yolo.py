import sys
import os
from ultralytics import YOLO
import torch

# Try to look in models/best directory
model_path = 'models/best'
print(f"Checking path: {os.path.abspath(model_path)}")

try:
    print("Attempting to load with YOLO...")
    # YOLO normally expects a .pt file, but let's see if it works with the folder
    model = YOLO(model_path, task='detect')
    print("SUCCESS: Loaded as YOLO")
    print(f"Classes: {model.names}")
except Exception as e:
    print(f"YOLO Load Failed: {e}")
    
    try:
        print("\nAttempting to load with torch.load...")
        # PyTorch can sometimes load folder-based models
        data = torch.load(model_path, map_location='cpu')
        print("SUCCESS: Loaded with torch.load")
        if isinstance(data, dict):
            print(f"Keys: {data.keys()}")
            if 'model' in data:
                print("Found 'model' key")
        else:
            print(f"Data type: {type(data)}")
    except Exception as e2:
        print(f"torch.load Failed: {e2}")
