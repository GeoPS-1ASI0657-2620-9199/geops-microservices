-- Orden de publicación estable: created_at puede repetirse o llegar desordenado entre réplicas;
-- seq lo asigna la base al insertar.
ALTER TABLE outbox_events ADD COLUMN seq bigserial NOT NULL;

DROP INDEX ix_outbox_events_pending;
CREATE INDEX ix_outbox_events_pending ON outbox_events (seq) WHERE status = 'PENDING';
