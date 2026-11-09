-- Deadline Scheduler: solo lee reportes abiertos por su plazo y reservas activas por su vencimiento.
CREATE INDEX ix_noncompliance_reports_open_deadline ON noncompliance_reports (response_deadline) WHERE status = 'OPEN';
CREATE INDEX ix_reservations_active_expires ON reservations (expires_at) WHERE status = 'ACTIVE';
-- Listados del comercio y del consumidor, del más nuevo al más viejo.
CREATE INDEX ix_noncompliance_reports_business ON noncompliance_reports (business_id, opened_at DESC);
CREATE INDEX ix_noncompliance_reports_consumer ON noncompliance_reports (consumer_id, opened_at DESC);
