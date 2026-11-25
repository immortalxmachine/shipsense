"""Wraps the trained model: builds features, predicts delay, derives on-time prob."""
from __future__ import annotations

import math
import os

import joblib

from app.schemas import PredictRequest, PredictResponse
from pipeline.features import single_row_frame


def on_time_probability(delay_hours: float, grace: float = 3.0, scale: float = 4.0) -> float:
    """Map a predicted delay to a probability of on-time delivery.

    A logistic curve: ~0.5 when the predicted delay equals the ``grace`` window,
    rising towards 1 for early/on-time parcels and falling towards 0 as the
    predicted delay grows. (A deterministic, explainable alternative to training
    a separate classifier.)
    """
    z = (delay_hours - grace) / scale
    z = max(-50.0, min(50.0, z))          # guard against overflow
    return 1.0 / (1.0 + math.exp(z))


class Predictor:
    def __init__(self, model, feature_columns: list[str]):
        self.model = model
        self.feature_columns = feature_columns

    def predict(self, req: PredictRequest) -> PredictResponse:
        row = single_row_frame(req.model_dump())[self.feature_columns]
        delay = float(self.model.predict(row)[0])
        return PredictResponse(
            predicted_delay_hours=round(delay, 2),
            on_time_probability=round(on_time_probability(delay), 4),
        )


def load_predictor(model_dir: str) -> Predictor:
    """Load the joblib bundle saved by train.py."""
    bundle = joblib.load(os.path.join(model_dir, "model.joblib"))
    return Predictor(bundle["model"], bundle["feature_columns"])
