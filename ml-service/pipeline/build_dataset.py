"""Build the real shipment dataset from the raw Olist CSVs.

Source: the Olist Brazilian E-Commerce Public Dataset (Kaggle:
``olistbr/brazilian-ecommerce``, CC BY-NC-SA 4.0). Download + unzip it into
``data/olist/`` at the repo root:

    export KAGGLE_API_TOKEN=$(cat ~/.kaggle/access_token)
    kaggle datasets download -d olistbr/brazilian-ecommerce -p data/olist --unzip

One row per real order. Nothing is invented: the target is the actual observed
delivery time and every feature is read or derived from real order attributes.

    label  delivery_days = order_delivered_customer_date − order_purchase_timestamp
    geo    distance_km   = haversine(seller-state centroid, customer-state centroid)

Run (from ml-service/):
    python -m pipeline.build_dataset --olist ../data/olist --out data/raw_shipments.csv
"""
from __future__ import annotations

import argparse
import os

import pandas as pd

from pipeline.geo import STATE_REGION, haversine_km, state_centroids

# Columns written to the raw dataset (consumed by clean_transform.py).
RAW_COLUMNS = [
    "order_id", "seller_state", "customer_state",
    "distance_km", "same_state", "weight_kg", "freight_value",
    "purchase_dow", "purchase_month", "seller_region", "cust_region",
    "purchase_ts", "estimated_ts", "delivered_ts",
    "estimated_days", "delivery_days",
]


def build(olist_dir: str) -> pd.DataFrame:
    orders = pd.read_csv(
        os.path.join(olist_dir, "olist_orders_dataset.csv"),
        usecols=["order_id", "customer_id", "order_status",
                 "order_purchase_timestamp", "order_delivered_customer_date",
                 "order_estimated_delivery_date"],
        parse_dates=["order_purchase_timestamp", "order_delivered_customer_date",
                     "order_estimated_delivery_date"],
    )
    items = pd.read_csv(
        os.path.join(olist_dir, "olist_order_items_dataset.csv"),
        usecols=["order_id", "order_item_id", "product_id", "seller_id", "freight_value"],
    )
    customers = pd.read_csv(
        os.path.join(olist_dir, "olist_customers_dataset.csv"),
        usecols=["customer_id", "customer_state"],
    )
    sellers = pd.read_csv(
        os.path.join(olist_dir, "olist_sellers_dataset.csv"),
        usecols=["seller_id", "seller_state"],
    )
    products = pd.read_csv(
        os.path.join(olist_dir, "olist_products_dataset.csv"),
        usecols=["product_id", "product_weight_g"],
    )

    # Only delivered orders carry a real delivery time.
    o = orders[
        (orders.order_status == "delivered")
        & orders.order_delivered_customer_date.notna()
        & orders.order_purchase_timestamp.notna()
    ].copy()
    o["delivery_days"] = (
        (o.order_delivered_customer_date - o.order_purchase_timestamp)
        .dt.total_seconds() / 86400.0
    )
    o["estimated_days"] = (
        (o.order_estimated_delivery_date - o.order_purchase_timestamp)
        .dt.total_seconds() / 86400.0
    )

    # Per-order item aggregates: total freight + the (first) seller of the order.
    item_agg = items.groupby("order_id").agg(
        freight_value=("freight_value", "sum"),
        seller_id=("seller_id", "first"),
    ).reset_index()

    # Per-order product weight (sum of item weights).
    iw = items.merge(products, on="product_id", how="left")
    weight_agg = (
        iw.groupby("order_id")["product_weight_g"].sum().div(1000.0)
        .rename("weight_kg").reset_index()
    )

    df = (
        o.merge(item_agg, on="order_id", how="inner")
         .merge(weight_agg, on="order_id", how="left")
         .merge(customers, on="customer_id", how="left")
         .merge(sellers, on="seller_id", how="left")
    )

    # Real state centroids -> geographic distance.
    cent = state_centroids(os.path.join(olist_dir, "olist_geolocation_dataset.csv"))
    df = (
        df.merge(cent.rename(columns={"lat": "s_lat", "lng": "s_lng"}),
                 left_on="seller_state", right_index=True, how="left")
          .merge(cent.rename(columns={"lat": "c_lat", "lng": "c_lng"}),
                 left_on="customer_state", right_index=True, how="left")
    )
    df["distance_km"] = haversine_km(df.s_lat, df.s_lng, df.c_lat, df.c_lng)
    df["same_state"] = (df.seller_state == df.customer_state).astype(int)

    df["purchase_dow"] = df.order_purchase_timestamp.dt.dayofweek   # 0=Mon .. 6=Sun
    df["purchase_month"] = df.order_purchase_timestamp.dt.month
    df["seller_region"] = df.seller_state.map(STATE_REGION)
    df["cust_region"] = df.customer_state.map(STATE_REGION)

    df["purchase_ts"] = df.order_purchase_timestamp
    df["estimated_ts"] = df.order_estimated_delivery_date
    df["delivered_ts"] = df.order_delivered_customer_date

    return df[RAW_COLUMNS]


def main() -> None:
    parser = argparse.ArgumentParser(description="Build the real Olist shipment dataset.")
    parser.add_argument("--olist", default=os.getenv("OLIST_DIR", "../data/olist"),
                        help="Directory with the unzipped Olist CSVs.")
    parser.add_argument("--out", default="data/raw_shipments.csv")
    args = parser.parse_args()

    df = build(args.olist)
    os.makedirs(os.path.dirname(args.out) or ".", exist_ok=True)
    df.to_csv(args.out, index=False)
    print(f"Wrote {len(df):,} real orders to {args.out}")
    print(f"  delivery_days: mean={df.delivery_days.mean():.2f} "
          f"median={df.delivery_days.median():.2f} "
          f"on-time-vs-estimate={100 * (df.delivered_ts <= df.estimated_ts).mean():.1f}%")


if __name__ == "__main__":
    main()
