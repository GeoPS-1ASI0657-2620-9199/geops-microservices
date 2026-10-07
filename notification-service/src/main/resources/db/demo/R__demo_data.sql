INSERT INTO recipients (user_id, email, email_confirmed, role)
VALUES (1, 'lucia.fernandez@ejemplo.pe', true, 'CONSUMER') ON CONFLICT DO NOTHING;
