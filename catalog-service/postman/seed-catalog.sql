INSERT INTO merchant_standings (business_id, business_name, ruc_verified, open_reports, compliance_index, verified_seal, updated_at)
VALUES (1, 'Bodega Doña Rosa', false, 0, 100.00, false, now()),
       (2, 'Pollería El Sabor', true, 0, 100.00, true, now());

INSERT INTO campaigns (business_id, name, description, start_date, end_date, status, zone_type, zone_radius_m, estimated_budget)
VALUES (1, 'Almuerzos de octubre', 'Menú ejecutivo a mitad de precio', '2026-10-05', '2026-10-31', 'ACTIVE', 'RADIUS', 800, 500.00),
       (2, 'Pollo a la brasa', 'Cuarto de pollo con papas', '2026-10-01', '2026-10-20', 'ACTIVE', 'RADIUS', 1000, 300.00);

INSERT INTO offers (campaign_id, business_id, title, conditions, price, valid_to, category, geocoding_status, address, source, status, location)
SELECT id, business_id, 'Menú ejecutivo a mitad de precio', 'De lunes a viernes de 12:00 a 15:00', 12.50, '2026-10-31',
       'Gastronomía', 'GEOCODED', 'Jr. Huánuco 1250, La Victoria', 'AFFILIATED', 'PUBLISHED',
       ST_SetSRID(ST_MakePoint(-77.0240, -12.0670), 4326)::geography
FROM campaigns WHERE name = 'Almuerzos de octubre';
