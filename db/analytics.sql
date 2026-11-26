-- ===========================================================================
-- ShipSense — analytical SQL
-- ---------------------------------------------------------------------------
-- Five read-only queries that answer real logistics questions over the Olist
-- order sample. Each is commented so it can be explained in an interview. Run
-- against the schema + seed data (or a larger real load) with:
--   psql ... -f db/analytics.sql
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- 1. On-time delivery % per origin state (seller state)
--    A shipment is "on time" when it was delivered on or before promised_at
--    (Olist's own estimated delivery date). Only delivered shipments are scored.
-- ---------------------------------------------------------------------------
SELECT
    h.code,
    h.city,
    COUNT(*)                                                AS delivered_count,
    COUNT(*) FILTER (WHERE s.delivered_at <= s.promised_at) AS on_time_count,
    ROUND(
        100.0 * COUNT(*) FILTER (WHERE s.delivered_at <= s.promised_at)
        / NULLIF(COUNT(*), 0)
    , 1)                                                    AS on_time_pct
FROM shipments s
JOIN hubs h ON h.id = s.origin_hub
WHERE s.status = 'DELIVERED' AND s.delivered_at IS NOT NULL
GROUP BY h.code, h.city
ORDER BY on_time_pct DESC, delivered_count DESC;

-- ---------------------------------------------------------------------------
-- 2. Average delivery time (days) by destination macro-region
--    Delivery time = delivered_at - created_at, in days. Captures how much
--    slower parcels into the North/Northeast run than into the Southeast.
--    Uses EXTRACT(EPOCH ...) to turn an interval into a numeric day count.
-- ---------------------------------------------------------------------------
SELECT
    hd.region,
    COUNT(*) AS delivered_count,
    ROUND(AVG(
        EXTRACT(EPOCH FROM (s.delivered_at - s.created_at)) / 86400.0
    )::numeric, 2) AS avg_delivery_days
FROM shipments s
JOIN hubs hd ON hd.id = s.dest_hub
WHERE s.status = 'DELIVERED' AND s.delivered_at IS NOT NULL
GROUP BY hd.region
ORDER BY avg_delivery_days DESC;

-- ---------------------------------------------------------------------------
-- 3. Busiest lanes (origin -> destination state) by shipment volume
-- ---------------------------------------------------------------------------
SELECT
    ho.code AS origin,
    hd.code AS destination,
    COUNT(*) AS shipment_count,
    ROUND(AVG(s.weight_kg), 2)     AS avg_weight_kg,
    ROUND(AVG(s.freight_value), 2) AS avg_freight_brl
FROM shipments s
JOIN hubs ho ON ho.id = s.origin_hub
JOIN hubs hd ON hd.id = s.dest_hub
GROUP BY ho.code, hd.code
ORDER BY shipment_count DESC, origin, destination
LIMIT 10;

-- ---------------------------------------------------------------------------
-- 4. WINDOW FUNCTION: rank states by shipment volume within each month
--    DENSE_RANK() restarts per month (PARTITION BY) and orders states by their
--    outbound volume. Shows month-over-month ranking without a self-join.
-- ---------------------------------------------------------------------------
SELECT
    to_char(date_trunc('month', s.created_at), 'YYYY-MM') AS month,
    h.code,
    COUNT(*) AS outbound_count,
    DENSE_RANK() OVER (
        PARTITION BY date_trunc('month', s.created_at)
        ORDER BY COUNT(*) DESC
    ) AS volume_rank
FROM shipments s
JOIN hubs h ON h.id = s.origin_hub
GROUP BY month, date_trunc('month', s.created_at), h.code
ORDER BY month, volume_rank;

-- ---------------------------------------------------------------------------
-- 5. CTE: states performing below the network-wide average on-time rate
--    Step A (CTE hub_stats): per-state delivered + on-time counts.
--    Step B: compare each state's on-time % to the overall average.
-- ---------------------------------------------------------------------------
WITH hub_stats AS (
    SELECT
        h.code,
        h.city,
        COUNT(*) AS delivered,
        COUNT(*) FILTER (WHERE s.delivered_at <= s.promised_at) AS on_time
    FROM shipments s
    JOIN hubs h ON h.id = s.origin_hub
    WHERE s.status = 'DELIVERED' AND s.delivered_at IS NOT NULL
    GROUP BY h.code, h.city
),
scored AS (
    SELECT
        code,
        city,
        delivered,
        ROUND(100.0 * on_time / NULLIF(delivered, 0), 1) AS on_time_pct
    FROM hub_stats
)
SELECT
    code,
    city,
    delivered,
    on_time_pct,
    ROUND((SELECT AVG(on_time_pct) FROM scored), 1) AS network_avg_pct
FROM scored
WHERE on_time_pct < (SELECT AVG(on_time_pct) FROM scored)
ORDER BY on_time_pct ASC;
