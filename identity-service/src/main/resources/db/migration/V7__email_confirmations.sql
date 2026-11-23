-- US28: enlace de confirmación del correo. Solo se guarda el hash; el enlace viaja en UserRegistered
-- y Notification envía el correo, así Identity no llama a Notification.
CREATE TABLE email_confirmations (
    user_id bigint NOT NULL,
    token_hash varchar(64) NOT NULL,
    expires_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL,
    CONSTRAINT pk_email_confirmations PRIMARY KEY (user_id),
    CONSTRAINT uk_email_confirmations_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_email_confirmations_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
