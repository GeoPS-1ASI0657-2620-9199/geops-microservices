-- Notification conoce a cada usuario por UserRegistered (BRD-11, RF-04). Esta carga cubre a los
-- que se registraron antes de que existiera el evento. Notification lo aplica por user_id, así que
-- una segunda entrega no cambia nada.
INSERT INTO outbox_events (id, aggregate_id, event_type, payload, created_at)
SELECT gen_random_uuid(), u.id, 'UserRegistered',
       jsonb_build_object(
           'eventId', NULL, 'eventType', 'UserRegistered', 'version', 1,
           'occurredAt', to_char(u.created_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS"Z"'),
           'aggregateId', u.id,
           'payload', jsonb_build_object(
               'userId', u.id, 'email', u.email, 'role', u.role, 'businessId', bp.id,
               'emailConfirmed', u.email_confirmed_at IS NOT NULL,
               'registeredAt', to_char(u.created_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS"Z"'))),
       now()
FROM users u
LEFT JOIN business_profiles bp ON bp.user_id = u.id
WHERE u.role <> 'ADMIN';

UPDATE outbox_events SET payload = jsonb_set(payload, '{eventId}', to_jsonb(id::text))
WHERE event_type = 'UserRegistered' AND payload->>'eventId' IS NULL;
