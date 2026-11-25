# Resume bullets — ShipSense

Format: **action verb + what I built + how + measurable result.** All numbers are
real, measured from this repository (see README / `metrics.json`). Keep to ~25
words each; trim to fit your resume.

1. **Built** a full-stack parcel-logistics platform (Spring Boot, FastAPI, React,
   PostgreSQL, Docker) exposing 9 REST endpoints, with 23 automated tests passing
   in GitHub Actions CI.

2. **Trained** a gradient-boosting delay-prediction model on 49,000 shipments,
   cutting error (MAE) 22.7% versus a linear baseline to 2.0 hours, served via a
   FastAPI microservice.

3. **Implemented** Dijkstra's shortest-path route optimizer from scratch
   (PriorityQueue + adjacency list, O((V+E) log V)) over a 20-hub road network,
   exposed as a REST endpoint.

4. **Containerized** four services with Docker Compose for one-command startup and
   automated build/test/lint across Java, Python and TypeScript through a
   GitHub Actions pipeline.

---

### Shorter variants (if space is tight)

- Designed a layered Spring Boot API (controller/service/repository, JPA, global
  error handling) calling a Python ML service with graceful fallback.
- Engineered a Python ETL + training pipeline (50k synthetic rows → cleaned 49k)
  producing a model with 2.0-hour MAE, 23% better than baseline.
