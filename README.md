# ShipSense — Parcel Delivery ETA Prediction & Route Optimization

> Predict shipment delays with machine learning and compute optimal hub-to-hub
> routes with Dijkstra's algorithm — a full-stack logistics platform built with
> **Spring Boot (Java)**, **FastAPI (Python)**, **React (TypeScript)**,
> **PostgreSQL** and **Docker**.

![CI](https://github.com/immortalxmachine/shipsense/actions/workflows/ci.yml/badge.svg)

ShipSense is a portfolio project. All data is **synthetic** and clearly labelled
as such; every metric below comes from actually running the code.

---

## Architecture

```mermaid
flowchart LR
    U[React + TypeScript<br/>dashboard] -->|REST / JSON| API[Spring Boot API<br/>Java 17]
    API -->|JPA| DB[(PostgreSQL)]
    API -->|POST /predict| ML[FastAPI ML service<br/>scikit-learn]
    ML -->|loads| M[[GradientBoosting<br/>model.joblib]]
    subgraph Offline training
        GEN[generate_data.py] --> CLEAN[clean_transform.py ETL] --> TRAIN[train.py] --> M
    end
```

- **Frontend** calls the **Java API** over REST.
- The **Java API** owns the database (shipments, hubs, routes, tracking events)
  and, for each shipment, calls the **Python ML service** for a delay prediction
  (with a graceful fallback if the ML service is down).
- The **ML service** serves a gradient-boosted model trained offline by a small
  data pipeline.

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

- **Track a parcel** — status, scan-event timeline and ML-predicted delay.
- **Route planner** — shortest path between any two hubs via Dijkstra's
  algorithm (own implementation), with total distance and ETA.
- **Analytics** — on-time % by hub, average delay by service type, and shipment
  volume over time (Recharts).
- **ML delay prediction** — gradient-boosted regressor with graceful fallback.
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
| GET    | `/api/hubs` | List hubs |
| GET    | `/api/routes/optimal?from=CHN&to=DEL` | Optimal route (Dijkstra) |

### Python ML service (`http://localhost:8000`)

| Method | Path | Description |
|--------|------|-------------|
| POST   | `/predict` | Predicted delay hours + on-time probability |
| GET    | `/health` | Liveness + model-loaded flag |
| GET    | `/metrics` | Training metrics (MAE/RMSE, feature importances) |

---

## Machine-learning results

Trained on **49,000** cleaned rows (from 50,000 generated; 1,000 dirty rows
dropped by the ETL step), 80/20 train/test split. Numbers from
[`ml-service/models/metrics.json`](ml-service/models/metrics.json):

| Model | MAE (hours) | RMSE (hours) |
|-------|------------:|-------------:|
| Baseline — Linear Regression | 2.633 | 3.287 |
| **GradientBoostingRegressor** | **2.035** | **2.551** |

The gradient-boosted model improves MAE by **22.7%** over the linear baseline by
capturing non-linear interactions (hub-load congestion threshold,
festival×monsoon compounding, distance×load coupling). Top feature importances:
`hub_load` (0.51), `distance_km` (0.32), `month` (0.09).

---

## Data

- **20** hubs (real Indian cities) and **29** road links (stored as 58 directed
  edges) in [`db/seed.sql`](db/seed.sql).
- **10** sample shipments with tracking histories.
- **5** analytical SQL queries (window function + CTE) in
  [`db/analytics.sql`](db/analytics.sql).

---

## Tests

**23 automated tests**, all green in CI:

- **Backend (15, JUnit 5 + Mockito + MockMvc, H2):** Dijkstra unit tests
  (incl. no-path and same-hub edge cases), service-layer tests, and a full-stack
  controller test.
- **ML service (8, pytest):** pipeline (generation, cleaning, feature
  engineering) and API (stubbed model, validation, 503 paths).

```bash
cd backend-java && ./mvnw test      # 15 tests
cd ml-service   && pytest           #  8 tests
```

---

## Screenshots

_Add screenshots of the Track, Route planner and Analytics pages here._

- `docs/screenshot-track.png`
- `docs/screenshot-route.png`
- `docs/screenshot-analytics.png`

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
