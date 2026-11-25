"""FastAPI app exposing the delay-prediction model.

Endpoints:
  GET  /health   -> liveness + whether a model is loaded
  GET  /metrics  -> the metrics.json produced by train.py
  POST /predict  -> predicted delay hours + on-time probability

Run locally:
    uvicorn app.main:app --reload --port 8000
"""
from __future__ import annotations

import json
import os
from contextlib import asynccontextmanager

from fastapi import Depends, FastAPI, HTTPException

from app.predictor import Predictor, load_predictor
from app.schemas import HealthResponse, PredictRequest, PredictResponse

MODEL_DIR = os.getenv("MODEL_DIR", "models")

# Simple in-process state populated at startup.
_state: dict = {"predictor": None, "metrics": None}


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Load the model (if trained) and metrics once, at startup.
    try:
        _state["predictor"] = load_predictor(MODEL_DIR)
        print(f"[ml] model loaded from {MODEL_DIR}")
    except Exception as exc:  # noqa: BLE001 - startup should not crash if model absent
        print(f"[ml] model NOT loaded ({exc}); run train.py and restart")

    metrics_path = os.path.join(MODEL_DIR, "metrics.json")
    if os.path.exists(metrics_path):
        with open(metrics_path, encoding="utf-8") as f:
            _state["metrics"] = json.load(f)
    yield


app = FastAPI(
    title="ShipSense ML Service",
    description="Delay prediction for parcel shipments",
    version="0.1.0",
    lifespan=lifespan,
)


def get_predictor() -> Predictor:
    """Dependency that yields the loaded predictor (503 if not trained yet)."""
    predictor = _state["predictor"]
    if predictor is None:
        raise HTTPException(status_code=503, detail="Model not loaded. Run train.py first.")
    return predictor


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(status="ok", model_loaded=_state["predictor"] is not None)


@app.get("/metrics")
def metrics() -> dict:
    if _state["metrics"] is None:
        raise HTTPException(status_code=404, detail="metrics.json not found; run train.py")
    return _state["metrics"]


@app.post("/predict", response_model=PredictResponse)
def predict(req: PredictRequest, predictor: Predictor = Depends(get_predictor)) -> PredictResponse:
    return predictor.predict(req)
