# ShipSense — Parcel Delivery ETA Prediction & Route Optimization

> Predict shipment delays with machine learning and compute optimal hub-to-hub
> routes with Dijkstra's algorithm — a full-stack logistics platform built with
> **Spring Boot (Java)**, **FastAPI (Python)**, **React (TypeScript)**,
> **PostgreSQL** and **Docker**.

![CI](https://github.com/immortalxmachine/shipsense/actions/workflows/ci.yml/badge.svg)

ShipSense is a portfolio project built on **real data**: the model trains on the
[Olist Brazilian E-Commerce Public Dataset](https://www.kaggle.com/datasets/olistbr/brazilian-ecommerce)
(~96k real orders with actual purchase→delivery timestamps), and the routing
graph is Brazil's 27 federative units with centroids from Olist's real
geolocation data. Nothing is fabricated — the ETA target is the *observed*
delivery time, and every metric below comes from actually running the code on
that data. See [Data](#data) for provenance and licence.

---

## Architecture

```mermaid
flowchart LR
    U[React + TypeScript<br/>dashboard] -->|REST / JSON| API[Spring Boot API<br/>Java 17]
    API -->|JPA| DB[(PostgreSQL)]
    API -->|POST /predict| ML[FastAPI ML service<br/>scikit-learn]
    ML -->|loads| M[[GradientBoosting<br/>model.joblib]]
    subgraph Offline training
        OLIST[(Olist CSVs<br/>Kaggle)] --> BUILD[build_dataset.py] --> CLEAN[clean_transform.py ETL] --> TRAIN[train.py] --> M
    end
```

- **Frontend** calls the **Java API** over REST.
- The **Java API** owns the database (shipments, hubs, routes, tracking events)
  and, for each shipment, calls the **Python ML service** for a delay prediction
  (with a graceful fallback if the ML service is down).
- The **ML service** serves a gradient-boosted delivery-time model trained
  offline, by a small data pipeline, on the real Olist dataset.

See [`docs/architecture.md`](docs/architecture.md) for the design rationale.

---

## Tech stack

| Layer     | Technology |
|-----------|-----------|
| Frontend  | React 18, TypeScript, Vite, Tailwind CSS, Recharts, React Router |
| Backend   | Java 17, Spring Boot 3.3, Spring Data JPA, Bean Validation, springdoc/OpenAPI |
| ML service| Python 3.12, FastAPI, pandas, scikit-learn, joblib, Pydantic |
| Database  | PostgreSQL 16 |
| Dev/Ops   | Docker, docker-compose, GitHub Actions CI |

---

## Features

- **Track a parcel** — status, scan-event timeline and ML-predicted delivery time.
- **Route planner** — shortest path between any two state hubs via Dijkstra's
  algorithm (own implementation), with total distance and ETA.
- **Analytics** — on-time % by origin state, average delivery time by destination
  state, and shipment volume over time (Recharts).
- **ML delivery-time prediction** — gradient-boosted regressor with graceful
  fallback, trained on real delivery durations.
- **OpenAPI/Swagger UI**, global JSON error handling, CORS, Actuator health.

---

## Run it

### Option 1 — Docker (one command)

```bash
cp .env.example .env     # adjust if you like
docker compose up --build
```

- Frontend: <http://localhost:5173>
- Java API + Swagger UI: <http://localhost:8080/swagger-ui.html>
- ML service docs: <http://localhost:8000/docs>

### Option 2 — run each service locally

```bash
# 1. PostgreSQL (any local instance), then load schema + seed:
psql "postgresql://shipsense:changeme_local_only@localhost:5432/shipsense" -f db/schema.sql
psql "postgresql://shipsense:changeme_local_only@localhost:5432/shipsense" -f db/seed.sql

# 2. ML service
cd ml-service && python -m venv .venv && .venv/Scripts/activate   # (Windows)
pip install -r requirements-dev.txt
# train.py trains from the committed data/clean_shipments.parquet if present.
# To rebuild from raw Olist, download it first (see "Data"), then:
#   rm -f data/clean_shipments.parquet && python train.py
python train.py                      # trains model + writes metrics.json
uvicorn app.main:app --port 8000

# 3. Java API
cd backend-java && ./mvnw spring-boot:run

# 4. Frontend
cd frontend && npm install && npm run dev
```

---

## API

### Java backend (`http://localhost:8080`)

| Method | Path | Description |
|--------|------|-------------|
| POST   | `/api/shipments` | Create a shipment |
| GET    | `/api/shipments` | List shipments |
| GET    | `/api/shipments/{trackingNo}` | Track a parcel (incl. ML delay) |
| PUT    | `/api/shipments/{id}` | Update a shipment |
| DELETE | `/api/shipments/{id}` | Delete a shipment |
| GET    | `/api/shipments/{trackingNo}/events` | Scan-event timeline |
| POST   | `/api/shipments/{id}/events` | Append a tracking event |
| GET    | `/api/hubs` | List hubs (states) |
| GET    | `/api/routes/optimal?from=SP&to=RS` | Optimal route (Dijkstra) |

### Python ML service (`http://localhost:8000`)

| Method | Path | Description |
|--------|------|-------------|
| POST   | `/predict` | Predicted delivery days + on-time probability |
| GET    | `/health` | Liveness + model-loaded flag |
| GET    | `/metrics` | Training metrics (MAE/RMSE, feature importances) |

---

## Machine-learning results

**Task:** predict a parcel's delivery time in **days** — the real target
`delivery_days = order_delivered_customer_date − order_purchase_timestamp`,
observed from Olist's actual timestamps.

Trained on **96,406** cleaned rows (from 96,470 delivered orders; 64 dropped for
out-of-range delivery times), 80/20 train/test split (77,124 / 19,282). Mean
real delivery time is **12.5 days**. Numbers from
[`ml-service/models/metrics.json`](ml-service/models/metrics.json):

| Model | MAE (days) | RMSE (days) |
|-------|-----------:|------------:|
| Baseline — Linear Regression | 5.287 | 7.653 |
| **GradientBoostingRegressor** | **4.866** | **7.159** |

The gradient-boosted model improves MAE by **8.0%** over the linear baseline by
capturing non-linear effects the line cannot. The gap is honestly modest — real
logistics data is far noisier than a clean synthetic rule, and a few very slow
deliveries drive RMSE well above MAE. Top feature importances: `distance_km`
(0.62), `purchase_month` (0.23, seasonality), `freight_value` (0.05). Features
are all derived from real order attributes (seller/customer-state distance,
weight, freight, same-state flag, purchase weekday/month, IBGE macro-region).

On-time probability is a deterministic, explainable logistic transform of the
predicted delivery time versus the carrier's own estimate (the real on-time rate
in the data, delivered ≤ estimate, is **91.9%**).

---

## Data

Source: **Olist Brazilian E-Commerce Public Dataset** (Kaggle:
[`olistbr/brazilian-ecommerce`](https://www.kaggle.com/datasets/olistbr/brazilian-ecommerce),
licence **CC BY-NC-SA 4.0**). ~100k real orders (2016–2018) with purchase,
estimate and delivery timestamps, geolocation, and seller/customer locations.

Download it (needed only to rebuild the model from raw) into `data/olist/`:

```bash
export KAGGLE_API_TOKEN=$(cat ~/.kaggle/access_token)
kaggle datasets download -d olistbr/brazilian-ecommerce -p data/olist --unzip
```

The raw CSVs (~130 MB) are **not** committed; the cleaned model dataset
(`ml-service/data/clean_shipments.parquet`) **is**, so the model and Docker image
reproduce without the download. The database seed is regenerated from the raw
data with [`db/build_seed.py`](db/build_seed.py).

- **27** hubs (Brazilian states/DF; centroids from real Olist geolocation) and
  **51** real border links (stored as 102 directed edges) in
  [`db/seed.sql`](db/seed.sql).
- **14** sample shipments — a real, reproducible sample of actual Olist orders
  (real timestamps, weights and freight) with tracking histories.
- **5** analytical SQL queries (window function + CTE) in
  [`db/analytics.sql`](db/analytics.sql).

---

## Tests

**28 automated tests**, all green in CI:

- **Backend (15, JUnit 5 + Mockito + MockMvc, H2):** Dijkstra unit tests
  (incl. no-path and same-hub edge cases), service-layer tests, and a full-stack
  controller test.
- **ML service (13, pytest):** geography/graph (symmetry, connectivity,
  haversine), pipeline (cleaning, feature engineering), and API (stubbed model,
  validation, unknown-region and 503 paths, on-time-probability logic).

```bash
cd backend-java && ./mvnw test      # 15 tests
cd ml-service   && pytest           # 13 tests
```

---

## Screenshots

![ShipSense — React frontend, Track a parcel page](docs/img/demo.png)

*The React + TypeScript frontend (Vite dev server): the Track-a-parcel page with
the nav to Route planner and Analytics. The SPA calls the Spring Boot API
(`:8080`); run the full stack (`docker compose up`) for live tracking, route and
analytics data.*

---

## Repository layout

```
shipsense/
├── backend-java/   Spring Boot REST API (Dijkstra, JPA, ML client)
├── ml-service/     FastAPI + data pipeline + training
├── frontend/       React + TypeScript dashboard
├── db/             schema.sql, seed.sql, analytics.sql
├── docs/           architecture.md, deploy.md
├── docker-compose.yml
└── .github/workflows/ci.yml
```

See also [`INTERVIEW_NOTES.md`](INTERVIEW_NOTES.md) and
[`RESUME_BULLETS.md`](RESUME_BULLETS.md).
