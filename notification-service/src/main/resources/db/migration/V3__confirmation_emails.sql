-- US28: un solo correo de confirmación por usuario aunque UserRegistered llegue dos veces.
CREATE TABLE confirmation_emails (
    user_id bigint NOT NULL,
    sent_at timestamptz NOT NULL,
    CONSTRAINT pk_confirmation_emails PRIMARY KEY (user_id)
);
