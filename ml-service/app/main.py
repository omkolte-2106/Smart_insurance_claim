"""
SmartInsure ML microservice (MobileNetV2 Damage Analysis).

This service loads a MobileNetV2-based Keras model for car damage severity analysis.
It includes a custom Keras 3 compatibility layer for models trained in older versions
(e.g., Google Colab) that use custom Lambda operations like 'TrueDivide'.
"""

from fastapi import FastAPI, UploadFile, File
from pydantic import BaseModel, Field
from typing import List, Optional
import os
import numpy as np
import logging
import pandas as pd
import io
import pickle
from PIL import Image
from ultralytics import YOLO

try:
    import tensorflow as tf
    from tensorflow.keras.models import load_model
    from tensorflow.keras.layers import Layer
    TF_AVAILABLE = True
except ImportError:
    TF_AVAILABLE = False

logger = logging.getLogger("uvicorn.error")

app = FastAPI(title="SmartInsure ML Service", version="0.1.0")

# Load models
MODEL_PATH = os.path.join(os.path.dirname(__file__), "..", "models", "car_damage_severity_model.h5")
YOLO_MODEL_PATH = os.path.join(os.path.dirname(__file__), "..", "models", "best.pt")

severity_model = None
yolo_model = None

if TF_AVAILABLE and os.path.exists(MODEL_PATH):
    try:
        # Keras 3 Compatibility Classes for legacy custom layers
        class TrueDivide(Layer):
            def __init__(self, **kwargs):
                super().__init__(**kwargs)
            def call(self, x, *args, **kwargs):
                divisor = 127.5
                if args: divisor = args[0]
                elif 'y' in kwargs: divisor = kwargs['y']
                return x / divisor
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
        # compile=False is used if you only need the model for inference
        severity_model = load_model(MODEL_PATH, custom_objects=custom_objs, compile=False)
        logger.info(f"Successfully loaded severity model from {MODEL_PATH}")
    except Exception as e:
        logger.error(f"Failed to load severity model: {e}")

if os.path.exists(YOLO_MODEL_PATH):
    try:
        yolo_model = YOLO(YOLO_MODEL_PATH)
        logger.info(f"Successfully loaded YOLO model from {YOLO_MODEL_PATH}")
    except Exception as e:
        logger.error(f"Failed to load YOLO model: {e}")
else:
    if not TF_AVAILABLE:
        logger.warning("TensorFlow is not installed. Models cannot be loaded.")
    if not os.path.exists(MODEL_PATH):
        logger.warning(f"Model file not found at {MODEL_PATH}.")

if os.path.exists(YOLO_MODEL_PATH):
    try:
        yolo_model = YOLO(YOLO_MODEL_PATH)
        logger.info(f"Successfully loaded YOLO model from {YOLO_MODEL_PATH}")
    except Exception as e:
        logger.error(f"Failed to load YOLO model: {e}")

# --- Preprocessing Helpers ---

def preprocess_image(image_bytes: bytes, target_size=(224, 224)):
    """
    Standard MobileNetV2 preprocessing:
    1. Resize to target_size
    2. Convert to RGB
    3. Normalize (handled by the model's TrueDivide layer if integrated, 
       otherwise usually [x/127.5 - 1])
    """
    img = Image.open(io.BytesIO(image_bytes))
    img = img.convert('RGB')
    img = img.resize(target_size)
    
    img_array = np.array(img).astype(np.float32)
    # Most models expect a batch dimension: (1, 224, 224, 3)
    img_array = np.expand_dims(img_array, axis=0)
    
    return img_array

# --- Payload Models ---

class DocumentVerificationPayload(BaseModel):
    claimPublicId: str
    documentCount: int = Field(ge=0)


class FraudPayload(BaseModel):
    claimPublicId: str


class DamagePayload(BaseModel):
    claimPublicId: str
    imageCount: int = Field(ge=0)


class PayoutPayload(BaseModel):
    claimPublicId: str
    severity: float
    sumInsured: float


class CustomerRankingPayload(BaseModel):
    topFraction: float = Field(default=0.15, ge=0.01, le=0.5)
    customers: Optional[List[dict]] = None

# --- Endpoints ---

@app.get("/health")
def health():
    return {"status": "ok", "model_loaded": severity_model is not None}


@app.post("/ml/analyze-damage")
async def analyze_damage(file: UploadFile = File(...)):
    """
    Performs real 'scanning' of an uploaded vehicle image.
    Returns severity score and label based on the 3-class softmax output.
    
    Model architecture:
      Input(224x224x3) -> [RandomFlip, RandomRotation, RandomZoom] (data aug, inactive during inference)
      -> TrueDivide(/127.5) -> Subtract(-1.0) -> MobileNetV2 -> GlobalAvgPool -> Dropout -> Dense(3, softmax)
    
    Output classes (softmax): [class_0, class_1, class_2]
    We interpret them as: [MINOR, MODERATE, SEVERE]
    
    Severity score is computed as a weighted average:
      score = 0.0 * P(MINOR) + 0.5 * P(MODERATE) + 1.0 * P(SEVERE)
    This gives a continuous 0-1 value that reflects the full probability distribution.
    """
    if severity_model is None:
        return {"error": "Model not loaded", "severityScore": 0.5, "severityLabel": "MODERATE (Fallback)"}
    
    try:
        content = await file.read()
        processed_img = preprocess_image(content)
        
        # Inference
        prediction = severity_model.predict(processed_img, verbose=0)
        probs = prediction[0]  # Shape: (3,) — softmax probabilities
        
        logger.info(f"=== DAMAGE ANALYSIS for {file.filename} ===")
        logger.info(f"  Raw softmax: MINOR={probs[0]:.4f}, MODERATE={probs[1]:.4f}, SEVERE={probs[2]:.4f}")
        logger.info(f"  Input stats: mean={np.mean(processed_img):.2f}, std={np.std(processed_img):.2f}")
        
        # Weighted severity score: 0*P(minor) + 0.5*P(moderate) + 1.0*P(severe)
        severity_score = 0.0 * float(probs[0]) + 0.5 * float(probs[1]) + 1.0 * float(probs[2])
        
        # Determine dominant class
        class_idx = int(np.argmax(probs))
        class_labels = ["MINOR", "MODERATE", "SEVERE"]
        dominant_label = class_labels[class_idx]
        
        # Final label based on the weighted score (more nuanced than just argmax)
        if severity_score >= 0.6:
            final_label = "SEVERE"
        elif severity_score >= 0.3:
            final_label = "MODERATE"
        else:
            final_label = "MINOR"
        
        logger.info(f"  Weighted severity score: {severity_score:.4f} -> {final_label}")

        return {
            "severityScore": round(severity_score, 4),
            "severityLabel": final_label,
            "modelVersion": "car_damage_severity_model.h5 v1.2 (Weighted Softmax)",
            "fileName": file.filename,
            "classBreakdown": {
                "minor": round(float(probs[0]), 4),
                "moderate": round(float(probs[1]), 4),
                "severe": round(float(probs[2]), 4)
            }
        }
    except Exception as e:
        logger.error(f"Error during image analysis: {e}")
        return {"error": str(e), "severityScore": 0.5, "severityLabel": "ERROR"}


