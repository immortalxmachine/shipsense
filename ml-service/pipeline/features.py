"""Shared feature engineering.

Both training (via ``clean_transform.py``) and serving (``app/predictor.py``)
build features through this module, so the feature order and encoding are
*identical* in both paths. This avoids train/serve skew — a classic and costly
ML bug.

All features are derived from real Olist order attributes (see
``pipeline/build_dataset.py``): the geographic distance between the seller's and
customer's states, parcel weight, freight paid, whether it ships within one
state, the purchase weekday/month, and the IBGE macro-region of each side.
"""
from __future__ import annotations

import pandas as pd

from pipeline.geo import REGIONS

# Numeric features, used as-is.
NUMERIC_FEATURES = [
    "distance_km",
    "weight_kg",
    "freight_value",
    "same_state",
    "purchase_dow",
    "purchase_month",
]

# The exact columns the model is trained on, in order (numeric + one-hot region).
FEATURE_COLUMNS = NUMERIC_FEATURES + [
    f"cust_region_{r}" for r in REGIONS
] + [
    f"seller_region_{r}" for r in REGIONS
]


def engineer(df: pd.DataFrame) -> pd.DataFrame:
    """Return a copy of ``df`` with all :data:`FEATURE_COLUMNS` present.

    - ``same_state`` is cast to 0/1.
    - ``cust_region`` / ``seller_region`` are one-hot encoded into a stable set
      of columns (one per :data:`pipeline.geo.REGIONS` entry), so an unseen or
      missing region simply yields all-zeros rather than a new column.
    """
    out = df.copy()
    out["same_state"] = out["same_state"].astype(int)

    for r in REGIONS:
        out[f"cust_region_{r}"] = (out["cust_region"] == r).astype(int)
        out[f"seller_region_{r}"] = (out["seller_region"] == r).astype(int)

    return out


def single_row_frame(payload: dict) -> pd.DataFrame:
    """Build a 1-row DataFrame (with :data:`FEATURE_COLUMNS`) from an API payload."""
    df = pd.DataFrame([payload])
    df = engineer(df)
    return df[FEATURE_COLUMNS]
