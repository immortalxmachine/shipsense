# Architecture & design decisions

This document explains *why* each major choice was made — the kind of reasoning
an interviewer probes for.

## Why a separate Python ML microservice (not ML inside Java)?

The Java ecosystem can run models, but the data-science tooling (pandas,
scikit-learn) lives in Python. Splitting the ML into its own FastAPI service
means:

- The right tool for each job: Spring for transactional REST + JPA, Python for
  modelling.
- Independent scaling and deployment — the ML service can be scaled or updated
  without redeploying the API.
- A clean contract (`POST /predict` with a JSON feature vector), so either side
  can be rewritten independently.

The trade-off is an extra network hop and the need to handle the ML service
being unavailable — which the backend does with a **graceful fallback**
(`MlPredictionClient` returns a neutral result instead of failing the request).

## Why Dijkstra for routing?

All edge weights are road distances, which are **non-negative**. Dijkstra is the
correct, efficient choice for single-source shortest paths with non-negative
weights. It is implemented over an adjacency list with a binary-heap
`PriorityQueue`, giving `O((V + E) log V)`. The algorithm sits behind a
`RoutingStrategy` interface (Strategy pattern), so an A\* or
contraction-hierarchies variant could replace it without touching callers.

Bellman-Ford would be needed only for negative weights (not applicable here) and
is slower (`O(V·E)`).

## Why gradient boosting over linear regression?

The true delay process has **non-linear interactions** (a congestion threshold
on hub load, festival×monsoon compounding, distance×load coupling). A linear
model cannot represent these with the raw features, so it plateaus. Gradient
boosting builds additive trees that capture interactions and thresholds,
improving test MAE by ~23% over the linear baseline. The linear model is kept as
an honest baseline so the improvement is measurable.

To avoid **train/serve skew**, both training and serving build features through
one shared module (`pipeline/features.py`), guaranteeing identical column order
and encoding.

## Why PostgreSQL?

A logistics domain is inherently relational (hubs, routes, shipments, events
with foreign keys and referential integrity). PostgreSQL gives strong
constraints (`CHECK`, FKs), indexes for the hot lookups, and rich analytical SQL
(window functions, CTEs) used in `db/analytics.sql`. H2 (in PostgreSQL
compatibility mode) is used only for fast, isolated tests.

## Layered backend

`controller → service → repository` with DTOs at the boundary:

- **Controllers** handle HTTP and validation only.
- **Services** hold business logic and transactions.
- **Repositories** (Spring Data JPA) handle persistence.
- **DTOs** decouple the API shape from entities, so the schema can evolve without
  breaking clients.
- A single `@RestControllerAdvice` turns exceptions into consistent JSON errors.

## Composite key for `routes`

A road link is naturally identified by its endpoints, so `routes` uses a
composite primary key `(from_hub, to_hub)` modelled with JPA `@IdClass`. Each
physical road is stored as two directed rows, which keeps the graph a plain
directed adjacency list and leaves room for one-way links later.

## Testing strategy

- Pure algorithm (`DijkstraRouteOptimizer`) is tested without Spring — fast and
  focused, including no-path and same-hub edge cases.
- Services are unit-tested with Mockito.
- One full-stack `@SpringBootTest` + MockMvc test runs against H2 to prove the
  wiring (controllers, validation, JPA) works end to end.
- The ML pipeline and API are tested with pytest; the API tests stub the model so
  they do not require training.
