-- engagement_db, generado desde bd-engagement.puml

CREATE TABLE business_snapshots (
    business_id bigint NOT NULL,
    business_name varchar(150) NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT pk_business_snapshots PRIMARY KEY (business_id)
);

CREATE TABLE offer_snapshots (
    offer_id bigint NOT NULL,
    business_id bigint NOT NULL,
    title varchar(255) NOT NULL,
    valid_to date NOT NULL,
    status varchar(20) NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT pk_offer_snapshots PRIMARY KEY (offer_id),
    CONSTRAINT fk_offer_snapshots_business_id FOREIGN KEY (business_id) REFERENCES business_snapshots (business_id)
);

CREATE TABLE saved_offers (
    id bigserial NOT NULL,
    consumer_id bigint NOT NULL,
    offer_id bigint NOT NULL,
    saved_at timestamptz NOT NULL,
    CONSTRAINT pk_saved_offers PRIMARY KEY (id),
    CONSTRAINT uk_saved_offers_1 UNIQUE (consumer_id, offer_id),
    CONSTRAINT fk_saved_offers_offer_id FOREIGN KEY (offer_id) REFERENCES offer_snapshots (offer_id)
);

CREATE TABLE saved_businesses (
    id bigserial NOT NULL,
    consumer_id bigint NOT NULL,
    business_id bigint NOT NULL,
    saved_at timestamptz NOT NULL,
    CONSTRAINT pk_saved_businesses PRIMARY KEY (id),
    CONSTRAINT uk_saved_businesses_1 UNIQUE (consumer_id, business_id),
    CONSTRAINT fk_saved_businesses_business_id FOREIGN KEY (business_id) REFERENCES business_snapshots (business_id)
);

CREATE TABLE follows (
    id bigserial NOT NULL,
    consumer_id bigint NOT NULL,
    business_id bigint NOT NULL,
    followed_at timestamptz NOT NULL,
    CONSTRAINT pk_follows PRIMARY KEY (id),
    CONSTRAINT uk_follows_1 UNIQUE (consumer_id, business_id),
    CONSTRAINT fk_follows_business_id FOREIGN KEY (business_id) REFERENCES business_snapshots (business_id)
);

CREATE TABLE redeemed_reservations (
    reservation_id bigint NOT NULL,
    consumer_id bigint NOT NULL,
    business_id bigint NOT NULL,
    redeemed_at timestamptz NOT NULL,
    CONSTRAINT pk_redeemed_reservations PRIMARY KEY (reservation_id)
);

CREATE TABLE reviews (
    id bigserial NOT NULL,
    reservation_id bigint NOT NULL,
    consumer_id bigint NOT NULL,
    business_id bigint NOT NULL,
    rating smallint NOT NULL,
    review_text varchar(2000) NOT NULL,
    verified_redemption boolean NOT NULL,
    created_at timestamptz NOT NULL,
    CONSTRAINT pk_reviews PRIMARY KEY (id),
    CONSTRAINT uk_reviews_reservation_id UNIQUE (reservation_id),
    CONSTRAINT fk_reviews_reservation_id FOREIGN KEY (reservation_id) REFERENCES redeemed_reservations (reservation_id),
    CONSTRAINT fk_reviews_business_id FOREIGN KEY (business_id) REFERENCES business_snapshots (business_id)
);

CREATE TABLE review_replies (
    id bigserial NOT NULL,
    review_id bigint NOT NULL,
    business_id bigint NOT NULL,
    reply_text varchar(2000) NOT NULL,
    replied_at timestamptz NOT NULL,
    CONSTRAINT pk_review_replies PRIMARY KEY (id),
    CONSTRAINT uk_review_replies_review_id UNIQUE (review_id),
    CONSTRAINT fk_review_replies_review_id FOREIGN KEY (review_id) REFERENCES reviews (id)
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
