-- El panel del comercio muestra su última verificación.
CREATE INDEX ix_ruc_verifications_profile_checked_at
    ON ruc_verifications (business_profile_id, checked_at DESC);
