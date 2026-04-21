"""
SmartInsure ML microservice (MobileNetV2 Damage Analysis).

FIX SUMMARY (v1.3):
  BUG 1 — /ml/damage-severity fed random dummy data to the model → always ~0.50.
           Fixed: endpoint is now a pure count-based heuristic. Real image analysis
           must go through /ml/analyze-damage (which the Java backend already does
           when a VEHICLE_DAMAGE_PHOTO document exists).

  BUG 2 — /ml/analyze-damage did NOT apply MobileNetV2 normalisation.
           preprocess_image() left pixel values in [0,255]; the backbone received
           wildly out-of-range inputs and collapsed to a near-constant output.
           Fixed: keras preprocess_input() (x/127.5 − 1) is now applied explicitly.

  BUG 3 — YOLO model was loaded twice (duplicate block lines 86-91).
           Fixed: single load block.

  BUG 4 — /ml/payout-estimation used a hardcoded 0.35 multiplier → same payout
           (e.g. 65 000) for every claim with the same sumInsured.
           Fixed: graduated multiplier — MINOR 10-20%, MODERATE 20-45%, SEVERE 45-75%.
"""

from fastapi import FastAPI, UploadFile, File
from pydantic import BaseModel, Field
from typing import List, Optional
import os
import numpy as np
import logging
import io
from PIL import Image
from ultralytics import YOLO

try:
    import tensorflow as tf
    from tensorflow.keras.models import load_model
    from tensorflow.keras.layers import Layer
    from tensorflow.keras.applications.mobilenet_v2 import preprocess_input as mobilenet_preprocess
    TF_AVAILABLE = True
except ImportError:
    TF_AVAILABLE = False
    mobilenet_preprocess = None

logger = logging.getLogger("uvicorn.error")

app = FastAPI(title="SmartInsure ML Service", version="1.3.0")

# ── Model paths ────────────────────────────────────────────────────────────────
_BASE           = os.path.dirname(__file__)
MODEL_PATH      = os.path.join(_BASE, "..", "models", "car_damage_severity_model.h5")
YOLO_MODEL_PATH = os.path.join(_BASE, "..", "models", "best.pt")

severity_model = None
yolo_model     = None

# ── Load Keras severity model ──────────────────────────────────────────────────
if TF_AVAILABLE and os.path.exists(MODEL_PATH):
    try:
        class TrueDivide(Layer):
            def __init__(self, **kwargs):
                super().__init__(**kwargs)
            def call(self, x, *args, **kwargs):
                divisor = kwargs.get('y', args[0] if args else 127.5)
                return x / divisor
            @classmethod
            def from_config(cls, config):
                return cls(**config)

        class Subtract(Layer):
            def __init__(self, **kwargs):
                super().__init__(**kwargs)
            def call(self, x, *args, **kwargs):
                val = kwargs.get('y', args[0] if args else 1.0)
                return x - val
            @classmethod
            def from_config(cls, config):
                return cls(**config)

        severity_model = load_model(
            MODEL_PATH,
            custom_objects={'TrueDivide': TrueDivide, 'Subtract': Subtract},
            compile=False,
        )
        logger.info("Severity model loaded — input=%s output=%s",
                    severity_model.input_shape, severity_model.output_shape)
    except Exception as e:
        logger.error("Failed to load severity model: %s", e)
else:
    if not TF_AVAILABLE:
        logger.warning("TensorFlow not installed — severity model unavailable.")
    elif not os.path.exists(MODEL_PATH):
        logger.warning("Model not found at %s", MODEL_PATH)

# ── Load YOLO model (ONCE — BUG 3 FIX) ───────────────────────────────────────
if os.path.exists(YOLO_MODEL_PATH):
    try:
        yolo_model = YOLO(YOLO_MODEL_PATH)
        logger.info("YOLO model loaded from %s", YOLO_MODEL_PATH)
    except Exception as e:
        logger.error("Failed to load YOLO model: %s", e)
else:
    logger.warning("YOLO model not found at %s", YOLO_MODEL_PATH)


# ── Preprocessing (BUG 2 FIX) ─────────────────────────────────────────────────

