-- US26: sesiones cerradas antes de que venza su token. Se guarda el SHA-256 del token, nunca el token, y solo
-- hasta expires_at: después el token se rechaza por vencido y la fila se borra.
CREATE TABLE revoked_tokens (
    fingerprint char(64) NOT NULL,
    user_id bigint NOT NULL,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz NOT NULL,
    CONSTRAINT pk_revoked_tokens PRIMARY KEY (fingerprint)
);

CREATE INDEX idx_revoked_tokens_expires_at ON revoked_tokens (expires_at);
