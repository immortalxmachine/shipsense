-- ===========================================================================
-- ShipSense — seed data
-- ---------------------------------------------------------------------------
-- 20 real Indian cities as hubs, a connected road network between neighbouring
-- hubs (realistic approximate road distances / drive times), and a handful of
-- sample shipments with tracking history so the app has data on first run.
--
-- NOTE: distances/times are realistic approximations for a portfolio demo,
--       not an authoritative road dataset.
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- Hubs  (code, name, city, lat, lng)
-- ---------------------------------------------------------------------------
INSERT INTO hubs (code, name, city, lat, lng) VALUES
  ('DEL', 'Delhi Hub',          'Delhi',          28.613900, 77.209000),
  ('MUM', 'Mumbai Hub',         'Mumbai',         19.076000, 72.877700),
  ('CHN', 'Chennai Hub',        'Chennai',        13.082700, 80.270700),
  ('KOL', 'Kolkata Hub',        'Kolkata',        22.572600, 88.363900),
  ('BLR', 'Bengaluru Hub',      'Bengaluru',      12.971600, 77.594600),
  ('HYD', 'Hyderabad Hub',      'Hyderabad',      17.385000, 78.486700),
  ('PUN', 'Pune Hub',           'Pune',           18.520400, 73.856700),
  ('AMD', 'Ahmedabad Hub',      'Ahmedabad',      23.022500, 72.571400),
  ('JAI', 'Jaipur Hub',         'Jaipur',         26.912400, 75.787300),
  ('LKO', 'Lucknow Hub',        'Lucknow',        26.846700, 80.946200),
  ('NAG', 'Nagpur Hub',         'Nagpur',         21.145800, 79.088200),
  ('BHO', 'Bhopal Hub',         'Bhopal',         23.259900, 77.412600),
  ('SUR', 'Surat Hub',          'Surat',          21.170200, 72.831100),
  ('KAN', 'Kanpur Hub',         'Kanpur',         26.449900, 80.331900),
  ('IND', 'Indore Hub',         'Indore',         22.719600, 75.857700),
  ('CBE', 'Coimbatore Hub',     'Coimbatore',     11.016800, 76.955800),
  ('COK', 'Kochi Hub',          'Kochi',           9.931200, 76.267300),
  ('VSK', 'Visakhapatnam Hub',  'Visakhapatnam',  17.686800, 83.218500),
  ('GUW', 'Guwahati Hub',       'Guwahati',       26.144500, 91.736200),
  ('PAT', 'Patna Hub',          'Patna',          25.594100, 85.137600);

-- ---------------------------------------------------------------------------
-- Routes  (undirected roads, inserted as BOTH directed edges)
-- ---------------------------------------------------------------------------
WITH edge(a, b, km, hrs) AS (
  VALUES
    ('DEL','JAI', 280.0,  5.5),
    ('DEL','LKO', 555.0,  9.5),
    ('DEL','KAN', 485.0,  8.5),
    ('DEL','AMD', 940.0, 15.0),   -- long direct link; Dijkstra should prefer DEL-JAI-AMD
    ('JAI','AMD', 660.0, 11.0),
    ('JAI','IND', 590.0, 10.0),
    ('AMD','SUR', 265.0,  4.5),
    ('AMD','MUM', 525.0,  9.0),
    ('SUR','MUM', 285.0,  5.0),
    ('MUM','PUN', 150.0,  3.0),
    ('IND','BHO', 195.0,  3.5),
    ('BHO','NAG', 350.0,  6.0),
    ('NAG','HYD', 500.0,  8.5),
    ('PUN','HYD', 560.0,  9.5),
    ('HYD','BLR', 575.0,  9.5),
    ('HYD','VSK', 620.0, 10.5),
    ('HYD','CHN', 630.0, 10.5),
    ('BLR','CHN', 350.0,  6.0),
    ('BLR','CBE', 365.0,  6.5),
    ('BLR','COK', 555.0,  9.5),
    ('CHN','CBE', 500.0,  8.5),
    ('CBE','COK', 190.0,  4.0),
    ('CHN','VSK', 780.0, 13.0),
    ('VSK','KOL', 875.0, 14.5),
    ('KOL','PAT', 585.0, 10.5),
    ('KOL','GUW',1030.0, 17.0),
    ('PAT','LKO', 530.0,  9.5),
    ('LKO','KAN',  85.0,  2.0),
    ('KAN','NAG', 605.0, 10.5)
)
INSERT INTO routes (from_hub, to_hub, distance_km, avg_travel_hours)
SELECT ha.id, hb.id, e.km, e.hrs
  FROM edge e JOIN hubs ha ON ha.code = e.a JOIN hubs hb ON hb.code = e.b
UNION ALL
SELECT hb.id, ha.id, e.km, e.hrs
  FROM edge e JOIN hubs ha ON ha.code = e.a JOIN hubs hb ON hb.code = e.b;

-- ---------------------------------------------------------------------------
-- Sample shipments  (reference hubs by code via a small helper view)
-- ---------------------------------------------------------------------------
-- Helper: resolve a hub code to its id inline.  (A CTE keeps the inserts readable.)
INSERT INTO shipments
    (tracking_no, origin_hub, dest_hub, weight_kg, service_type, status,
     created_at, promised_at, delivered_at)
