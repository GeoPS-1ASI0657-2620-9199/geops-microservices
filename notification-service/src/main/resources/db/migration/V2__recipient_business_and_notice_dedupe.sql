ALTER TABLE recipients ADD COLUMN business_id bigint;
CREATE UNIQUE INDEX ux_recipients_business_id ON recipients (business_id) WHERE business_id IS NOT NULL;
CREATE UNIQUE INDEX ux_notifications_recipient_type_entity ON notifications (recipient_id, notification_type, related_entity_id) WHERE related_entity_id IS NOT NULL;
CREATE INDEX ix_notifications_recipient_sent_at ON notifications (recipient_id, sent_at);
CREATE INDEX ix_email_digests_pending ON email_digests (window_end) WHERE sent_at IS NULL;
CREATE INDEX ix_followed_businesses_business ON followed_businesses (business_id);
