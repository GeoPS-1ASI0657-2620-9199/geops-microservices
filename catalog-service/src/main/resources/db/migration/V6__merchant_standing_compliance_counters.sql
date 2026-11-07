-- Índice de cumplimiento (US44, S3-09): canjes registrados y reportes cerrados a favor del consumidor.
ALTER TABLE merchant_standings
    ADD COLUMN redeemed_reservations integer NOT NULL DEFAULT 0,
    ADD COLUMN consumer_favor_reports integer NOT NULL DEFAULT 0;
