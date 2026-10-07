-- notification_db, generado desde bd-notification.puml

CREATE TABLE recipients (
    user_id bigint NOT NULL,
    email varchar(255) NOT NULL,
    email_confirmed boolean NOT NULL,
    role varchar(20) NOT NULL,
    CONSTRAINT pk_recipients PRIMARY KEY (user_id)
);

CREATE TABLE notification_preferences (
    consumer_id bigint NOT NULL,
    push_enabled boolean NOT NULL,
    email_enabled boolean NOT NULL,
    daily_limit smallint NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT pk_notification_preferences PRIMARY KEY (consumer_id),
    CONSTRAINT fk_notification_preferences_consumer_id FOREIGN KEY (consumer_id) REFERENCES recipients (user_id)
);

CREATE TABLE push_subscriptions (
    id bigserial NOT NULL,
    consumer_id bigint NOT NULL,
    fcm_token varchar(500) NOT NULL,
    user_agent varchar(255),
    created_at timestamptz NOT NULL,
    CONSTRAINT pk_push_subscriptions PRIMARY KEY (id),
    CONSTRAINT uk_push_subscriptions_fcm_token UNIQUE (fcm_token),
    CONSTRAINT fk_push_subscriptions_consumer_id FOREIGN KEY (consumer_id) REFERENCES notification_preferences (consumer_id)
);

CREATE TABLE last_known_locations (
    consumer_id bigint NOT NULL,
    position geography(Point, 4326) NOT NULL,
    accuracy_m integer NOT NULL,
    radius_m integer NOT NULL,
    captured_at timestamptz NOT NULL,
    CONSTRAINT pk_last_known_locations PRIMARY KEY (consumer_id),
    CONSTRAINT fk_last_known_locations_consumer_id FOREIGN KEY (consumer_id) REFERENCES notification_preferences (consumer_id)
);

CREATE INDEX ix_last_known_locations_position ON last_known_locations USING GIST (position);

CREATE TABLE email_unsubscriptions (
    id bigserial NOT NULL,
    consumer_id bigint NOT NULL,
    notification_type varchar(40) NOT NULL,
    token varchar(64) NOT NULL,
    unsubscribed_at timestamptz NOT NULL,
    CONSTRAINT pk_email_unsubscriptions PRIMARY KEY (id),
    CONSTRAINT uk_email_unsubscriptions_token UNIQUE (token),
    CONSTRAINT fk_email_unsubscriptions_consumer_id FOREIGN KEY (consumer_id) REFERENCES notification_preferences (consumer_id)
);

CREATE TABLE notifications (
    id bigserial NOT NULL,
    recipient_id bigint NOT NULL,
    notification_type varchar(40) NOT NULL,
    channel varchar(20) NOT NULL,
    title varchar(200) NOT NULL,
    message varchar(500) NOT NULL,
    related_entity_id varchar(50),
    delivery_status varchar(20) NOT NULL,
    sent_at timestamptz NOT NULL,
    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT fk_notifications_recipient_id FOREIGN KEY (recipient_id) REFERENCES recipients (user_id)
);

CREATE TABLE email_digests (
    id bigserial NOT NULL,
    consumer_id bigint NOT NULL,
    window_start timestamptz NOT NULL,
    window_end timestamptz NOT NULL,
    offer_ids bigint[] NOT NULL,
    sent_at timestamptz,
    CONSTRAINT pk_email_digests PRIMARY KEY (id),
    CONSTRAINT fk_email_digests_consumer_id FOREIGN KEY (consumer_id) REFERENCES recipients (user_id)
);

CREATE TABLE followed_businesses (
    consumer_id bigint NOT NULL,
    business_id bigint NOT NULL,
    CONSTRAINT pk_followed_businesses PRIMARY KEY (consumer_id, business_id),
    CONSTRAINT fk_followed_businesses_consumer_id FOREIGN KEY (consumer_id) REFERENCES recipients (user_id)
);