SELECT s.tracking_no, ho.id, hd.id, s.weight_kg, s.service_type, s.status,
       s.created_at, s.promised_at, s.delivered_at
FROM (
    VALUES
      ('SS0000000001','DEL','CHN',  2.50,'EXPRESS','DELIVERED',
          now() - interval '6 days', now() - interval '3 days', now() - interval '3 days 2 hours'),
      ('SS0000000002','MUM','KOL', 12.00,'STANDARD','DELIVERED',
          now() - interval '8 days', now() - interval '3 days', now() - interval '2 days'),
      ('SS0000000003','BLR','DEL',  0.80,'PRIORITY','DELIVERED',
          now() - interval '4 days', now() - interval '2 days', now() - interval '2 days 1 hour'),
      ('SS0000000004','CHN','AMD',  5.40,'STANDARD','IN_TRANSIT',
          now() - interval '2 days', now() + interval '2 days', NULL),
      ('SS0000000005','HYD','GUW',  3.10,'EXPRESS','IN_TRANSIT',
          now() - interval '1 days', now() + interval '2 days', NULL),
      ('SS0000000006','PUN','COK',  7.75,'STANDARD','DELAYED',
          now() - interval '5 days', now() - interval '1 days', NULL),
      ('SS0000000007','KOL','MUM',  1.20,'PRIORITY','OUT_FOR_DELIVERY',
          now() - interval '2 days', now() + interval '1 days', NULL),
      ('SS0000000008','DEL','BLR',  9.60,'STANDARD','CREATED',
          now() - interval '4 hours', now() + interval '3 days', NULL),
      ('SS0000000009','AMD','VSK',  4.00,'EXPRESS','IN_TRANSIT',
          now() - interval '1 days', now() + interval '2 days', NULL),
      ('SS0000000010','JAI','HYD',  2.20,'STANDARD','DELIVERED',
          now() - interval '7 days', now() - interval '4 days', now() - interval '3 days 20 hours')
) AS s(tracking_no, o_code, d_code, weight_kg, service_type, status,
       created_at, promised_at, delivered_at)
JOIN hubs ho ON ho.code = s.o_code
JOIN hubs hd ON hd.code = s.d_code;

-- ---------------------------------------------------------------------------
-- Tracking events for a couple of shipments (so the timeline UI has data)
-- ---------------------------------------------------------------------------
-- Shipment 1 (DEL -> CHN, delivered): a full happy-path scan history.
INSERT INTO tracking_events (shipment_id, hub_id, event_type, event_time)
SELECT s.id, h.id, e.event_type, e.event_time
FROM shipments s
JOIN (
    VALUES
      ('DEL','PICKED_UP',        now() - interval '6 days'),
      ('DEL','DEPARTED_HUB',     now() - interval '5 days 20 hours'),
      ('HYD','ARRIVED_AT_HUB',   now() - interval '4 days'),
      ('HYD','DEPARTED_HUB',     now() - interval '3 days 18 hours'),
      ('CHN','ARRIVED_AT_HUB',   now() - interval '3 days 6 hours'),
      ('CHN','OUT_FOR_DELIVERY', now() - interval '3 days 4 hours'),
      ('CHN','DELIVERED',        now() - interval '3 days 2 hours')
) AS e(hub_code, event_type, event_time) ON TRUE
JOIN hubs h ON h.code = e.hub_code
WHERE s.tracking_no = 'SS0000000001';

-- Shipment 4 (CHN -> AMD, in transit): partial history.
INSERT INTO tracking_events (shipment_id, hub_id, event_type, event_time)
SELECT s.id, h.id, e.event_type, e.event_time
FROM shipments s
JOIN (
    VALUES
      ('CHN','PICKED_UP',      now() - interval '2 days'),
      ('CHN','DEPARTED_HUB',   now() - interval '1 days 20 hours'),
      ('BLR','ARRIVED_AT_HUB', now() - interval '1 days 10 hours'),
      ('BLR','DEPARTED_HUB',   now() - interval '1 days')
) AS e(hub_code, event_type, event_time) ON TRUE
JOIN hubs h ON h.code = e.hub_code
WHERE s.tracking_no = 'SS0000000004';

-- Shipment 6 (PUN -> COK, delayed): shows an EXCEPTION scan.
INSERT INTO tracking_events (shipment_id, hub_id, event_type, event_time)
SELECT s.id, h.id, e.event_type, e.event_time
FROM shipments s
JOIN (
    VALUES
      ('PUN','PICKED_UP',    now() - interval '5 days'),
      ('PUN','DEPARTED_HUB', now() - interval '4 days 20 hours'),
      ('HYD','ARRIVED_AT_HUB', now() - interval '4 days'),
      ('HYD','EXCEPTION',    now() - interval '3 days 12 hours')
) AS e(hub_code, event_type, event_time) ON TRUE
JOIN hubs h ON h.code = e.hub_code
WHERE s.tracking_no = 'SS0000000006';
