-- QAS02: las mismas 10 000 ofertas de src/test/resources/seed/proximity-10000-offers.sql, repartidas sobre Lima
-- Centro, Miraflores, San Isidro y Surco, para la prueba de carga local. Se borra con:
--   DELETE FROM offers WHERE campaign_id = 9001; DELETE FROM campaigns WHERE id = 9001;
--   DELETE FROM merchant_standings WHERE business_id = 9001;
SELECT setseed(0.42);

INSERT INTO merchant_standings (business_id, business_name, ruc_verified, open_reports, compliance_index, verified_seal, updated_at)
VALUES (9001, 'Comercio de medición', true, 0, 100.00, true, now())
ON CONFLICT (business_id) DO NOTHING;

INSERT INTO campaigns (id, business_id, name, description, start_date, end_date, status, zone_type, zone_radius_m, estimated_budget)
VALUES (9001, 9001, 'Campaña de medición', 'Datos para la prueba de carga', CURRENT_DATE - 1, CURRENT_DATE + 30, 'ACTIVE', 'RADIUS', 1600, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO offers (campaign_id, business_id, title, conditions, price, valid_to, category, location,
                    geocoding_status, address, source, status)
SELECT 9001, 9001, 'Oferta ' || g, 'Condiciones de medición', 10.00, CURRENT_DATE + 30, 'Gastronomía',
       ST_SetSRID(ST_MakePoint(-77.15 + random() * 0.25, -12.25 + random() * 0.30), 4326)::geography,
       'GEOCODED', 'Dirección ' || g, 'AFFILIATED', 'PUBLISHED'
FROM generate_series(1, 10000) AS g;

ANALYZE offers;