@app.post("/ml/damage-severity")
def damage_severity(payload: DamagePayload):
    """
    Legacy endpoint for count-based estimation.
    Now attempts to use the model on dummy data to confirm integration.
    """
    label = "MODERATE"
    model_version = "placeholder-0.1"
    
    if severity_model is not None:
        try:
            input_shape = severity_model.input_shape
            shape = tuple([1 if d is None else d for d in input_shape])
            dummy_input = np.random.rand(*shape).astype(np.float32)
            
            prediction = severity_model.predict(dummy_input, verbose=0)
            severity = float(np.mean(prediction)) 
            severity = min(0.99, max(0.01, severity))
            model_version = "car_damage_severity_model.h5 v1.0 (Simulation)"
        except Exception as e:
            logger.error(f"Error running inference simulation: {e}")
            severity = min(0.95, 0.35 + 0.05 * payload.imageCount)
    else:
        severity = min(0.95, 0.35 + 0.05 * payload.imageCount)
        model_version = "fallback-mode"

    if severity > 0.7: label = "SEVERE"
    elif severity > 0.4: label = "MODERATE"
    else: label = "MINOR"

    return {"severityScore": round(severity, 4), "severityLabel": label, "modelVersion": model_version}


@app.post("/ml/document-verification")
def document_verification(payload: DocumentVerificationPayload):
    clarity = min(0.95, 0.7 + 0.02 * payload.documentCount)
    return {
        "clarityScore": clarity,
        "validityScore": 0.85,
        "fraudSuspicion": 0.15,
        "notes": "Verified via SmartInsure Simulation",
    }


@app.post("/ml/fraud-detection")
def fraud_detection(payload: FraudPayload):
    return {"fraudScore": 0.12, "reasons": ["Historical consistency: High"]}


@app.post("/ml/part-damage-detection")
async def part_damage_detection(file: UploadFile = File(...)):
    """
    Real part detection using the integrated YOLOv8 model.
    """
    if yolo_model is None:
        return {"detectedParts": ["BUMPER (Fallback)"], "severity": "MEDIUM", "modelVersion": "fallback"}
    
    try:
        content = await file.read()
        img = Image.open(io.BytesIO(content))
        
        # Run inference
        results = yolo_model.predict(img, conf=0.25)
        
        detected_parts = []
        for result in results:
            for box in result.boxes:
                class_id = int(box.cls[0])
                label = yolo_model.names[class_id]
                # Format label for display (e.g. front-bumper-dent -> Front Bumper Dent)
                formatted_label = label.replace("-", " ").title()
                detected_parts.append(formatted_label)
        
        # Unique parts only
        unique_parts = list(set(detected_parts))
        
        return {
            "detectedParts": unique_parts,
            "severity": "HIGH" if len(unique_parts) > 3 else "MEDIUM",
            "modelVersion": "yolov8-damage-detection-v1.0",
            "detectionCount": len(detected_parts)
        }
    except Exception as e:
        logger.error(f"Error during part detection: {e}")
        return {"error": str(e), "detectedParts": [], "severity": "UNKNOWN"}


@app.post("/ml/payout-estimation")
def payout_estimation(payload: PayoutPayload):
    recommended = round(payload.sumInsured * payload.severity * 0.35, 2)
    return {"recommendedPayout": recommended, "currency": "INR", "rationale": "Severity Model Multiplier"}


@app.post("/ml/customer-ranking")
def customer_ranking(payload: CustomerRankingPayload):
    ranked: List[RankedCustomer] = []
    if payload.customers:
        sorted_rows = sorted(payload.customers, key=lambda c: c.get("loyaltyScore", 0), reverse=True)
        take = max(1, int(len(sorted_rows) * payload.topFraction))
        for idx, row in enumerate(sorted_rows[:take]):
            cid = str(row.get("id"))
            ranked.append(
                RankedCustomer(
                    externalCustomerKey=cid,
                    percentileRank=95 - idx * 2,
                    suggestedDiscountPercent=8 + idx,
                    rationale="placeholder loyalty ordering",
                )
            )
    return {"rankedCustomers": [r.model_dump() for r in ranked]}

@app.post("/ml/churn-prediction")
async def churn_prediction(file: UploadFile = File(...)):
    return {"error": "Churn model disintegrated as per damage analysis focus."}
