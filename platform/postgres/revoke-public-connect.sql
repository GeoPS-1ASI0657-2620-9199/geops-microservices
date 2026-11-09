-- US16: cada servicio se conecta solo a su base. Para la VM, donde las bases ya existen.
-- Uso: docker compose -f docker-compose.yml -f docker-compose.prod.yml exec -T postgres \
--        psql -v ON_ERROR_STOP=1 -U postgres -d postgres < postgres/revoke-public-connect.sql
REVOKE CONNECT ON DATABASE identity_db FROM PUBLIC;
GRANT CONNECT ON DATABASE identity_db TO identity_user;

REVOKE CONNECT ON DATABASE catalog_db FROM PUBLIC;
GRANT CONNECT ON DATABASE catalog_db TO catalog_user;

REVOKE CONNECT ON DATABASE reservation_db FROM PUBLIC;
GRANT CONNECT ON DATABASE reservation_db TO reservation_user;

REVOKE CONNECT ON DATABASE engagement_db FROM PUBLIC;
GRANT CONNECT ON DATABASE engagement_db TO engagement_user;

REVOKE CONNECT ON DATABASE notification_db FROM PUBLIC;
GRANT CONNECT ON DATABASE notification_db TO notification_user;

SELECT datname, datacl FROM pg_database WHERE datname LIKE '%\_db' ORDER BY datname;
