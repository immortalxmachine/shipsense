# Interview notes — ShipSense

20 likely questions with answers grounded in *this* codebase. Each answer points
to where the concept lives (file + function/class).

---

### 1. Explain Dijkstra's algorithm and its complexity.
Dijkstra finds the shortest path from a source to all nodes in a graph with
non-negative edge weights. It keeps a set of "settled" nodes and repeatedly
expands the closest unsettled node (via a min-heap), relaxing its outgoing edges.
**Complexity:** `O((V + E) log V)` — each vertex is polled once (`V log V`) and
each edge can trigger a heap insert (`E log V`).
→ `backend-java/.../routing/DijkstraRouteOptimizer.java`, `findOptimalRoute`.

### 2. Why a PriorityQueue?
We always need the next-closest unsettled hub. A binary-heap `PriorityQueue`
gives `O(log V)` insert and extract-min, versus `O(V)` to scan an array each
step. We also skip "stale" heap entries using a `settled` set instead of a
decrease-key operation.
→ `DijkstraRouteOptimizer` (the `QueueEntry` record and the `while` loop).

### 3. How do you handle the no-path and same-hub cases?
Same hub → return a zero-length route immediately. Unreachable destination → the
distance map never gets an entry for the target, so we throw
`NoRouteFoundException` (mapped to HTTP 422). Both are covered by unit tests.
→ `DijkstraRouteOptimizerTest` (`sameHubIsTrivial`, `noPathThrows`).

### 4. Why the Strategy pattern for routing?
`RoutingStrategy` is an interface with `DijkstraRouteOptimizer` as one
implementation. This decouples callers (`RouteService`) from the algorithm, so an
A\* implementation could be injected without changing the service — Spring wires
the chosen bean by type.
→ `routing/RoutingStrategy.java`, injected in `service/RouteService.java`.

### 5. How does JPA map your entities?
Each `@Entity` maps to a table; fields map to columns. `@ManyToOne` +
`@JoinColumn` model foreign keys (e.g. a shipment's origin/dest hub). Enums use
`@Enumerated(EnumType.STRING)` so the DB stores readable text. IDs use
`@GeneratedValue(IDENTITY)` (DB auto-increment).
→ `entity/Shipment.java`, `entity/Hub.java`.

### 6. How do you model a composite primary key?
`routes` has PK `(from_hub, to_hub)`. The entity declares two `@Id @ManyToOne`
fields and `@IdClass(RouteId.class)`; `RouteId` holds matching `Integer` fields
with `equals`/`hashCode`.
→ `entity/Route.java`, `entity/RouteId.java`.

### 7. What REST principles did you follow?
Resource-oriented URIs (`/api/shipments`, `/api/hubs`), correct HTTP verbs
(POST=create, GET=read, PUT=update, DELETE=delete), correct status codes (201 +
`Location` on create, 204 on delete, 404/422/400 on errors), and JSON bodies with
DTOs rather than entities.
→ `controller/ShipmentController.java`.

### 8. How is validation done, and what happens on failure?
Request DTOs use Bean Validation annotations (`@NotBlank`, `@DecimalMin`,
`@Size`); controllers mark bodies `@Valid`. A failure throws
`MethodArgumentNotValidException`, caught by the global handler, which returns a
400 with a `fieldErrors` map.
→ `dto/ShipmentRequest.java`, `exception/GlobalExceptionHandler.java`.

### 9. How does the backend call the ML service, and what if it's down?
`MlPredictionClient` uses Spring's `RestClient` (short timeouts) to POST a feature
vector to `/predict`. Any failure (timeout, connection refused, 5xx) is caught and
a neutral `PredictionResult.unavailable()` is returned — the API degrades
gracefully instead of failing.
→ `service/MlPredictionClient.java`, `config/RestClientConfig.java`.

### 10. What is RAG / overfitting / how did you check for it?
Overfitting is when a model memorises training data and generalises poorly. We
check it by reporting metrics on a **held-out test set** (20%, never seen in
training) and comparing against a baseline. The gap between train and test error
would reveal overfitting; gradient boosting here generalises (test MAE 2.035).
→ `train.py` (`train_test_split`, `evaluate`).

### 11. MAE vs RMSE — what's the difference and why report both?
MAE is the mean absolute error (average miss, in hours, robust to outliers). RMSE
squares errors before averaging, so it penalises large misses more. Reporting both
shows typical error (MAE) and sensitivity to big errors (RMSE).
→ `train.py` (`mean_absolute_error`, `root_mean_squared_error`).

### 12. Why gradient boosting over linear regression here?
The synthetic delay rule has non-linear interactions (a hub-load threshold,
festival×monsoon compounding, distance×load coupling). Linear regression can't
represent those, so it plateaus (MAE 2.633). Gradient boosting captures them
(MAE 2.035, ~23% better).
→ `pipeline/generate_data.py` (delay rule), `train.py`.

### 13. What is train/serve skew and how do you prevent it?
Skew is when features are built differently in training vs serving, so the model
sees inconsistent inputs in production. We prevent it by building features through
one shared module used by both paths.
→ `pipeline/features.py` (`engineer`, `single_row_frame`).

### 14. What does your ETL step do?
`clean_transform.py` loads the raw CSV, drops rows with invalid weight, distance,
service type or calendar fields, removes duplicates, logs how many of each were
dropped (a `CleaningReport`), engineers features, and writes Parquet + CSV.
→ `pipeline/clean_transform.py` (`clean`, `run`).

### 15. Why Pydantic in the ML service?
Pydantic models validate and coerce the request body automatically — e.g.
`month` must be 1–12, `service_type` must be one of three literals. Invalid input
returns 422 before our code runs.
→ `app/schemas.py` (`PredictRequest`).

### 16. What SQL did you write beyond CRUD?
`db/analytics.sql`: on-time % per hub, average delay by service type, busiest
routes, a **window function** (`DENSE_RANK` per month) and a **CTE** comparing
each hub to the network-average on-time rate.
→ `db/analytics.sql`.

### 17. How does CI work?
GitHub Actions runs three parallel jobs on every push/PR: Maven `test` for the
backend, `pytest` for the ML service, and `npm run lint && npm run build` for the
frontend. A green run is required.
→ `.github/workflows/ci.yml`.

### 18. What does Docker solve here?
It packages each service with its exact runtime so "works on my machine" becomes
"works everywhere". `docker compose up` starts Postgres (auto-seeded), the ML
service (with a baked model), the API and the frontend together.
→ `docker-compose.yml`, the three `Dockerfile`s.

### 19. How would you scale this in the cloud?
Stateless API and ML service scale horizontally behind a load balancer; Postgres
scales with read replicas and connection pooling. The ML model would move to a
model registry / object storage, routing graphs could be cached, and heavy
training would run on a schedule. See `docs/deploy.md`.

### 20. How would you improve the model?
Train on real historical data; add features (weather APIs, carrier, time-of-day);
try hyperparameter tuning and cross-validation; calibrate the on-time probability
(currently a logistic transform of predicted delay) against observed outcomes;
monitor for drift in production.
→ `app/predictor.py` (`on_time_probability`), `train.py`.