def preprocess_image(image_bytes: bytes, target_size=(224, 224)) -> np.ndarray:
    """
    Load image → resize → apply MobileNetV2 normalisation ([0,255] → [-1,1]).

    The original code skipped normalisation entirely, so MobileNetV2 received
    raw pixel values in [0,255] and produced near-constant outputs.
    Applying preprocess_input() here is safe regardless of whether the model's
    own TrueDivide/Subtract layers fire correctly after .h5 reload.
    """
    img = Image.open(io.BytesIO(image_bytes)).convert('RGB').resize(target_size)
    arr = np.array(img, dtype=np.float32)    # (H, W, 3)  range [0, 255]
    arr = np.expand_dims(arr, axis=0)        # (1, H, W, 3)

    if mobilenet_preprocess is not None:
        arr = mobilenet_preprocess(arr)      # → [-1, 1]
    else:
        arr = (arr / 127.5) - 1.0           # manual fallback

    return arr


# ── Payout helper (BUG 4 FIX) ─────────────────────────────────────────────────

def compute_payout(severity_score: float, sum_insured: float) -> float:
    """
    Graduated payout multiplier so different severities produce different amounts.

    MINOR    (score < 0.30) → 10–20 % of sum insured
    MODERATE (0.30–0.60)   → 20–45 %
    SEVERE   (> 0.60)      → 45–75 %
    """
    if severity_score >= 0.60:
        multiplier = 0.45 + 0.30 * ((severity_score - 0.60) / 0.40)
    elif severity_score >= 0.30:
        multiplier = 0.20 + 0.25 * ((severity_score - 0.30) / 0.30)
    else:
        multiplier = 0.10 + 0.10 * (severity_score / 0.30)

    multiplier = min(multiplier, 0.90)
    return round(sum_insured * multiplier, 2)


# ── Pydantic models ────────────────────────────────────────────────────────────

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


# ── Shared inference helper ────────────────────────────────────────────────────

def _run_severity_inference(arr: np.ndarray, filename: str = "unknown") -> dict:
    if severity_model is None:
        raise RuntimeError("Severity model is not loaded")

    prediction = severity_model.predict(arr, verbose=0)
    probs = prediction[0]   # (3,) softmax — [MINOR, MODERATE, SEVERE]

    logger.info("Inference [%s]  MINOR=%.4f  MODERATE=%.4f  SEVERE=%.4f",
                filename, probs[0], probs[1], probs[2])
    logger.info("Input stats: mean=%.3f  std=%.3f  min=%.3f  max=%.3f",
                np.mean(arr), np.std(arr), np.min(arr), np.max(arr))

    score = 0.0 * float(probs[0]) + 0.5 * float(probs[1]) + 1.0 * float(probs[2])

    if score >= 0.60:
        label = "SEVERE"
    elif score >= 0.30:
        label = "MODERATE"
    else:
        label = "MINOR"

    logger.info("Weighted score=%.4f → %s", score, label)

    return {
        "severityScore":  round(score, 4),
        "severityLabel":  label,
        "classBreakdown": {
            "minor":    round(float(probs[0]), 4),
            "moderate": round(float(probs[1]), 4),
            "severe":   round(float(probs[2]), 4),
        },
    }


# ── Endpoints ──────────────────────────────────────────────────────────────────

@app.get("/health")
def health():
    return {
        "status":                "ok",
        "severity_model_loaded": severity_model is not None,
        "yolo_model_loaded":     yolo_model is not None,
    }


@app.post("/ml/analyze-damage")
async def analyze_damage(file: UploadFile = File(...)):
    """
    Primary damage analysis — accepts a real vehicle image and returns a severity
    score from the MobileNetV2 model.  (BUG 2 FIX: normalisation now applied.)
    """
    if severity_model is None:
        return {
            "error":         "Severity model not loaded",
            "severityScore": 0.5,
            "severityLabel": "MODERATE (Fallback – model missing)",
            "modelVersion":  "fallback",
        }

    try:
        content = await file.read()
        arr     = preprocess_image(content)
        result  = _run_severity_inference(arr, filename=file.filename or "upload")
        result["modelVersion"] = "car_damage_severity_model.h5 v1.3 (MobileNetV2)"
        result["fileName"]     = file.filename
        return result
    except Exception as e:
        logger.error("Error in analyze_damage: %s", e)
        return {"error": str(e), "severityScore": 0.5, "severityLabel": "ERROR"}


