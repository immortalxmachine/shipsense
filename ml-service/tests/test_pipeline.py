"""Tests for the data pipeline (building, cleaning, feature engineering).

These use small inline fixtures — they do NOT require the Olist download — so
they run in CI without the raw dataset present.
"""
import pandas as pd

from pipeline import build_dataset
from pipeline.clean_transform import clean
from pipeline.features import FEATURE_COLUMNS, NUMERIC_FEATURES, engineer
from pipeline.geo import (
    REGIONS, STATE_ADJACENCY, STATE_NAME, STATE_REGION, border_edges, haversine_km,
)


def test_adjacency_is_symmetric_and_connected():
    # Every border is listed on both sides.
    for a, neighbours in STATE_ADJACENCY.items():
        for b in neighbours:
            assert a in STATE_ADJACENCY[b], f"{a}-{b} not symmetric"
    # All 27 federative units are reachable (the graph is connected).
    seen, stack = {"SP"}, ["SP"]
    while stack:
        for nxt in STATE_ADJACENCY[stack.pop()]:
            if nxt not in seen:
                seen.add(nxt)
                stack.append(nxt)
    assert seen == set(STATE_NAME)
    assert len(STATE_REGION) == 27
    assert set(STATE_REGION.values()) == set(REGIONS)


def test_haversine_known_distance():
    # São Paulo centroid <-> Rio de Janeiro centroid is roughly 350-360 km.
    d = haversine_km(-23.15, -47.08, -22.75, -43.16)
    assert 300 < d < 420


def test_border_edges_unique_and_nonempty():
    edges = border_edges()
    assert len(edges) > 0
    # No undirected pair is emitted twice.
    assert len({frozenset(e) for e in edges}) == len(edges)


def _raw_row(**over):
    base = dict(
        order_id="o1", seller_state="SP", customer_state="RJ",
        distance_km=350.0, same_state=0, weight_kg=0.9, freight_value=15.1,
        purchase_dow=1, purchase_month=11, seller_region="SE", cust_region="SE",
        purchase_ts="2017-11-01", estimated_ts="2017-11-20", delivered_ts="2017-11-10",
        estimated_days=19.0, delivery_days=9.0,
    )
    base.update(over)
    return base


def test_clean_drops_invalid_rows_and_reports_counts():
    raw = pd.DataFrame([
        _raw_row(order_id="good"),
        _raw_row(order_id="bad_weight", weight_kg=-1.0),
        _raw_row(order_id="bad_freight", freight_value=-5.0),
        _raw_row(order_id="bad_delivery", delivery_days=0.0),
        _raw_row(order_id="bad_region", cust_region="ZZ"),
        _raw_row(order_id="missing", distance_km=None),
        _raw_row(order_id="good"),  # exact duplicate of the first row
    ])
    cleaned, report = clean(raw)

    assert report.rows_in == 7
    assert report.dropped_missing_fields == 1
    assert report.dropped_bad_weight == 1
    assert report.dropped_bad_freight == 1
    assert report.dropped_bad_delivery == 1
    assert report.dropped_bad_region == 1
    assert report.dropped_duplicates == 1
    # Only the first valid, non-duplicate row survives.
    assert report.rows_out == 1
    assert len(cleaned) == 1


def test_engineer_produces_all_feature_columns():
    df = pd.DataFrame([_raw_row()])
    out = engineer(df)
    for col in FEATURE_COLUMNS:
        assert col in out.columns
    # Exactly one customer-region and one seller-region one-hot column is set.
    assert sum(int(out[f"cust_region_{r}"].iloc[0]) for r in REGIONS) == 1
    assert sum(int(out[f"seller_region_{r}"].iloc[0]) for r in REGIONS) == 1
    assert out["cust_region_SE"].iloc[0] == 1


def test_feature_columns_shape():
    # 6 numeric + 5 customer-region + 5 seller-region one-hots.
    assert NUMERIC_FEATURES[0] == "distance_km"
    assert len(FEATURE_COLUMNS) == len(NUMERIC_FEATURES) + 2 * len(REGIONS)


def test_raw_columns_contract():
    # The builder's output contract is stable (consumed by clean_transform).
    for col in ["distance_km", "weight_kg", "freight_value", "delivery_days",
                "cust_region", "seller_region"]:
        assert col in build_dataset.RAW_COLUMNS
