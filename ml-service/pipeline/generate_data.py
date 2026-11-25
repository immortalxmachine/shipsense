"""Generate a realistic *synthetic* dataset of historical shipments.

The data is SYNTHETIC (clearly labelled as such in the README). A transparent
rule generates the ``delay_hours`` target from the features plus Gaussian noise,
so a model has a real signal to learn — but the rule is non-trivial (interactions
between distance, service tier, weather and hub load), so a linear baseline is
beaten by a gradient-boosted model.

A small fraction of deliberately "dirty" rows is injected so the downstream ETL
step (``clean_transform.py``) has something real to clean.

Run:
    python -m pipeline.generate_data --rows 50000 --out data/raw_shipments.csv
"""
from __future__ import annotations

import argparse
import math
import os

import numpy as np
import pandas as pd

# 20 Indian hubs (code -> lat, lng) — mirrors db/seed.sql.
HUBS = {
    "DEL": (28.6139, 77.2090), "MUM": (19.0760, 72.8777), "CHN": (13.0827, 80.2707),
    "KOL": (22.5726, 88.3639), "BLR": (12.9716, 77.5946), "HYD": (17.3850, 78.4867),
    "PUN": (18.5204, 73.8567), "AMD": (23.0225, 72.5714), "JAI": (26.9124, 75.7873),
    "LKO": (26.8467, 80.9462), "NAG": (21.1458, 79.0882), "BHO": (23.2599, 77.4126),
    "SUR": (21.1702, 72.8311), "KAN": (26.4499, 80.3319), "IND": (22.7196, 75.8577),
    "CBE": (11.0168, 76.9558), "COK": (9.9312, 76.2673), "VSK": (17.6868, 83.2185),
    "GUW": (26.1445, 91.7362), "PAT": (25.5941, 85.1376),
}
HUB_CODES = list(HUBS.keys())

SERVICE_TYPES = ["STANDARD", "EXPRESS", "PRIORITY"]
SERVICE_PROBS = [0.55, 0.30, 0.15]
# Priority is quickest, standard slowest (hours added to the base delay).
SERVICE_DELAY = {"STANDARD": 4.0, "EXPRESS": 1.0, "PRIORITY": -1.0}


def haversine_km(a: tuple[float, float], b: tuple[float, float]) -> float:
    """Great-circle distance in km between two (lat, lng) points."""
    r = 6371.0
    lat1, lon1, lat2, lon2 = map(math.radians, [a[0], a[1], b[0], b[1]])
    dlat, dlon = lat2 - lat1, lon2 - lon1
    h = math.sin(dlat / 2) ** 2 + math.cos(lat1) * math.cos(lat2) * math.sin(dlon / 2) ** 2
    return 2 * r * math.asin(math.sqrt(h))


def generate(n_rows: int, seed: int = 42) -> pd.DataFrame:
    rng = np.random.default_rng(seed)

    # Pick distinct origin/destination hub pairs.
    origin_idx = rng.integers(0, len(HUB_CODES), n_rows)
    dest_idx = rng.integers(0, len(HUB_CODES), n_rows)
    # Nudge any origin==dest to a different hub.
    same = origin_idx == dest_idx
    dest_idx[same] = (dest_idx[same] + 1) % len(HUB_CODES)

    origin = np.array(HUB_CODES)[origin_idx]
    dest = np.array(HUB_CODES)[dest_idx]

    # Road distance ~ 1.3 x great-circle distance (+ small noise).
    base_km = np.array([haversine_km(HUBS[o], HUBS[d]) for o, d in zip(origin, dest)])
    distance_km = np.round(base_km * 1.3 * rng.normal(1.0, 0.05, n_rows), 1)
    distance_km = np.clip(distance_km, 50, None)

    weight_kg = np.round(rng.gamma(shape=2.0, scale=3.0, size=n_rows) + 0.1, 2)
    service_type = rng.choice(SERVICE_TYPES, size=n_rows, p=SERVICE_PROBS)
    day_of_week = rng.integers(0, 7, n_rows)             # 0=Mon .. 6=Sun
    month = rng.integers(1, 13, n_rows)                  # 1..12
    is_festival_season = np.isin(month, [10, 11])        # Oct/Nov (Diwali)
    is_monsoon = np.isin(month, [6, 7, 8, 9])            # SW monsoon
    hub_load = rng.integers(10, 101, n_rows)             # congestion proxy

    # --- Delay rule (hours): linear terms PLUS non-linear interactions -----
    # The non-linear parts (a load threshold, weather compounding, and a
    # distance x load product) are things a plain linear model cannot represent,
    # so a gradient-boosted model has real signal to beat the baseline on.
    svc_delay = np.array([SERVICE_DELAY[s] for s in service_type])
    weekend = np.isin(day_of_week, [5, 6]).astype(float)

    base = 0.0035 * distance_km + 0.04 * weight_kg + svc_delay + 2.0 * weekend

    # Congestion is non-linear: mild below 75% load, sharp penalty above it.
    load_effect = np.where(hub_load > 75, 8.0 + 0.15 * (hub_load - 75), 0.04 * hub_load)

    # Weather interaction: festival AND monsoon together compound the disruption.
    weather = np.select(
        [is_festival_season & is_monsoon,
         is_festival_season & ~is_monsoon,
         ~is_festival_season & is_monsoon],
        [15.0, 6.0, 5.0],
        default=0.0,
    )

    # Distance x load coupling: long hauls through busy hubs degrade faster.
    interaction = 0.00006 * distance_km * hub_load

    delay = base + load_effect + weather + interaction + rng.normal(0.0, 2.5, n_rows)
    delay_hours = np.round(delay, 2)

    df = pd.DataFrame({
        "origin_hub": origin,
        "dest_hub": dest,
        "distance_km": distance_km,
        "weight_kg": weight_kg,
        "service_type": service_type,
        "day_of_week": day_of_week,
        "month": month,
        "is_festival_season": is_festival_season,
        "is_monsoon": is_monsoon,
        "hub_load": hub_load,
        "delay_hours": delay_hours,
    })

    df = _inject_dirty_rows(df, rng)
    return df


def _inject_dirty_rows(df: pd.DataFrame, rng: np.random.Generator, frac: float = 0.02) -> pd.DataFrame:
    """Corrupt ~``frac`` of rows so the ETL step has real cleaning to do."""
    n_bad = int(len(df) * frac)
    if n_bad == 0:
        return df
    idx = rng.choice(df.index, size=n_bad, replace=False)
    third = n_bad // 3
    df.loc[idx[:third], "weight_kg"] = -1.0                      # impossible weight
    df.loc[idx[third:2 * third], "service_type"] = None          # missing service
    df.loc[idx[2 * third:], "distance_km"] = 0.0                 # impossible distance
    return df


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate synthetic shipment data.")
    parser.add_argument("--rows", type=int, default=50_000)
    parser.add_argument("--out", type=str, default="data/raw_shipments.csv")
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()

    df = generate(args.rows, args.seed)
    os.makedirs(os.path.dirname(args.out) or ".", exist_ok=True)
    df.to_csv(args.out, index=False)
    print(f"Wrote {len(df):,} rows to {args.out} "
          f"({df['service_type'].isna().sum()} null-service dirty rows injected)")


if __name__ == "__main__":
    main()
