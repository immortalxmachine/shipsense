-- ===========================================================================
-- ShipSense — PostgreSQL schema
-- ---------------------------------------------------------------------------
-- Four core tables model a parcel-logistics network over Brazil's federative
-- units (the hubs), built around the real Olist e-commerce dataset:
--   hubs            Brazilian states/DF (nodes in the route graph)
--   routes          direct links between bordering states (edges in the graph)
--   shipments       a parcel moving from an origin state to a destination state
--   tracking_events the scan history of a shipment as it passes through hubs
--
-- Design notes (good interview talking points):
--   * Every foreign key is indexed so JOINs and lookups stay fast.
--   * `routes` stores each physical link as TWO directed rows (A->B and B->A)
--     so the Java RouteOptimizer can read a plain directed adjacency list and
--     still support one-way links later without schema changes.
--   * `hubs.code` is the 2-letter UF (SP, RJ, MG, ...) used by the REST API,
--     e.g. GET /api/routes/optimal?from=SP&to=RS.
-- ===========================================================================

-- Run idempotently: drop in dependency order first.
DROP TABLE IF EXISTS tracking_events CASCADE;
DROP TABLE IF EXISTS shipments      CASCADE;
DROP TABLE IF EXISTS routes         CASCADE;
DROP TABLE IF EXISTS hubs           CASCADE;

-- ---------------------------------------------------------------------------
-- hubs  (one per Brazilian federative unit)
-- ---------------------------------------------------------------------------
CREATE TABLE hubs (
    id      SERIAL PRIMARY KEY,
    code    CHAR(2)      NOT NULL UNIQUE,         -- UF: SP, RJ, MG ...
    name    VARCHAR(100) NOT NULL,               -- "São Paulo Hub"
    city    VARCHAR(100) NOT NULL,               -- state name, e.g. "São Paulo"
    region  CHAR(2)      NOT NULL                 -- IBGE macro-region
            CHECK (region IN ('N','NE','CO','SE','S')),
    lat     NUMERIC(9,6) NOT NULL,               -- centroid latitude  (deg)
    lng     NUMERIC(9,6) NOT NULL                -- centroid longitude (deg)
);

-- ---------------------------------------------------------------------------
-- routes  (directed edge: from_hub -> to_hub)
-- ---------------------------------------------------------------------------
CREATE TABLE routes (
    from_hub          INTEGER      NOT NULL REFERENCES hubs(id) ON DELETE CASCADE,
    to_hub            INTEGER      NOT NULL REFERENCES hubs(id) ON DELETE CASCADE,
    distance_km       NUMERIC(7,1) NOT NULL CHECK (distance_km > 0),
    avg_travel_hours  NUMERIC(5,1) NOT NULL CHECK (avg_travel_hours > 0),
    PRIMARY KEY (from_hub, to_hub)
);

-- Index the destination too so reverse lookups ("who reaches me?") are fast.
CREATE INDEX idx_routes_to_hub ON routes(to_hub);

-- ---------------------------------------------------------------------------
-- shipments
-- ---------------------------------------------------------------------------
CREATE TABLE shipments (
    id            SERIAL PRIMARY KEY,
    tracking_no   VARCHAR(20)  NOT NULL UNIQUE,
    origin_hub    INTEGER      NOT NULL REFERENCES hubs(id),  -- seller state
    dest_hub      INTEGER      NOT NULL REFERENCES hubs(id),  -- customer state
    weight_kg     NUMERIC(7,2) NOT NULL CHECK (weight_kg > 0),
    freight_value NUMERIC(10,2) NOT NULL CHECK (freight_value >= 0),  -- BRL
    status        VARCHAR(20)  NOT NULL DEFAULT 'CREATED'
                  CHECK (status IN ('CREATED','IN_TRANSIT','OUT_FOR_DELIVERY',
                                    'DELIVERED','DELAYED','EXCEPTION')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),  -- order purchase time
    promised_at   TIMESTAMPTZ  NOT NULL,                -- carrier's estimated date
    delivered_at  TIMESTAMPTZ,                          -- NULL until delivered
    CHECK (dest_hub <> origin_hub)
);

CREATE INDEX idx_shipments_origin  ON shipments(origin_hub);
CREATE INDEX idx_shipments_dest    ON shipments(dest_hub);
CREATE INDEX idx_shipments_status  ON shipments(status);
CREATE INDEX idx_shipments_created ON shipments(created_at);

-- ---------------------------------------------------------------------------
-- tracking_events  (one scan of a shipment at a hub)
-- ---------------------------------------------------------------------------
CREATE TABLE tracking_events (
    id           SERIAL PRIMARY KEY,
    shipment_id  INTEGER     NOT NULL REFERENCES shipments(id) ON DELETE CASCADE,
    hub_id       INTEGER     NOT NULL REFERENCES hubs(id),
    event_type   VARCHAR(30) NOT NULL
                 CHECK (event_type IN ('PICKED_UP','ARRIVED_AT_HUB',
                                       'DEPARTED_HUB','OUT_FOR_DELIVERY',
                                       'DELIVERED','EXCEPTION')),
    event_time   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_events_shipment ON tracking_events(shipment_id);
CREATE INDEX idx_events_hub      ON tracking_events(hub_id);
CREATE INDEX idx_events_time     ON tracking_events(event_time);
