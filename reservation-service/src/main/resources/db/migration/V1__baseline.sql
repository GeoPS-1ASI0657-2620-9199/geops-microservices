-- reservation_db, generado desde bd-reservation.puml

CREATE TABLE reservations (
    id bigserial NOT NULL,
    code varchar(12) NOT NULL,
    consumer_id bigint NOT NULL,
    offer_id bigint NOT NULL,
    business_id bigint NOT NULL,
    offer_title varchar(255) NOT NULL,
    reserved_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    redeemed_at timestamptz,
    status varchar(20) NOT NULL,
    CONSTRAINT pk_reservations PRIMARY KEY (id),
    CONSTRAINT uk_reservations_code UNIQUE (code)
);

CREATE TABLE noncompliance_reports (
    id bigserial NOT NULL,
    reservation_id bigint NOT NULL,
    consumer_id bigint NOT NULL,
    business_id bigint NOT NULL,
    reason varchar(500) NOT NULL,
    opened_at timestamptz NOT NULL,
    response_deadline timestamptz NOT NULL,
    status varchar(20) NOT NULL,
    resolution varchar(20),
    resolved_at timestamptz,
    CONSTRAINT pk_noncompliance_reports PRIMARY KEY (id),
    CONSTRAINT uk_noncompliance_reports_reservation_id UNIQUE (reservation_id),
    CONSTRAINT fk_noncompliance_reports_reservation_id FOREIGN KEY (reservation_id) REFERENCES reservations (id)
);

CREATE TABLE report_responses (
    id bigserial NOT NULL,
    report_id bigint NOT NULL,
    response_text text NOT NULL,
    responded_at timestamptz NOT NULL,
    CONSTRAINT pk_report_responses PRIMARY KEY (id),
    CONSTRAINT uk_report_responses_report_id UNIQUE (report_id),
    CONSTRAINT fk_report_responses_report_id FOREIGN KEY (report_id) REFERENCES noncompliance_reports (id)
);

CREATE TABLE outbox_events (
    id uuid NOT NULL,
    aggregate_id bigint NOT NULL,
    event_type varchar(80) NOT NULL,
    payload jsonb NOT NULL,
    created_at timestamptz NOT NULL,
    published_at timestamptz,
    CONSTRAINT pk_outbox_events PRIMARY KEY (id)
);

CREATE UNIQUE INDEX ux_reservations_consumer_offer_active ON reservations (consumer_id, offer_id) WHERE status = 'ACTIVE';
