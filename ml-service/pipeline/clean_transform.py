"""ETL step: clean + validate + engineer features for the raw shipment data.

Reads the raw CSV, drops/repairs invalid rows (logging how many of each), then
writes an analysis-ready dataset to Parquet (and CSV). Kept modular so each
stage reads like a real data pipeline.

Run:
    python -m pipeline.clean_transform --in data/raw_shipments.csv \
        --out-parquet data/clean_shipments.parquet --out-csv data/clean_shipments.csv
"""
from __future__ import annotations

import argparse
import logging
import os
from dataclasses import dataclass, asdict

import pandas as pd

from pipeline.features import SERVICE_TYPES, engineer

logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")
log = logging.getLogger("clean_transform")


@dataclass
class CleaningReport:
    rows_in: int = 0
    dropped_bad_weight: int = 0
    dropped_bad_distance: int = 0
    dropped_bad_service: int = 0
    dropped_bad_calendar: int = 0
    dropped_duplicates: int = 0
    rows_out: int = 0


def load_raw(path: str) -> pd.DataFrame:
    return pd.read_csv(path)


def clean(df: pd.DataFrame) -> tuple[pd.DataFrame, CleaningReport]:
    """Validate and drop bad rows, returning the clean frame and a report."""
    report = CleaningReport(rows_in=len(df))

    # 1. Weight must be positive.
    bad = df["weight_kg"] <= 0
    report.dropped_bad_weight = int(bad.sum())
    df = df[~bad]

    # 2. Distance must be positive.
    bad = df["distance_km"] <= 0
    report.dropped_bad_distance = int(bad.sum())
    df = df[~bad]

    # 3. Service type must be one of the known tiers (catches nulls + typos).
    bad = ~df["service_type"].isin(SERVICE_TYPES)
    report.dropped_bad_service = int(bad.sum())
    df = df[~bad]

    # 4. Calendar fields must be in range.
    bad = (~df["month"].between(1, 12)) | (~df["day_of_week"].between(0, 6))
    report.dropped_bad_calendar = int(bad.sum())
    df = df[~bad]

    # 5. Exact-duplicate rows add no information.
    before = len(df)
    df = df.drop_duplicates()
    report.dropped_duplicates = before - len(df)

    report.rows_out = len(df)
    return df.reset_index(drop=True), report


def transform(df: pd.DataFrame) -> pd.DataFrame:
    """Add the engineered model features (one-hot service, int flags)."""
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
