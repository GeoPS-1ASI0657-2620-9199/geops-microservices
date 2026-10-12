-- QAS02: 10 000 ofertas activas repartidas en los 43 distritos de Lima Metropolitana (los mismos centros que usa
-- qas02-nearby.js), hasta 1,5 km alrededor de cada centro, con las once categorías. Así la prueba lee muchas celdas
-- distintas y el acierto de caché no sale de unas pocas celdas calientes (riesgo R-06 del BRD-08).
-- Se borra con:
--   DELETE FROM offers WHERE campaign_id = 9001; DELETE FROM campaigns WHERE id = 9001;
--   DELETE FROM merchant_standings WHERE business_id = 9001;
SELECT setseed(0.42);

INSERT INTO merchant_standings (business_id, business_name, ruc_verified, open_reports, compliance_index, verified_seal, updated_at)
VALUES (9001, 'Comercio de medición', true, 0, 100.00, true, now())
ON CONFLICT (business_id) DO NOTHING;

INSERT INTO campaigns (id, business_id, name, description, start_date, end_date, status, zone_type, zone_radius_m, estimated_budget)
VALUES (9001, 9001, 'Campaña de medición', 'Datos para la prueba de carga', CURRENT_DATE - 1, CURRENT_DATE + 30, 'ACTIVE', 'RADIUS', 1600, 0)
ON CONFLICT (id) DO NOTHING;

WITH districts (n, lat, lng) AS (
    VALUES (1, -12.1211, -77.0297), (2, -12.0975, -77.0365), (3, -12.0464, -77.0306), (4, -12.1404, -76.9914),
           (5, -12.0874, -77.0500), (6, -11.7742, -77.1736), (7, -11.8586, -77.0750), (8, -12.0880, -77.0498),
           (9, -12.0590, -77.0500), (10, -12.1467, -77.0219), (11, -12.0453, -77.0108), (12, -11.8770, -77.0182),
           (13, -12.0333, -76.9300), (14, -12.0500, -76.9667), (15, -12.0800, -77.0000), (16, -11.9897, -77.0617),
           (17, -12.0731, -77.0806), (18, -12.0828, -77.0317), (19, -12.0647, -77.0367), (20, -12.0661, -77.0167),
           (21, -12.0889, -76.9922), (22, -11.9333, -77.0500), (23, -12.0833, -77.0833), (24, -12.0617, -77.0447),
           (25, -11.9833, -77.0083), (26, -11.9681, -77.0650), (27, -12.1000, -77.0475), (28, -12.0333, -77.0167),
           (29, -12.0236, -76.9878), (30, -12.1233, -77.0050), (31, -12.1083, -76.9967), (32, -11.9581, -76.9983),
           (33, -12.0742, -76.9472), (34, -12.1500, -76.9833), (35, -12.1750, -77.0167), (36, -12.1967, -76.9533),
           (37, -12.1667, -76.9333), (38, -12.2667, -76.9167), (39, -12.0464, -77.0428), (40, -12.0217, -77.0533),
           (41, -12.0083, -77.0917), (42, -12.0592, -77.1361), (43, -12.0600, -77.1000)
),
categories (k, name) AS (
    VALUES (0, 'Gastronomía'), (1, 'Cafetería y panadería'), (2, 'Bodega y minimarket'), (3, 'Salud y farmacia'),
           (4, 'Belleza y cuidado personal'), (5, 'Deporte'), (6, 'Mascotas'), (7, 'Hogar y ferretería'),
           (8, 'Librería y útiles'), (9, 'Servicios'), (10, 'Cultura y entretenimiento')
),
points AS (
    SELECT g,
           1 + (g % 43)        AS district,
           g % 11              AS category,
           random() * 1500     AS meters,
           random() * 2 * pi() AS angle
    FROM generate_series(1, 10000) AS g
)
INSERT INTO offers (campaign_id, business_id, title, conditions, price, valid_to, category, location,
                    geocoding_status, address, source, status)
SELECT 9001, 9001, 'Oferta ' || p.g, 'Condiciones de medición', 10.00, CURRENT_DATE + 30, c.name,
       ST_SetSRID(ST_MakePoint(d.lng + p.meters * sin(p.angle) / (111320 * cos(radians(d.lat))),
                               d.lat + p.meters * cos(p.angle) / 111320), 4326)::geography,
       'GEOCODED', 'Dirección ' || p.g, 'AFFILIATED', 'PUBLISHED'
FROM points p
JOIN districts d ON d.n = p.district
JOIN categories c ON c.k = p.category;

ANALYZE offers;
