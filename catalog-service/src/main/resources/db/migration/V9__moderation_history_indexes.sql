-- Moderación de ofertas (US24): el historial se lee del más reciente al más antiguo y se consulta por oferta.
CREATE INDEX ix_moderation_actions_removed_at ON moderation_actions (removed_at DESC, id DESC);
CREATE INDEX ix_moderation_actions_offer_id ON moderation_actions (offer_id);
