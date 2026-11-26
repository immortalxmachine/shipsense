"""ETL step: clean + validate + engineer features for the raw Olist shipment data.

Reads the raw per-order CSV produced by ``build_dataset.py``, drops invalid or
incomplete rows (logging how many of each), then writes an analysis-ready
dataset to Parquet (and CSV). Kept modular so each stage reads like a real data
pipeline.

Rows are *dropped*, never imputed — the dataset is large enough that we never
need to invent a missing value.

Run (from ml-service/):
    python -m pipeline.clean_transform --in data/raw_shipments.csv \
        --out-parquet data/clean_shipments.parquet --out-csv data/clean_shipments.csv
"""
from __future__ import annotations

import argparse
import logging
import os
from dataclasses import dataclass, asdict

import pandas as pd

from pipeline.features import engineer
from pipeline.geo import REGIONS

logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")
log = logging.getLogger("clean_transform")

# Essential real fields a usable row must carry.
REQUIRED = ["distance_km", "weight_kg", "freight_value",
            "cust_region", "seller_region", "delivery_days"]

# Guard against physically impossible / data-entry outliers.
MAX_DELIVERY_DAYS = 100.0


@dataclass
class CleaningReport:
    rows_in: int = 0
    dropped_missing_fields: int = 0
    dropped_bad_weight: int = 0
    dropped_bad_freight: int = 0
    dropped_bad_delivery: int = 0
    dropped_bad_region: int = 0
    dropped_duplicates: int = 0
    rows_out: int = 0


def load_raw(path: str) -> pd.DataFrame:
    return pd.read_csv(path)


def clean(df: pd.DataFrame) -> tuple[pd.DataFrame, CleaningReport]:
    """Validate and drop bad rows, returning the clean frame and a report."""
    report = CleaningReport(rows_in=len(df))

    # 1. Essential fields must be present.
    bad = df[REQUIRED].isna().any(axis=1)
    report.dropped_missing_fields = int(bad.sum())
    df = df[~bad]

    # 2. Weight must be non-negative (0 is allowed; some products have no weight).
    bad = df["weight_kg"] < 0
    report.dropped_bad_weight = int(bad.sum())
    df = df[~bad]

    # 3. Freight must be non-negative.
    bad = df["freight_value"] < 0
    report.dropped_bad_freight = int(bad.sum())
    df = df[~bad]

    # 4. Delivery time must be positive and within a sane bound.
    bad = (df["delivery_days"] <= 0) | (df["delivery_days"] > MAX_DELIVERY_DAYS)
    report.dropped_bad_delivery = int(bad.sum())
    df = df[~bad]

    # 5. Regions must be known IBGE macro-regions.
    bad = (~df["cust_region"].isin(REGIONS)) | (~df["seller_region"].isin(REGIONS))
    report.dropped_bad_region = int(bad.sum())
    df = df[~bad]

    # 6. Exact-duplicate rows add no information.
    before = len(df)
    df = df.drop_duplicates()
    report.dropped_duplicates = before - len(df)

    report.rows_out = len(df)
    return df.reset_index(drop=True), report


def transform(df: pd.DataFrame) -> pd.DataFrame:
    """Add the engineered model features (one-hot regions, int flags)."""
    return engineer(df)


def run(raw_path: str, out_parquet: str, out_csv: str | None = None) -> CleaningReport:
    df = load_raw(raw_path)
    df, report = clean(df)
    df = transform(df)

    os.makedirs(os.path.dirname(out_parquet) or ".", exist_ok=True)
    df.to_parquet(out_parquet, index=False)
    if out_csv:
        df.to_csv(out_csv, index=False)

    log.info("Cleaning report: %s", asdict(report))
    log.info("Wrote %d clean rows to %s", report.rows_out, out_parquet)
    return report


def main() -> None:
    parser = argparse.ArgumentParser(description="Clean + transform raw shipment data.")
    parser.add_argument("--in", dest="raw", default="data/raw_shipments.csv")
    parser.add_argument("--out-parquet", default="data/clean_shipments.parquet")
    parser.add_argument("--out-csv", default="data/clean_shipments.csv")
    args = parser.parse_args()
    run(args.raw, args.out_parquet, args.out_csv)


if __name__ == "__main__":
    main()
