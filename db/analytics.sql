-- ===========================================================================
-- ShipSense — analytical SQL
-- ---------------------------------------------------------------------------
-- Five read-only queries that answer real logistics questions. Each is
-- commented so it can be explained in an interview. Run against the schema +
-- seed data (or a larger real dataset) with: psql ... -f db/analytics.sql
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- 1. On-time delivery % per hub (by ORIGIN hub)
--    A shipment is "on time" when it was delivered on or before promised_at.
--    We only score shipments that have actually been delivered.
-- ---------------------------------------------------------------------------
SELECT
    h.code,
    h.city,
    COUNT(*)                                            AS delivered_count,
    COUNT(*) FILTER (WHERE s.delivered_at <= s.promised_at) AS on_time_count,
    ROUND(
        100.0 * COUNT(*) FILTER (WHERE s.delivered_at <= s.promised_at)
        / NULLIF(COUNT(*), 0)
    , 1)                                                AS on_time_pct
FROM shipments s
JOIN hubs h ON h.id = s.origin_hub
WHERE s.status = 'DELIVERED' AND s.delivered_at IS NOT NULL
GROUP BY h.code, h.city
ORDER BY on_time_pct DESC, delivered_count DESC;

-- ---------------------------------------------------------------------------
-- 2. Average delay (hours) by service type
--    Delay = delivered_at - promised_at, in hours. Negative means early.
--    Uses EXTRACT(EPOCH ...) to turn an interval into a numeric hour count.
-- ---------------------------------------------------------------------------
SELECT
    s.service_type,
    COUNT(*) AS delivered_count,
    ROUND(AVG(
        EXTRACT(EPOCH FROM (s.delivered_at - s.promised_at)) / 3600.0
    )::numeric, 2) AS avg_delay_hours
FROM shipments s
WHERE s.status = 'DELIVERED' AND s.delivered_at IS NOT NULL
GROUP BY s.service_type
ORDER BY avg_delay_hours DESC;

-- ---------------------------------------------------------------------------
-- 3. Busiest routes (origin -> destination lane) by shipment volume
-- ---------------------------------------------------------------------------
SELECT
    ho.code AS origin,
    hd.code AS destination,
    COUNT(*) AS shipment_count,
    ROUND(AVG(s.weight_kg), 2) AS avg_weight_kg
FROM shipments s
JOIN hubs ho ON ho.id = s.origin_hub
JOIN hubs hd ON hd.id = s.dest_hub
GROUP BY ho.code, hd.code
ORDER BY shipment_count DESC, origin, destination
LIMIT 10;

-- ---------------------------------------------------------------------------
-- 4. WINDOW FUNCTION: rank hubs by shipment volume within each month
--    DENSE_RANK() restarts per month (PARTITION BY) and orders hubs by their
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
-- 5. CTE: hubs performing below the network-wide average on-time rate
--    Step A (CTE hub_stats): per-hub delivered + on-time counts.
--    Step B: compare each hub's on-time % to the overall average.
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
