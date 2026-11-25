"""Tests for the data pipeline (generation, cleaning, feature engineering)."""
import pandas as pd

from pipeline import generate_data
from pipeline.clean_transform import clean
from pipeline.features import FEATURE_COLUMNS, SERVICE_TYPES, engineer


def test_generate_has_expected_shape_and_columns():
    df = generate_data.generate(n_rows=500, seed=1)
    assert len(df) == 500
    for col in ["origin_hub", "dest_hub", "distance_km", "weight_kg",
                "service_type", "delay_hours"]:
        assert col in df.columns
    # Origin and destination are never the same hub.
    assert (df["origin_hub"] != df["dest_hub"]).all()


def test_generate_injects_dirty_rows():
    df = generate_data.generate(n_rows=1000, seed=2)
    # ~2% dirty rows were injected: some null service, negative weight, zero distance.
    assert df["service_type"].isna().sum() > 0
    assert (df["weight_kg"] <= 0).sum() > 0
    assert (df["distance_km"] <= 0).sum() > 0


def test_clean_drops_invalid_rows_and_reports_counts():
    raw = pd.DataFrame({
        "origin_hub": ["DEL", "MUM", "CHN", "BLR", "DEL"],
        "dest_hub":   ["CHN", "KOL", "DEL", "HYD", "CHN"],
        "distance_km": [2000.0, 1800.0, 0.0, 500.0, 2000.0],   # row 3 bad (0)
        "weight_kg":   [2.0, -1.0, 3.0, 4.0, 2.0],             # row 2 bad (<=0)
        "service_type": ["EXPRESS", "STANDARD", "PRIORITY", None, "EXPRESS"],  # row 4 bad (None)
        "day_of_week": [1, 2, 3, 4, 1],
        "month":       [11, 7, 5, 3, 11],
        "is_festival_season": [True, False, False, False, True],
        "is_monsoon":  [False, True, False, False, False],
        "hub_load":    [50, 60, 40, 30, 50],
        "delay_hours": [10.0, 12.0, 5.0, 8.0, 10.0],           # row 5 duplicates row 1
    })
    cleaned, report = clean(raw)

    assert report.rows_in == 5
    assert report.dropped_bad_distance == 1
    assert report.dropped_bad_weight == 1
    assert report.dropped_bad_service == 1
    assert report.dropped_duplicates == 1
    # Only the first valid, non-duplicate row survives.
    assert report.rows_out == 1
    assert len(cleaned) == 1


def test_engineer_produces_all_feature_columns():
    df = pd.DataFrame({
        "distance_km": [100.0], "weight_kg": [2.0],
        "service_type": ["EXPRESS"], "day_of_week": [3], "month": [11],
        "is_festival_season": [True], "is_monsoon": [False], "hub_load": [50],
    })
    out = engineer(df)
    for col in FEATURE_COLUMNS:
        assert col in out.columns
    # Exactly one of the one-hot service columns is set.
    one_hot_sum = sum(int(out[f"service_{s}"].iloc[0]) for s in SERVICE_TYPES)
    assert one_hot_sum == 1
    assert out["service_EXPRESS"].iloc[0] == 1
