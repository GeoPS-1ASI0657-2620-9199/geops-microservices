#!/bin/bash
# Crea la cuenta ADMIN en identity_db si no existe. Se corre a mano una vez, desde platform/,
# con la plataforma levantada:  ./postgres/seed-admin.sh [-f docker-compose.yml -f docker-compose.prod.yml]
# Lee ADMIN_EMAIL, ADMIN_PASSWORD_HASH (BCrypt), ADMIN_PHONE y ADMIN_FULL_NAME de .env.admin,
# un archivo que solo usa este script (ningún servicio lo recibe). Plantilla: .env.admin.example.
set -euo pipefail

cd "$(dirname "$0")/.."
ADMIN_ENV_FILE=.env.admin

read_env() {
  # Lectura literal: el hash BCrypt trae "$" y no debe expandirse.
  local value
  value=$(grep -E "^$1=" "$ADMIN_ENV_FILE" | tail -n 1 | cut -d= -f2- || true)
  value=${value%$'\r'}
  value=${value#\'}; value=${value%\'}
  value=${value#\"}; value=${value%\"}
  printf '%s' "$value"
}

[ -f "$ADMIN_ENV_FILE" ] || { echo "No existe $ADMIN_ENV_FILE en platform/ (copiar .env.admin.example)" >&2; exit 1; }

ADMIN_EMAIL=$(read_env ADMIN_EMAIL)
ADMIN_PASSWORD_HASH=$(read_env ADMIN_PASSWORD_HASH)
ADMIN_PHONE=$(read_env ADMIN_PHONE)
ADMIN_FULL_NAME=$(read_env ADMIN_FULL_NAME)
ADMIN_FULL_NAME=${ADMIN_FULL_NAME:-Administrador GeoPS}

for name in ADMIN_EMAIL ADMIN_PASSWORD_HASH ADMIN_PHONE; do
  [ -n "${!name}" ] || { echo "Falta $name en $ADMIN_ENV_FILE" >&2; exit 1; }
done
[[ "$ADMIN_PASSWORD_HASH" =~ ^\$2[aby]\$[0-9]{2}\$.{53}$ ]] || { echo "ADMIN_PASSWORD_HASH no es un hash BCrypt" >&2; exit 1; }
export ADMIN_EMAIL ADMIN_PASSWORD_HASH ADMIN_PHONE ADMIN_FULL_NAME

# Los valores viajan como variables de entorno del exec (-e NOMBRE, sin valor en la línea de
# comandos), psql los lee con \getenv y se citan con :'nombre', nunca concatenados al SQL.
role=$(docker compose "$@" exec -T -e ADMIN_EMAIL -e ADMIN_PASSWORD_HASH -e ADMIN_PHONE -e ADMIN_FULL_NAME \
  postgres psql -X -q -A -t -v ON_ERROR_STOP=1 -U postgres -d identity_db <<'SQL'
\getenv email ADMIN_EMAIL
\getenv hash ADMIN_PASSWORD_HASH
\getenv phone ADMIN_PHONE
\getenv full_name ADMIN_FULL_NAME
INSERT INTO users (full_name, email, email_confirmed_at, phone, password_hash, role, failed_login_attempts, created_at)
SELECT :'full_name', lower(trim(:'email')), now(), :'phone', :'hash', 'ADMIN', 0, now()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = lower(trim(:'email')));
SELECT role FROM users WHERE email = lower(trim(:'email'));
SQL
)

if [ "$role" != "ADMIN" ]; then
  echo "La cuenta ${ADMIN_EMAIL} existe con rol '${role}', no ADMIN; no se modificó" >&2
  exit 1
fi
echo "Cuenta ADMIN lista: ${ADMIN_EMAIL}"
