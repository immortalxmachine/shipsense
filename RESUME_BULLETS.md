# Resume bullets — ShipSense

Format: **action verb + what I built + how + measurable result.** All numbers are
real, measured from this repository on real public data (see README /
`metrics.json`). Keep to ~25 words each; trim to fit your resume.

1. **Built** a full-stack parcel-logistics platform (Spring Boot, FastAPI, React,
   PostgreSQL, Docker) exposing 9 REST endpoints, with 28 automated tests passing
   in GitHub Actions CI.

2. **Trained** a gradient-boosting delivery-time (ETA) model on **96K real orders**
   from the Olist Brazilian e-commerce dataset, predicting actual delivery days at
   4.9-day MAE (8% better than a linear baseline), served via a FastAPI microservice.

3. **Implemented** Dijkstra's shortest-path route optimizer from scratch
   (PriorityQueue + adjacency list, O((V+E) log V)) over a real 27-state Brazilian
   road-border network, exposed as a REST endpoint.

4. **Containerized** four services with Docker Compose for one-command startup and
   automated build/test/lint across Java, Python and TypeScript through a
   GitHub Actions pipeline.

---

### Shorter variants (if space is tight)

- Designed a layered Spring Boot API (controller/service/repository, JPA, global
  error handling) calling a Python ML service with graceful fallback.
- Engineered a Python ETL + training pipeline over the real Olist dataset (96K
  orders), deriving every feature from real order attributes with zero fabricated
  values, producing a delivery-time model with 4.9-day MAE.
