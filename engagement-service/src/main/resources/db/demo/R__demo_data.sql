INSERT INTO business_snapshots (business_id, business_name, updated_at)
VALUES (1, 'Bodega Doña Rosa', now()) ON CONFLICT DO NOTHING;

INSERT INTO offer_snapshots (offer_id, business_id, title, valid_to, status, updated_at) VALUES
  (1, 1, 'Menú ejecutivo a mitad de precio', DATE '2026-10-31', 'PUBLISHED', now()),
  (2, 1, 'Desayuno criollo a S/ 8', DATE '2026-09-30', 'EXPIRED', now())
ON CONFLICT DO NOTHING;

INSERT INTO redeemed_reservations (reservation_id, consumer_id, business_id, redeemed_at)
VALUES (1, 1, 1, now()) ON CONFLICT DO NOTHING;
