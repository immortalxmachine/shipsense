"""Wraps the trained model: builds features, predicts delivery days, derives on-time prob."""
from __future__ import annotations

import math
import os

import joblib

from app.schemas import PredictRequest, PredictResponse
from pipeline.features import single_row_frame


def on_time_probability(predicted_days: float, estimated_days: float, scale: float = 3.0) -> float:
    """Probability the parcel arrives on or before the carrier's estimate.

    A logistic curve on the slack between the estimate and the prediction:
    ~0.5 when the predicted delivery time equals the estimate, rising towards 1
    when we predict delivery comfortably inside the estimate and falling towards
    0 when we predict it blowing past the estimate. A deterministic, explainable
    alternative to training a separate classifier.
    """
    z = (predicted_days - estimated_days) / scale
    z = max(-50.0, min(50.0, z))          # guard against overflow
    return 1.0 / (1.0 + math.exp(z))


class Predictor:
    def __init__(self, model, feature_columns: list[str]):
        self.model = model
        self.feature_columns = feature_columns

    def predict(self, req: PredictRequest) -> PredictResponse:
        row = single_row_frame(req.model_dump())[self.feature_columns]
        days = float(self.model.predict(row)[0])
        prob = (
            round(on_time_probability(days, req.estimated_days), 4)
            if req.estimated_days is not None else None
        )
        return PredictResponse(
            predicted_delivery_days=round(days, 2),
            on_time_probability=prob,
        )


def load_predictor(model_dir: str) -> Predictor:
    """Load the joblib bundle saved by train.py."""
    bundle = joblib.load(os.path.join(model_dir, "model.joblib"))
    return Predictor(bundle["model"], bundle["feature_columns"])
