-- catalog_db, generado desde bd-catalog.puml

CREATE TABLE merchant_standings (
    business_id bigint NOT NULL,
    business_name varchar(150) NOT NULL,
    ruc_verified boolean NOT NULL,
    open_reports integer NOT NULL,
    compliance_index numeric(5,2) NOT NULL,
    verified_seal boolean NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT pk_merchant_standings PRIMARY KEY (business_id)
);

CREATE TABLE campaigns (
    id bigserial NOT NULL,
    business_id bigint NOT NULL,
    name varchar(150) NOT NULL,
    description text NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    status varchar(20) NOT NULL,
    zone_type varchar(10) NOT NULL,
    zone_center geography(Point, 4326),
    zone_radius_m integer,
    zone_district varchar(100),
    estimated_budget numeric(10,2) NOT NULL,
    CONSTRAINT pk_campaigns PRIMARY KEY (id),
    CONSTRAINT fk_campaigns_business_id FOREIGN KEY (business_id) REFERENCES merchant_standings (business_id)
);

CREATE INDEX ix_campaigns_zone_center ON campaigns USING GIST (zone_center);

CREATE TABLE offers (
    id bigserial NOT NULL,
    campaign_id bigint,
    business_id bigint,
    title varchar(255) NOT NULL,
    conditions text NOT NULL,
    price numeric(10,2) NOT NULL,
    valid_to date NOT NULL,
    category varchar(100) NOT NULL,
    geocoding_status varchar(20) NOT NULL,
    address varchar(255) NOT NULL,
    image_url varchar(500),
    source varchar(20) NOT NULL,
    source_name varchar(150),
    status varchar(20) NOT NULL,
    CONSTRAINT pk_offers PRIMARY KEY (id),
    CONSTRAINT ck_offers_1 CHECK (source = 'PUBLIC_SOURCE' OR campaign_id IS NOT NULL),
    CONSTRAINT fk_offers_campaign_id FOREIGN KEY (campaign_id) REFERENCES campaigns (id)
);

CREATE TABLE moderation_actions (
    id bigserial NOT NULL,
    offer_id bigint NOT NULL,
    admin_id bigint NOT NULL,
    reason varchar(255) NOT NULL,
    removed_at timestamptz NOT NULL,
    CONSTRAINT pk_moderation_actions PRIMARY KEY (id),
    CONSTRAINT fk_moderation_actions_offer_id FOREIGN KEY (offer_id) REFERENCES offers (id)
);

CREATE TABLE campaign_daily_metrics (
    id bigserial NOT NULL,
    campaign_id bigint NOT NULL,
    metric_date date NOT NULL,
    impressions bigint NOT NULL,
    clicks bigint NOT NULL,
    conversions bigint NOT NULL,
    CONSTRAINT pk_campaign_daily_metrics PRIMARY KEY (id),
    CONSTRAINT uk_campaign_daily_metrics_1 UNIQUE (campaign_id, metric_date),
    CONSTRAINT fk_campaign_daily_metrics_campaign_id FOREIGN KEY (campaign_id) REFERENCES campaigns (id)
);

CREATE TABLE business_baselines (
    business_id bigint NOT NULL,
    customers_per_week integer NOT NULL,
    average_ticket numeric(10,2) NOT NULL,
    declared_at timestamptz NOT NULL,
    CONSTRAINT pk_business_baselines PRIMARY KEY (business_id),
    CONSTRAINT fk_business_baselines_business_id FOREIGN KEY (business_id) REFERENCES merchant_standings (business_id)
);

CREATE TABLE standing_changes (
    id bigserial NOT NULL,
    business_id bigint NOT NULL,
    verified_seal boolean NOT NULL,
    reason varchar(255) NOT NULL,
    changed_at timestamptz NOT NULL,
    CONSTRAINT pk_standing_changes PRIMARY KEY (id),
    CONSTRAINT fk_standing_changes_business_id FOREIGN KEY (business_id) REFERENCES merchant_standings (business_id)
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

CREATE TABLE processed_events (
    event_id uuid NOT NULL,
    event_type varchar(80) NOT NULL,
    processed_at timestamptz NOT NULL,
    CONSTRAINT pk_processed_events PRIMARY KEY (event_id)
);
