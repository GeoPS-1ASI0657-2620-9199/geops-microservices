-- Una fila que Kafka rechaza siempre deja de bloquear la bandeja: se cuentan los intentos y,
-- al llegar al máximo, la fila queda FAILED con su último error y el relay sigue con las demás.
ALTER TABLE outbox_events
    ADD COLUMN status varchar(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN attempts integer NOT NULL DEFAULT 0,
    ADD COLUMN last_error varchar(500);

UPDATE outbox_events SET status = 'PUBLISHED' WHERE published_at IS NOT NULL;

DROP INDEX ix_outbox_events_unpublished;
CREATE INDEX ix_outbox_events_pending ON outbox_events (created_at) WHERE status = 'PENDING';
