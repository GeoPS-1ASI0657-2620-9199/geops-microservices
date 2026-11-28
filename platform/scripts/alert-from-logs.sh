#!/bin/bash
# Alerta simple (ajuste 8): en los últimos ALERT_WINDOW cuenta los 401 y 403 por IP en el gateway y los canjes
# rechazados por comercio en Reservation; si alguno pasa su umbral, avisa por correo SMTP.
# Uso, desde platform/: ./scripts/alert-from-logs.sh   (cron cada 5 minutos en la VM)
# Obligatorias: ALERT_TO, ALERT_FROM, ALERT_SMTP_URL (smtp://localhost:1025 en local).
# Solo con SMTP autenticado: ALERT_SMTP_USER y ALERT_SMTP_PASSWORD, desde un archivo fuera del repo.
set -euo pipefail
export MSYS_NO_PATHCONV=1
cd "$(dirname "$0")/.."
read -r -a COMPOSE <<< "docker compose ${COMPOSE_ARGS:--f docker-compose.yml}"
window=${ALERT_WINDOW:-5m}
auth_threshold=${ALERT_AUTH_THRESHOLD:-20}
redeem_threshold=${ALERT_REDEEM_THRESHOLD:-10}
cooldown_minutes=${ALERT_COOLDOWN_MINUTES:-30}
state_file=${ALERT_STATE_FILE:-/tmp/geops-alert.last}
to=${ALERT_TO:?correo que recibe la alerta}
from=${ALERT_FROM:?remitente}
smtp_url=${ALERT_SMTP_URL:?por ejemplo smtp://localhost:1025}

over_threshold() { sort | uniq -c | awk -v limit="$1" '$1 >= limit { print $2 " (" $1 ")" }'; }

auth=$("${COMPOSE[@]}" logs --no-log-prefix --since "$window" gateway \
  | sed -nE 's/^([0-9a-fA-F.:]+) \[[^]]*\] "[^"]*" (401|403) .*/\1/p' | over_threshold "$auth_threshold")
redeem=$("${COMPOSE[@]}" logs --no-log-prefix --since "$window" reservation-service \
  | sed -nE 's/.*reservation\.redeem\.rejected .*businessId=([0-9]+).*/\1/p' | over_threshold "$redeem_threshold")

[ -z "$auth$redeem" ] && exit 0
if [ -f "$state_file" ] && [ -n "$(find "$state_file" -mmin -"$cooldown_minutes")" ]; then
  echo "alert.suppressed"; exit 0
fi

message=$(mktemp)
{
  printf 'From: %s\r\nTo: %s\r\nSubject: [GeoPS] Alerta de seguridad\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\n' "$from" "$to"
  [ -n "$auth" ] && printf 'Respuestas 401/403 por IP en los últimos %s (umbral %s):\r\n%s\r\n\r\n' "$window" "$auth_threshold" "$auth"
  [ -n "$redeem" ] && printf 'Canjes rechazados por comercio en los últimos %s (umbral %s):\r\n%s\r\n' "$window" "$redeem_threshold" "$redeem"
} > "$message"
curl_args=(-sS --url "$smtp_url" --mail-from "$from" --mail-rcpt "$to" --upload-file "$message")
if [ -n "${ALERT_SMTP_USER:-}" ]; then curl_args+=(--ssl-reqd --user "$ALERT_SMTP_USER:${ALERT_SMTP_PASSWORD:?}"); fi
curl "${curl_args[@]}"
touch "$state_file"; rm -f "$message"
echo "alert.sent"
