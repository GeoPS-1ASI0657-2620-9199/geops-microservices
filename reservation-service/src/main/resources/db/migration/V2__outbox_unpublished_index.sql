-- El relay lee las filas sin publicar por orden de llegada; el índice parcial solo guarda esas.
CREATE INDEX ix_outbox_events_unpublished ON outbox_events (created_at) WHERE published_at IS NULL;