@app.post("/ml/damage-severity")
def damage_severity(payload: DamagePayload):
    """
    Legacy count-based endpoint.  (BUG 1 FIX: no longer feeds random data to
    the model — that always produced ~0.50.  This is now a pure heuristic used
    only when no image has been uploaded yet.  Real analysis uses /ml/analyze-damage.)
    """
    score = min(0.90, 0.30 + 0.07 * payload.imageCount)

    if score >= 0.60:
        label = "SEVERE"
    elif score >= 0.30:
        label = "MODERATE"
    else:
        label = "MINOR"

    logger.info("Legacy damage-severity (imageCount=%d) → score=%.4f label=%s",
                payload.imageCount, score, label)

    return {
        "severityScore": round(score, 4),
        "severityLabel": label,
        "modelVersion":  "heuristic-count-v1.3 (upload an image to use the real model)",
    }


@app.post("/ml/document-verification")
def document_verification(payload: DocumentVerificationPayload):
    clarity = min(0.95, 0.70 + 0.02 * payload.documentCount)
    return {
        "clarityScore":   round(clarity, 4),
        "validityScore":  0.85,
        "fraudSuspicion": 0.15,
        "notes":          "Verified via SmartInsure Document Check",
    }


@app.post("/ml/fraud-detection")
def fraud_detection(payload: FraudPayload):
    return {"fraudScore": 0.12, "reasons": ["Historical consistency: High"]}


@app.post("/ml/part-damage-detection")
async def part_damage_detection(file: UploadFile = File(...)):
    """Real part detection using the integrated YOLOv8 model."""
    if yolo_model is None:
        return {
            "detectedParts": ["BUMPER (Fallback)"],
            "severity":      "MEDIUM",
            "modelVersion":  "fallback – YOLO model not loaded",
        }

    try:
        content = await file.read()
        img     = Image.open(io.BytesIO(content))
        results = yolo_model.predict(img, conf=0.25)

        detected = []
        for result in results:
            for box in result.boxes:
                class_id = int(box.cls[0])
                label    = yolo_model.names[class_id]
                detected.append(label.replace("-", " ").title())

        unique = list(set(detected))
        return {
            "detectedParts":  unique,
            "severity":       "HIGH" if len(unique) > 3 else "MEDIUM",
            "modelVersion":   "yolov8-damage-detection-v1.0",
            "detectionCount": len(detected),
        }
    except Exception as e:
        logger.error("Error in part_damage_detection: %s", e)
        return {"error": str(e), "detectedParts": [], "severity": "UNKNOWN"}


@app.post("/ml/payout-estimation")
def payout_estimation(payload: PayoutPayload):
    """
    (BUG 4 FIX) Graduated payout — different severity scores now produce
    meaningfully different payout amounts instead of always multiplying by 0.35.
    """
    recommended = compute_payout(payload.severity, float(payload.sumInsured))

    if payload.severity >= 0.60:
        rationale = "SEVERE damage — high-tier payout (45–75 % of sum insured)"
    elif payload.severity >= 0.30:
        rationale = "MODERATE damage — mid-tier payout (20–45 % of sum insured)"
    else:
        rationale = "MINOR damage — low-tier payout (10–20 % of sum insured)"

    logger.info("Payout: severity=%.4f  sumInsured=%.2f → recommended=%.2f",
                payload.severity, float(payload.sumInsured), recommended)

    return {
        "recommendedPayout": recommended,
        "currency":          "INR",
        "rationale":         rationale,
    }


@app.post("/ml/customer-ranking")
def customer_ranking(payload: CustomerRankingPayload):
    ranked: List[dict] = []
    if payload.customers:
        sorted_rows = sorted(
            payload.customers,
            key=lambda c: c.get("loyaltyScore", 0),
            reverse=True,
        )
        take = max(1, int(len(sorted_rows) * payload.topFraction))
        for idx, row in enumerate(sorted_rows[:take]):
            cid = str(row.get("id", idx))
            ranked.append({
                "externalCustomerKey":      cid,
                "percentileRank":           max(0, 95 - idx * 2),
                "suggestedDiscountPercent": 8 + idx,
                "rationale":               "loyalty-score ordering",
            })
    return {"rankedCustomers": ranked}


@app.post("/ml/churn-prediction")
async def churn_prediction(file: UploadFile = File(...)):
    return {"error": "Churn model not implemented in this service version."}