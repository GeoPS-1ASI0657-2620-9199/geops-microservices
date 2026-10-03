-- identity_db, generado desde bd-identity.puml

CREATE TABLE users (
    id bigserial NOT NULL,
    full_name varchar(255) NOT NULL,
    email varchar(255) NOT NULL,
    email_confirmed_at timestamptz,
    phone varchar(20) NOT NULL,
    password_hash varchar(255) NOT NULL,
    role varchar(20) NOT NULL,
    failed_login_attempts smallint NOT NULL,
    locked_until timestamptz,
    created_at timestamptz NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_phone UNIQUE (phone)
);

CREATE TABLE consumer_profiles (
    id bigserial NOT NULL,
    user_id bigint NOT NULL,
    location_permission boolean NOT NULL,
    search_radius_minutes smallint NOT NULL,
    default_district varchar(100),
    CONSTRAINT pk_consumer_profiles PRIMARY KEY (id),
    CONSTRAINT uk_consumer_profiles_user_id UNIQUE (user_id),
    CONSTRAINT fk_consumer_profiles_user_id FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE business_profiles (
    id bigserial NOT NULL,
    user_id bigint NOT NULL,
    business_name varchar(150) NOT NULL,
    business_type varchar(100),
    ruc char(11) NOT NULL,
    address varchar(255) NOT NULL,
    latitude numeric(9,6),
    longitude numeric(9,6),
    opening_hours varchar(255),
    account_status varchar(20) NOT NULL,
    verification_status varchar(20) NOT NULL,
    CONSTRAINT pk_business_profiles PRIMARY KEY (id),
    CONSTRAINT uk_business_profiles_user_id UNIQUE (user_id),
    CONSTRAINT uk_business_profiles_ruc UNIQUE (ruc),
    CONSTRAINT fk_business_profiles_user_id FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE ruc_verifications (
    id bigserial NOT NULL,
    business_profile_id bigint NOT NULL,
    ruc char(11) NOT NULL,
    taxpayer_status varchar(30),
    taxpayer_condition varchar(30),
    result varchar(20) NOT NULL,
    rejection_reason varchar(255),
    checked_at timestamptz NOT NULL,
    CONSTRAINT pk_ruc_verifications PRIMARY KEY (id),
    CONSTRAINT fk_ruc_verifications_business_profile_id FOREIGN KEY (business_profile_id) REFERENCES business_profiles (id)
);

CREATE TABLE sunat_taxpayers (
    ruc char(11) NOT NULL,
    business_name varchar(255) NOT NULL,
    taxpayer_status varchar(30) NOT NULL,
    taxpayer_condition varchar(30) NOT NULL,
    loaded_at timestamptz NOT NULL,
    CONSTRAINT pk_sunat_taxpayers PRIMARY KEY (ruc)
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
