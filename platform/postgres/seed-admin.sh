#!/bin/bash
# Crea la cuenta ADMIN en identity_db si no existe. Se corre a mano una vez, desde platform/,
# con la plataforma levantada:  ./postgres/seed-admin.sh [-f docker-compose.yml -f docker-compose.prod.yml]
# Lee ADMIN_EMAIL, ADMIN_PASSWORD_HASH (BCrypt), ADMIN_PHONE y ADMIN_FULL_NAME de .env.
# Ningún secreto vive en el repo: el hash se genera fuera y se guarda solo en .env.
set -euo pipefail

cd "$(dirname "$0")/.."
ENV_FILE=.env

read_env() {
  # Lectura literal: el hash BCrypt trae "$" y no debe expandirse.
  local value
  value=$(grep -E "^$1=" "$ENV_FILE" | tail -n 1 | cut -d= -f2- || true)
  value=${value%$'\r'}
  value=${value#\'}; value=${value%\'}
  value=${value#\"}; value=${value%\"}
  printf '%s' "$value"
}

[ -f "$ENV_FILE" ] || { echo "No existe $ENV_FILE en platform/" >&2; exit 1; }

ADMIN_EMAIL=$(read_env ADMIN_EMAIL)
ADMIN_PASSWORD_HASH=$(read_env ADMIN_PASSWORD_HASH)
ADMIN_PHONE=$(read_env ADMIN_PHONE)
ADMIN_FULL_NAME=$(read_env ADMIN_FULL_NAME)
ADMIN_FULL_NAME=${ADMIN_FULL_NAME:-Administrador GeoPS}

for name in ADMIN_EMAIL ADMIN_PASSWORD_HASH ADMIN_PHONE; do
  [ -n "${!name}" ] || { echo "Falta $name en $ENV_FILE" >&2; exit 1; }
done
[[ "$ADMIN_PASSWORD_HASH" =~ ^\$2[aby]\$[0-9]{2}\$.{53}$ ]] || { echo "ADMIN_PASSWORD_HASH no es un hash BCrypt" >&2; exit 1; }

# Los valores viajan como variables de psql y se citan con :'nombre', nunca concatenados al SQL.
docker compose "$@" exec -T postgres psql -v ON_ERROR_STOP=1 -U postgres -d identity_db \
  -v email="$ADMIN_EMAIL" -v hash="$ADMIN_PASSWORD_HASH" -v phone="$ADMIN_PHONE" -v full_name="$ADMIN_FULL_NAME" <<'SQL'
INSERT INTO users (full_name, email, email_confirmed_at, phone, password_hash, role, failed_login_attempts, created_at)
SELECT :'full_name', lower(trim(:'email')), now(), :'phone', :'hash', 'ADMIN', 0, now()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = lower(trim(:'email')));

SELECT id, email, role, created_at FROM users WHERE role = 'ADMIN';
SQL
