"""Shared feature engineering.

Both training (``train.py`` via ``clean_transform.py``) and serving
(``app/predictor.py``) build features through this module, so the feature
order and encoding are *identical* in both paths. This avoids train/serve
skew — a classic and costly ML bug.
"""
from __future__ import annotations

import pandas as pd

# The three service tiers, one-hot encoded in a fixed order.
SERVICE_TYPES = ["STANDARD", "EXPRESS", "PRIORITY"]

# The exact columns the model is trained on, in order.
FEATURE_COLUMNS = [
    "distance_km",
    "weight_kg",
    "day_of_week",
    "month",
    "is_festival_season",
    "is_monsoon",
    "hub_load",
    "service_STANDARD",
    "service_EXPRESS",
    "service_PRIORITY",
]


def engineer(df: pd.DataFrame) -> pd.DataFrame:
    """Return a copy of ``df`` with all :data:`FEATURE_COLUMNS` present.

    - Booleans are cast to 0/1.
    - ``service_type`` is one-hot encoded into three binary columns.
    """
    out = df.copy()

    out["is_festival_season"] = out["is_festival_season"].astype(int)
    out["is_monsoon"] = out["is_monsoon"].astype(int)

    # One-hot encode service type with a stable set of columns.
    for svc in SERVICE_TYPES:
        out[f"service_{svc}"] = (out["service_type"] == svc).astype(int)

    return out


def single_row_frame(payload: dict) -> pd.DataFrame:
    """Build a 1-row DataFrame (with :data:`FEATURE_COLUMNS`) from an API payload."""
    df = pd.DataFrame([payload])
    df = engineer(df)
    return df[FEATURE_COLUMNS]
