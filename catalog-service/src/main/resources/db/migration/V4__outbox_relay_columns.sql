-- Bandeja de salida de Identity con el mismo diseño que la de Reservation:
-- status, intentos y último error para que una fila que Kafka rechaza siempre no bloquee a las demás,
-- y seq para publicar en el orden en que se escribieron, aunque created_at se repita.
ALTER TABLE outbox_events
    ADD COLUMN status varchar(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN attempts integer NOT NULL DEFAULT 0,
    ADD COLUMN last_error varchar(500),
    ADD COLUMN seq bigserial NOT NULL;

UPDATE outbox_events SET status = 'PUBLISHED' WHERE published_at IS NOT NULL;

CREATE INDEX ix_outbox_events_pending ON outbox_events (seq) WHERE status = 'PENDING';
