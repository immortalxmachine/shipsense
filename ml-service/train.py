"""Train delay-prediction models and write real metrics.

Trains two models on the cleaned data:
  * a LinearRegression **baseline**, and
  * a GradientBoostingRegressor (the model we actually serve),
then reports MAE and RMSE for both on a held-out test set, plus the gradient
model's feature importances. The model is saved with joblib and all metrics are
written to ``models/metrics.json`` (every number in the README comes from here).

If the cleaned dataset is missing, the raw-generation + ETL steps run first, so
the whole thing works with one command:
    python train.py
"""
from __future__ import annotations

import argparse
import json
import os
from datetime import datetime, timezone

import joblib
import pandas as pd
from sklearn.ensemble import GradientBoostingRegressor
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_absolute_error, root_mean_squared_error
from sklearn.model_selection import train_test_split

from pipeline import clean_transform, generate_data
from pipeline.features import FEATURE_COLUMNS

TARGET = "delay_hours"


def ensure_clean_data(clean_path: str, raw_path: str, rows: int) -> None:
    """Generate raw data + run ETL if the cleaned dataset does not exist yet."""
    if os.path.exists(clean_path):
        return
    if not os.path.exists(raw_path):
        print(f"[train] raw data missing -> generating {rows:,} synthetic rows")
        df = generate_data.generate(rows)
        os.makedirs(os.path.dirname(raw_path) or ".", exist_ok=True)
        df.to_csv(raw_path, index=False)
    print("[train] cleaned data missing -> running ETL")
    clean_transform.run(raw_path, clean_path, clean_path.replace(".parquet", ".csv"))


def evaluate(model, X_test, y_test) -> dict:
    preds = model.predict(X_test)
    return {
        "mae": round(float(mean_absolute_error(y_test, preds)), 3),
        "rmse": round(float(root_mean_squared_error(y_test, preds)), 3),
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="Train ShipSense delay models.")
    parser.add_argument("--data", default="data/clean_shipments.parquet")
    parser.add_argument("--raw", default="data/raw_shipments.csv")
    parser.add_argument("--rows", type=int, default=50_000)
    parser.add_argument("--model-dir", default="models")
    args = parser.parse_args()

    ensure_clean_data(args.data, args.raw, args.rows)

    df = pd.read_parquet(args.data)
    X = df[FEATURE_COLUMNS]
    y = df[TARGET]
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42
    )
    print(f"[train] rows={len(df):,}  train={len(X_train):,}  test={len(X_test):,}")

    # Baseline: linear regression.
    baseline = LinearRegression().fit(X_train, y_train)
    baseline_metrics = evaluate(baseline, X_test, y_test)

    # Main model: gradient-boosted trees.
    model = GradientBoostingRegressor(random_state=42).fit(X_train, y_train)
    model_metrics = evaluate(model, X_test, y_test)

    improvement_pct = round(
        100.0 * (baseline_metrics["mae"] - model_metrics["mae"]) / baseline_metrics["mae"], 1
    )

    importances = {
        feat: round(float(imp), 4)
        for feat, imp in sorted(
            zip(FEATURE_COLUMNS, model.feature_importances_),
            key=lambda kv: kv[1], reverse=True,
        )
    }

    os.makedirs(args.model_dir, exist_ok=True)
    model_path = os.path.join(args.model_dir, "model.joblib")
    joblib.dump({"model": model, "feature_columns": FEATURE_COLUMNS}, model_path)

    metrics = {
        "trained_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "n_rows": int(len(df)),
        "n_train": int(len(X_train)),
        "n_test": int(len(X_test)),
        "baseline_model": "LinearRegression",
        "model": "GradientBoostingRegressor",
        "baseline_metrics": baseline_metrics,
        "model_metrics": model_metrics,
        "mae_improvement_pct_over_baseline": improvement_pct,
        "feature_importances": importances,
    }
    metrics_path = os.path.join(args.model_dir, "metrics.json")
    with open(metrics_path, "w", encoding="utf-8") as f:
        json.dump(metrics, f, indent=2)

    print(f"[train] baseline  MAE={baseline_metrics['mae']}  RMSE={baseline_metrics['rmse']}")
    print(f"[train] gradient  MAE={model_metrics['mae']}  RMSE={model_metrics['rmse']}")
    print(f"[train] MAE improvement over baseline: {improvement_pct}%")
    print(f"[train] saved model -> {model_path}")
    print(f"[train] saved metrics -> {metrics_path}")


if __name__ == "__main__":
    main()
