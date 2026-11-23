-- Métricas por campaña (US11) y línea base (US45).
-- campaign_daily_metrics ya existía desde V1 sin código encima: se agregan las reservas y los contadores arrancan en 0,
-- porque cada fuente (búsqueda, detalle, reservas, canjes) suma solo su columna con un upsert.
ALTER TABLE campaign_daily_metrics
    ADD COLUMN reservations bigint NOT NULL DEFAULT 0,
    ALTER COLUMN impressions SET DEFAULT 0,
    ALTER COLUMN clicks SET DEFAULT 0,
    ALTER COLUMN conversions SET DEFAULT 0;

COMMENT ON COLUMN campaign_daily_metrics.impressions IS 'Veces que una oferta de la campaña apareció en la búsqueda cercana';
COMMENT ON COLUMN campaign_daily_metrics.clicks IS 'Veces que se abrió el detalle de una oferta de la campaña';
COMMENT ON COLUMN campaign_daily_metrics.reservations IS 'ReservationCreated de ofertas de la campaña';
COMMENT ON COLUMN campaign_daily_metrics.conversions IS 'ReservationRedeemed de ofertas de la campaña (canjes)';

-- processed_events guarda un registro por consumidor: el mismo ReservationRedeemed cambia el cumplimiento del
-- comercio (reputation) y suma un canje a la campaña (campaign-metrics), y cada uno descarta su propia repetición.
ALTER TABLE processed_events
    ADD COLUMN consumer varchar(40) NOT NULL DEFAULT 'reputation';
ALTER TABLE processed_events DROP CONSTRAINT pk_processed_events;
ALTER TABLE processed_events ADD CONSTRAINT pk_processed_events PRIMARY KEY (event_id, consumer);
