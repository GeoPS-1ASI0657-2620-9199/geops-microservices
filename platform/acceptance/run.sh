#!/bin/bash
# Pasos de US16, US17, US25, US27 y US33 contra la plataforma levantada con Docker Compose, en local (C2-01).
# Uso, desde platform/:
#   docker compose --profile app --profile messaging up -d --build
#   ./acceptance/run.sh [us16|us17|us25|us27|us33|all]
# Variables opcionales:
#   COMPOSE_ARGS  archivos de compose (por defecto "-f docker-compose.yml")
#   GATEWAY_URL   puerta de entrada (por defecto http://localhost:8080)
#   SKIP_MAVEN=1  no corre las pruebas de ArchUnit (si ya pasaron en CI)
#   US27_SAMPLES  peticiones por medición del sobrecosto de US27 (por defecto 200)
set -uo pipefail

cd "$(dirname "$0")/.."
export COMPOSE_PROFILES=${COMPOSE_PROFILES:-app,messaging}
read -r -a COMPOSE <<< "docker compose ${COMPOSE_ARGS:--f docker-compose.yml}"
GATEWAY_URL=${GATEWAY_URL:-http://localhost:8080}
SERVICES=(identity catalog reservation engagement notification)
NEARBY_PATH="/api/v1/offers/nearby?lat=-12.0697&lng=-77.0240&radiusMinutes=10"
# Tablas de infraestructura que cada servicio tiene por su cuenta; no son datos de otro contexto.
OWN_INFRA_TABLES="flyway_schema_history|outbox_events|processed_events|spatial_ref_sys"

passed=0
failed=0

step() { printf '  %-72s' "$1"; }
ok() { echo "PASS${1:+  ($1)}"; passed=$((passed + 1)); }
ko() { echo "FAIL${1:+  ($1)}"; failed=$((failed + 1)); }
psql_db() { "${COMPOSE[@]}" exec -T postgres psql -X -A -t -U postgres -d "$1" -c "$2"; }
started_at() { docker inspect -f '{{.State.StartedAt}}' "$1"; }
catalog_ids() { "${COMPOSE[@]}" ps -q catalog-service; }
identity_health() { docker inspect -f '{{.State.Health.Status}}' $("${COMPOSE[@]}" ps -q identity-service) 2>/dev/null; }
identity_healthy() { [ "$(identity_health)" = "healthy" ]; }
# Maven no arranca con MSYS_NO_PATHCONV=1 (lo exportan los pasos de US33), así que se quita para él.
archunit_passes() {
  (cd "../$1-service" && env -u MSYS_NO_PATHCONV ./mvnw -q test -Dtest=ContextBoundariesArchTest \
    -Dsurefire.failIfNoSpecifiedTests=false > /dev/null 2>&1)
}

wait_catalog_healthy() {
  local expected=$1 healthy=0
  for _ in $(seq 1 60); do
    healthy=0
    for id in $(catalog_ids); do
      [ "$(docker inspect -f '{{.State.Health.Status}}' "$id")" = "healthy" ] && healthy=$((healthy + 1))
    done
    [ "$healthy" -ge "$expected" ] && break
    sleep 5
  done
  echo "$healthy"
}

us16() {
  echo "Feature: US16 Reference other contexts only by identifier"

  echo " Scenario: Every aggregate refers to other contexts by identifier"
  if [ "${SKIP_MAVEN:-0}" = "1" ]; then
    echo "  (SKIP_MAVEN=1: ArchUnit no se ejecuta)"
  else
    for service in "${SERVICES[@]}"; do
      step "ContextBoundariesArchTest in ${service}-service"
      if archunit_passes "$service"; then ok; else ko; fi
    done
  fi

  echo " Scenario: There is no integrity constraint between tables of different services"
  step "verificar-independencia.sh (cross foreign keys, cross connections)"
  local report
  if report=$(./scripts/verificar-independencia.sh ${COMPOSE_ARGS:--f docker-compose.yml}); then
    ok "$(printf '%s\n' "$report" | grep -E 'cruzadas|rechazadas' | tr -s ' ' | paste -sd ';' -)"
  else
    ko; printf '%s\n' "$report" | sed 's/^/    /'
  fi

  echo " Scenario: Data of another context comes from its published operation"
  local offer_id
  offer_id=$(psql_db catalog_db "SELECT id FROM offers ORDER BY id LIMIT 1" | tr -d '[:space:]')
  step "Reservation reaches GET /internal/v1/offers/${offer_id:-?}/availability"
  if [ -z "$offer_id" ]; then
    ko "catalog_db has no offers to ask for"
  else
    local code
    code=$("${COMPOSE[@]}" exec -T reservation-service curl -s -o /dev/null -w '%{http_code}' \
      "http://catalog-service:8082/internal/v1/offers/${offer_id}/availability")
    [ "$code" = "200" ] && ok "HTTP $code" || ko "HTTP $code"
  fi

  step "The gateway does not publish /internal/"
  local public_code
  public_code=$(curl -s -o /dev/null -w '%{http_code}' "${GATEWAY_URL}/internal/v1/offers/${offer_id:-1}/availability")
  [ "$public_code" = "404" ] && ok "HTTP $public_code" || ko "HTTP $public_code"

  step "reservation_db has no table of catalog_db"
  local catalog_tables reservation_tables shared
  catalog_tables=$(psql_db catalog_db "SELECT tablename FROM pg_tables WHERE schemaname = 'public'" | grep -vxE "$OWN_INFRA_TABLES")
  reservation_tables=$(psql_db reservation_db "SELECT tablename FROM pg_tables WHERE schemaname = 'public'" | grep -vxE "$OWN_INFRA_TABLES")
  shared=$(comm -12 <(printf '%s\n' "$catalog_tables" | sort) <(printf '%s\n' "$reservation_tables" | sort) | grep . || true)
  [ -z "$shared" ] && ok || ko "shared: $(printf '%s' "$shared" | paste -sd, -)"
}

us17() {
  echo "Feature: US17 Deploy the offer catalog as an independent service"

  echo " Scenario: Raising the catalog capacity does not redeploy the other services"
  local started scale_start
  "${COMPOSE[@]}" up -d --no-deps --scale catalog-service=1 catalog-service > /dev/null 2>&1
  step "The platform is running with one catalog-service replica"
  [ "$(wait_catalog_healthy 1)" -ge 1 ] && [ "$(catalog_ids | wc -l)" -eq 1 ] && ok || ko

  declare -A before
  for id in $("${COMPOSE[@]}" ps -q); do
    [[ " $(catalog_ids | tr '\n' ' ') " == *" $id "* ]] && continue
    before[$id]=$(started_at "$id")
  done
  scale_start=$(date -u +%Y-%m-%dT%H:%M:%SZ)
  "${COMPOSE[@]}" up -d --no-deps --scale catalog-service=2 catalog-service > /dev/null 2>&1

  step "2 catalog-service replicas are healthy"
  local healthy
  healthy=$(wait_catalog_healthy 2)
  [ "$healthy" -eq 2 ] && ok "$healthy healthy" || ko "$healthy healthy"

  step "Every other container keeps its start time (${#before[@]} containers)"
  local restarted=0
  for id in "${!before[@]}"; do
    [ "$(started_at "$id")" = "${before[$id]}" ] || restarted=$((restarted + 1))
  done
  [ "$restarted" -eq 0 ] && ok || ko "$restarted restarted"

  step "20 nearby searches through the gateway are answered by both replicas"
  # nginx resuelve catalog-service cada 10 s (resolver valid=10s); se reparte en el tiempo
  # hasta ver las dos direcciones, con un tope de 60 peticiones.
  local sent=0 answered=0 upstreams=0
  while [ "$sent" -lt 60 ]; do
    [ "$(curl -s -o /dev/null -w '%{http_code}' "${GATEWAY_URL}${NEARBY_PATH}")" = "200" ] && answered=$((answered + 1))
    sent=$((sent + 1))
    if [ "$sent" -ge 20 ]; then
      upstreams=$("${COMPOSE[@]}" logs --no-log-prefix --since "$scale_start" gateway 2>/dev/null \
        | grep 'offers/nearby' | grep -o 'upstream=[0-9.]*:8082' | sort -u | wc -l)
      [ "$upstreams" -ge 2 ] && break
    fi
    sleep 1
  done
  [ "$answered" -eq "$sent" ] && [ "$upstreams" -ge 2 ] \
    && ok "$answered/$sent answered, $upstreams upstreams" || ko "$answered/$sent answered, $upstreams upstreams"

  echo " Scenario: The catalog answers with its own data only"
  step "ContextBoundariesArchTest in catalog-service"
  if [ "${SKIP_MAVEN:-0}" = "1" ]; then echo "SKIP"; elif archunit_passes catalog; then ok; else ko; fi

  echo " Scenario: Identity keeps working while the catalog restarts"
  # Cuenta fija, como en US33: si ya existe el registro responde 409 y el login sirve igual, así
  # cada corrida no deja un consumidor nuevo en la base de la demo.
  local email phone password login_code
  email="us17.consumidor@ejemplo.pe"
  phone="987000017"
  password="Us17#Consumidor2026"
  curl -s -o /dev/null -X POST "${GATEWAY_URL}/api/v1/auth/register" -H 'Content-Type: application/json' \
    -d "{\"role\":\"CONSUMER\",\"fullName\":\"Prueba US17\",\"email\":\"${email}\",\"phone\":\"${phone}\",\"password\":\"${password}\"}"
  "${COMPOSE[@]}" restart catalog-service > /dev/null 2>&1 &
  local restart_pid=$!
  # El login debe ocurrir con Catalog caído de verdad: se espera a que ninguna réplica esté healthy.
  local down=0
  for _ in $(seq 1 60); do
    if ! docker inspect -f '{{.State.Health.Status}}' $(catalog_ids) 2>/dev/null | grep -qx healthy; then
      down=1
      break
    fi
    sleep 0.5
  done
  step "The login answers 200 while catalog-service restarts"
  login_code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "${GATEWAY_URL}/api/v1/auth/login" \
    -H 'Content-Type: application/json' -d "{\"email\":\"${email}\",\"password\":\"${password}\"}")
  local catalog_state
  catalog_state=$(docker inspect -f '{{.State.Health.Status}}' $(catalog_ids) | sort | uniq -c | paste -sd, - | tr -s ' ')
  if [ "$down" -ne 1 ]; then
    ko "catalog-service never left healthy; login not measured during the restart"
  elif [ "$login_code" = "200" ]; then
    ok "catalog:${catalog_state}"
  else
    ko "HTTP $login_code, catalog:${catalog_state}"
  fi
  wait "$restart_pid"

  "${COMPOSE[@]}" up -d --no-deps --scale catalog-service=1 catalog-service > /dev/null 2>&1
}

# Datos de la prueba de US33. Las cuentas son fijas para poder repetir la prueba: si ya existen,
# el registro responde 409 y el login sirve igual.
US33_TOPIC="reservation.events"
US33_OWNER_EMAIL="us33.comercio@ejemplo.pe"
US33_OWNER_PASSWORD="Us33#Comercio2026"
US33_CONSUMER_EMAIL="us33.consumidor@ejemplo.pe"
US33_CONSUMER_PASSWORD="Us33#Consumidor2026"
# Git Bash en Windows reescribe las rutas que empiezan con / al llamar a docker; se desactiva.
export MSYS_NO_PATHCONV=1
KAFKA_BIN=/opt/kafka/bin

json_field() { sed -n "s/.*\"$1\" *: *\"\{0,1\}\([^\",}]*\)\"\{0,1\}.*/\1/p" | head -1; }
# método ruta token cuerpo -> imprime el cuerpo y, en otra línea, el código HTTP
http_json() {
  curl -s -w '\n%{http_code}' -X "$1" "${GATEWAY_URL}$2" -H 'Content-Type: application/json' \
    ${3:+-H "Authorization: Bearer $3"} ${4:+-d "$4"}
}
login_token() {
  http_json POST /api/v1/auth/login "" "{\"email\":\"$1\",\"password\":\"$2\"}" | head -1 | json_field accessToken
}
# grupo tópico -> suma del atraso de todas las particiones
topic_lag() {
  "${COMPOSE[@]}" exec -T kafka ${KAFKA_BIN}/kafka-consumer-groups.sh --bootstrap-server kafka:9092 \
    --describe --group "$1" 2> /dev/null | awk -v t="$2" '$2 == t && $6 ~ /^[0-9]+$/ { s += $6 } END { print s + 0 }'
}
# tópico patrón -> 0 si algún registro lo contiene
topic_has() {
  "${COMPOSE[@]}" exec -T kafka ${KAFKA_BIN}/kafka-console-consumer.sh --bootstrap-server kafka:9092 \
    --topic "$1" --from-beginning --timeout-ms 8000 2> /dev/null | grep -Eq "$2"
}
# segundos comando... -> 0 si el comando pasa antes del plazo
wait_until() {
  local limit=$1; shift
  for _ in $(seq 1 "$limit"); do "$@" && return 0; sleep 1; done
  return 1
}
redeemed_once() {
  [ "$(psql_db engagement_db "SELECT count(*) FROM redeemed_reservations WHERE reservation_id = $1" | tr -d '[:space:]')" = "1" ]
}
lag_is_zero() { [ "$(topic_lag "$1" "$US33_TOPIC")" = "0" ]; }

# Deja un canje listo: crea (o reutiliza) comercio y consumidor, publica una oferta y reserva.
# Deja en variables globales US33_OWNER_TOKEN, US33_RESERVATION_ID y US33_CODE.
us33_prepare_reservation() {
  local start end body offer_id business_id campaign_id reservation
  if [ -z "${US33_OWNER_TOKEN:-}" ]; then
    US33_OWNER_TOKEN=$(login_token "$US33_OWNER_EMAIL" "$US33_OWNER_PASSWORD")
    if [ -z "$US33_OWNER_TOKEN" ]; then
      # Primera vez: se registra el comercio con el primer RUC del padron de muestra que siga libre.
      local ruc status
      for ruc in $(cut -d'|' -f1 ../identity-service/src/main/resources/sunat/padron-muestra.txt | grep -E '^[0-9]{11}$'); do
        status=$(http_json POST /api/v1/auth/register "" "{\"role\":\"BUSINESS_OWNER\",\"fullName\":\"Comercio US33\",\"email\":\"${US33_OWNER_EMAIL}\",\"phone\":\"987000033\",\"password\":\"${US33_OWNER_PASSWORD}\",\"businessProfile\":{\"businessName\":\"Bodega US33\",\"businessType\":\"Bodega\",\"ruc\":\"${ruc}\",\"address\":\"Jr. Huanuco 1250, La Victoria\",\"latitude\":-12.0681,\"longitude\":-77.035,\"openingHours\":\"Lun-Sab 07:00-22:00\"}}" | tail -1)
        [ "$status" = "201" ] && break
      done
    fi
    http_json POST /api/v1/auth/register "" "{\"role\":\"CONSUMER\",\"fullName\":\"Consumidor US33\",\"email\":\"${US33_CONSUMER_EMAIL}\",\"phone\":\"987000034\",\"password\":\"${US33_CONSUMER_PASSWORD}\"}" > /dev/null
    US33_OWNER_TOKEN=$(login_token "$US33_OWNER_EMAIL" "$US33_OWNER_PASSWORD")
    US33_CONSUMER_TOKEN=$(login_token "$US33_CONSUMER_EMAIL" "$US33_CONSUMER_PASSWORD")
  fi
  [ -n "$US33_OWNER_TOKEN" ] && [ -n "$US33_CONSUMER_TOKEN" ] || return 1
  start=$(date -u +%Y-%m-%d); end=$(date -u -d '+30 days' +%Y-%m-%d)
  body=$(http_json POST /api/v1/campaigns "$US33_OWNER_TOKEN" "{\"businessName\":\"Bodega US33\",\"name\":\"Prueba US33 $(date +%s)\",\"description\":\"Oferta para la prueba de eventos\",\"period\":{\"start\":\"${start}\",\"end\":\"${end}\"},\"storeLocation\":{\"address\":\"Jr. Huanuco 1250, La Victoria\",\"latitude\":-12.067,\"longitude\":-77.024},\"zone\":{\"type\":\"RADIUS\",\"center\":{\"latitude\":-12.067,\"longitude\":-77.024},\"radiusMeters\":800},\"offers\":[{\"title\":\"Almuerzo US33\",\"conditions\":\"Solo para la prueba.\",\"price\":15,\"validTo\":\"${end}\",\"category\":\"Gastronomia\"}]}")
  business_id=$(printf '%s' "$body" | json_field businessId)
  campaign_id=$(printf '%s' "$body" | json_field campaignId)
  [ -n "$campaign_id" ] && US33_CAMPAIGNS+=("$campaign_id")
  offer_id=$(psql_db catalog_db "SELECT id FROM offers WHERE business_id = ${business_id:-0} ORDER BY id DESC LIMIT 1" | tr -d '[:space:]')
  [ -n "$offer_id" ] || return 1
  reservation=$(http_json POST /api/v1/reservations "$US33_CONSUMER_TOKEN" "{\"offerId\":${offer_id}}" | head -1)
  US33_RESERVATION_ID=$(printf '%s' "$reservation" | json_field reservationId)
  US33_CODE=$(printf '%s' "$reservation" | json_field code)
  [ -n "$US33_RESERVATION_ID" ] && [ -n "$US33_CODE" ]
}
# Al terminar se pausan las campañas de la prueba: la búsqueda por cercanía solo muestra campañas
# ACTIVE, así que no quedan ofertas "Almuerzo US33" visibles en la demo.
us33_pause_campaigns() {
  local id
  for id in "${US33_CAMPAIGNS[@]}"; do
    http_json PATCH "/api/v1/campaigns/${id}/status" "$US33_OWNER_TOKEN" '{"status":"PAUSED"}' > /dev/null
  done
  US33_CAMPAIGNS=()
}
us33_redeem() {
  http_json POST /api/v1/reservations/redeem "$US33_OWNER_TOKEN" "{\"code\":\"${US33_CODE}\"}" | tail -1
}

us33() {
  echo "Feature: US33 Communicate the services asynchronously"
  US33_OWNER_TOKEN=""
  US33_CAMPAIGNS=()
  local code lag marker
  # US17 deja a Catalog reiniciando; sin Catalog sano no se puede publicar la oferta del escenario.
  step "Catalog is healthy before the scenarios"
  [ "$(wait_catalog_healthy 1)" -ge 1 ] && ok || { ko; return; }

  echo " Scenario: The redemption completes while the consumer service is down"
  step "Background: a business owner has an ACTIVE reservation code"
  if us33_prepare_reservation; then ok "reservation ${US33_RESERVATION_ID}"; else ko; us33_pause_campaigns; return; fi
  "${COMPOSE[@]}" stop engagement-service > /dev/null 2>&1
  step "engagement-service is stopped"
  [ -z "$("${COMPOSE[@]}" ps -q --status running engagement-service)" ] && ok || ko
  code=$(us33_redeem)
  step "The business owner redeems the code through the gateway"
  [ "$code" = "200" ] && ok "HTTP $code" || ko "HTTP $code"
  step "The ReservationRedeemed event is in topic ${US33_TOPIC}"
  wait_until 30 topic_has "$US33_TOPIC" "\"aggregateId\" *: *${US33_RESERVATION_ID}[,}]" && ok || ko
  step "The consumer group engagement-service has a lag of at least 1"
  lag=$(topic_lag engagement-service "$US33_TOPIC")
  [ "$lag" -ge 1 ] && ok "lag $lag" || ko "lag $lag"

  echo " Scenario: The consumer processes the pending events when it comes back"
  step "engagement-service starts again"
  "${COMPOSE[@]}" start engagement-service > /dev/null 2>&1 && ok || ko
  step "Within 60 seconds the reservation is in redeemed_reservations once"
  wait_until 60 redeemed_once "$US33_RESERVATION_ID" && ok || ko
  step "The consumer group engagement-service has a lag of 0"
  wait_until 30 lag_is_zero engagement-service && ok || ko "lag $(topic_lag engagement-service "$US33_TOPIC")"

  echo " Scenario: An event that cannot be processed is kept for review"
  marker="us33-unreadable-$(date +%s)"
  printf '%s\n' "$marker" | "${COMPOSE[@]}" exec -T kafka ${KAFKA_BIN}/kafka-console-producer.sh \
    --bootstrap-server kafka:9092 --topic "$US33_TOPIC" > /dev/null 2>&1
  step "Within 10 seconds the record is in ${US33_TOPIC}.DLT"
  wait_until 10 topic_has "${US33_TOPIC}.DLT" "$marker" && ok || ko
  step "The logs of engagement-service show event.dead-lettered with its offset"
  "${COMPOSE[@]}" logs --no-log-prefix engagement-service 2> /dev/null | grep 'event.dead-lettered' | grep -q 'offset=[0-9]' && ok || ko
  step "The next valid event is still processed"
  if us33_prepare_reservation && [ "$(us33_redeem)" = "200" ] && wait_until 60 redeemed_once "$US33_RESERVATION_ID"; then
    ok "reservation ${US33_RESERVATION_ID}"
  else
    ko
  fi
  us33_pause_campaigns
}

# Rutas de US27, "método ruta". Las públicas son las que la web pide sin sesión; las protegidas cubren
# cada bloque de rutas del gateway, también la escritura en la ruta pública de ofertas.
US27_PUBLIC_ROUTES=(
  "GET ${NEARBY_PATH}"
  "GET /api/v1/offers/categories"
  "GET /.well-known/jwks.json"
  "GET /docs/identity/v3/api-docs"
  "GET /"
)
US27_PROTECTED_ROUTES=(
  "GET /api/v1/consumer-profiles/me"
  "GET /api/v1/business-profiles/1"
  "POST /api/v1/campaigns"
  "POST /api/v1/admin/public-offers"
  "POST /api/v1/offers/1"
  "GET /api/v1/reservations"
  "GET /api/v1/reports"
  "GET /api/v1/saved-offers"
  "GET /api/v1/reviews"
  "GET /api/v1/follows"
  "GET /api/v1/notification-preferences"
  "PUT /api/v1/locations/last"
  "POST /api/v1/push-subscriptions"
  "POST /api/v1/auth/logout"
)
US27_CACHE_SECONDS=5
US27_MAX_OVERHEAD_MS=50
US27_SAMPLES=${US27_SAMPLES:-200}
US27_PROFILE_PATH="/api/v1/consumer-profiles/me"

# método ruta [token] -> código HTTP
status_of() {
  curl -s -o /dev/null -w '%{http_code}' -X "$1" "${GATEWAY_URL}$2" ${3:+-H "Authorization: Bearer $3"}
}
# url token -> p95 en milisegundos de US27_SAMPLES peticiones seguidas, medidas desde el contenedor del gateway
p95_ms() {
  "${COMPOSE[@]}" exec -T gateway sh -c "for i in \$(seq 1 ${US27_SAMPLES}); do \
      curl -s -o /dev/null -w '%{time_total}\n' -H 'Authorization: Bearer $2' '$1'; done" \
    | sort -n | awk '{ t[NR] = $1 } END { i = int(NR * 0.95); if (i < 1) i = 1; printf "%.1f", t[i] * 1000 }'
}
# Registra un consumidor con datos únicos y deja su token en US27_TOKEN.
us27_consumer_token() {
  local suffix email password
  suffix=$(date +%s%N | cut -c6-14)
  email="us27.${suffix}@ejemplo.pe"
  password="Us27#${suffix}"
  http_json POST /api/v1/auth/register "" "{\"role\":\"CONSUMER\",\"fullName\":\"Prueba US27\",\"email\":\"${email}\",\"phone\":\"9${suffix: -8}\",\"password\":\"${password}\"}" > /dev/null
  US27_TOKEN=$(login_token "$email" "$password")
  [ -n "$US27_TOKEN" ]
}

us27() {
  echo "Feature: US27 Require a valid token in the API gateway"
  local route method path code body since rejected token gateway_p95 direct_p95 overhead

  echo " Scenario: The public routes answer without a token"
  for route in "${US27_PUBLIC_ROUTES[@]}"; do
    read -r method path <<< "$route"
    step "${method} ${path%%\?*} answers without a token"
    code=$(status_of "$method" "$path")
    [ "$code" = "200" ] && ok "HTTP $code" || ko "HTTP $code"
  done
  step "POST /api/v1/auth/login reaches identity without a token"
  body=$(http_json POST /api/v1/auth/login "" '{"email":"nadie.us27@ejemplo.pe","password":"Otra#2026"}')
  [ "$(printf '%s' "$body" | head -1 | json_field code)" = "INVALID_CREDENTIALS" ] && ok || ko "$(printf '%s' "$body" | tr '\n' ' ')"
  step "POST /api/v1/email-unsubscriptions reaches notification without a token"
  code=$(http_json POST /api/v1/email-unsubscriptions "" '{"token":"enlace-de-prueba"}' | tail -1)
  [ "$code" != "401" ] && ok "HTTP $code" || ko "HTTP $code"

  echo " Scenario: Every protected route rejects a request without a token"
  since=$(date -u +%Y-%m-%dT%H:%M:%SZ)
  for route in "${US27_PROTECTED_ROUTES[@]}"; do
    read -r method path <<< "$route"
    step "${method} ${path} without a token"
    body=$(http_json "$method" "$path" "")
    code=$(printf '%s' "$body" | tail -1)
    [ "$code" = "401" ] && [ "$(printf '%s' "$body" | head -1 | json_field code)" = "UNAUTHORIZED" ] \
      && ok "HTTP $code" || ko "HTTP $code"
  done
  step "None of them reached a service (upstream=- in the gateway log)"
  rejected=$("${COMPOSE[@]}" logs --no-log-prefix --since "$since" gateway 2> /dev/null | grep -c '" 401 .* upstream=- ')
  [ "$rejected" -ge "${#US27_PROTECTED_ROUTES[@]}" ] && ok "$rejected rejected by the gateway" || ko "$rejected rejected by the gateway"

  echo " Scenario: A malformed token is rejected by the gateway"
  step "GET /api/v1/reservations with a malformed token"
  code=$(status_of GET /api/v1/reservations "esto-no-es-un-token")
  [ "$code" = "401" ] && ok "HTTP $code" || ko "HTTP $code"

  echo " Scenario: A valid token reaches the service"
  step "Background: a consumer logs in"
  if us27_consumer_token; then ok; else ko; return; fi
  token=$US27_TOKEN
  step "GET ${US27_PROFILE_PATH} with the token"
  code=$(status_of GET "$US27_PROFILE_PATH" "$token")
  [ "$code" = "200" ] && ok "HTTP $code" || ko "HTTP $code"

  echo " Scenario: The validation adds at most ${US27_MAX_OVERHEAD_MS} ms to the p95"
  step "p95 of ${US27_SAMPLES} requests through the gateway minus straight to identity"
  gateway_p95=$(p95_ms "http://localhost${US27_PROFILE_PATH}" "$token")
  direct_p95=$(p95_ms "http://identity-service:8081${US27_PROFILE_PATH}" "$token")
  overhead=$(awk -v g="$gateway_p95" -v d="$direct_p95" 'BEGIN { printf "%.1f", g - d }')
  awk -v o="$overhead" -v m="$US27_MAX_OVERHEAD_MS" 'BEGIN { exit !(o <= m) }' \
    && ok "gateway ${gateway_p95} ms, direct ${direct_p95} ms, +${overhead} ms" \
    || ko "gateway ${gateway_p95} ms, direct ${direct_p95} ms, +${overhead} ms"

  echo " Scenario: A closed session stops passing the gateway"
  step "POST /api/v1/auth/logout with the token"
  code=$(status_of POST /api/v1/auth/logout "$token")
  [ "$code" = "204" ] && ok "HTTP $code" || ko "HTTP $code"
  step "Within $((US27_CACHE_SECONDS + 1)) seconds the token is rejected"
  closed_token_rejected() { [ "$(status_of GET "$US27_PROFILE_PATH" "$token")" = "401" ]; }
  wait_until $((US27_CACHE_SECONDS + 1)) closed_token_rejected && ok || ko

  echo " Scenario: The token validation is not published"
  step "POST /api/v1/auth/introspect through the gateway"
  code=$(status_of POST /api/v1/auth/introspect "$token")
  [ "$code" = "404" ] && ok "HTTP $code" || ko "HTTP $code"
}

us25() {
  echo "Feature: US25 Give the identity service its own schema"

  echo " Scenario: The identity tables declare no foreign key to tables of other contexts"
  step "verificar-independencia.sh (identity_db only references its own tables)"
  local report identity_line
  if report=$(./scripts/verificar-independencia.sh ${COMPOSE_ARGS:--f docker-compose.yml}); then
    identity_line=$(printf '%s\n' "$report" | grep -E '^  identity_db:' | tr -s ' ')
    ok "${identity_line# }; $(printf '%s\n' "$report" | grep -E 'cruzadas:' | tr -s ' ')"
  else
    ko; printf '%s\n' "$report" | sed 's/^/    /'
  fi

  echo " Scenario: Other contexts refer to the user by identifier"
  if [ "${SKIP_MAVEN:-0}" = "1" ]; then
    echo "  (SKIP_MAVEN=1: ArchUnit no se ejecuta)"
  else
    for service in catalog reservation engagement notification; do
      step "ContextBoundariesArchTest in ${service}-service"
      if archunit_passes "$service"; then ok; else ko; fi
    done
  fi

  echo " Scenario: The rest of the system keeps working while identity restarts"
  local suffix email phone password code down=0
  suffix=$(date +%s)
  email="us25.${suffix}@ejemplo.pe"
  phone="9$(printf '%08d' $(((suffix + 25) % 100000000)))"
  password="Us25#${suffix}"
  step "Background: a consumer registered before the restart"
  code=$(http_json POST /api/v1/auth/register "" "{\"role\":\"CONSUMER\",\"fullName\":\"Prueba US25\",\"email\":\"${email}\",\"phone\":\"${phone}\",\"password\":\"${password}\"}" | tail -1)
  [ "$code" = "201" ] && ok "HTTP $code" || ko "HTTP $code"

  "${COMPOSE[@]}" restart identity-service > /dev/null 2>&1 &
  local restart_pid=$!
  # La búsqueda debe ocurrir con Identity caído de verdad: se espera a que deje de estar healthy.
  for _ in $(seq 1 60); do
    if [ "$(identity_health)" != "healthy" ]; then down=1; break; fi
    sleep 0.5
  done
  step "The nearby search answers 200 while identity-service restarts"
  code=$(curl -s -o /dev/null -w '%{http_code}' "${GATEWAY_URL}${NEARBY_PATH}")
  local identity_state
  identity_state=$(identity_health)
  if [ "$down" -ne 1 ]; then
    ko "identity-service never left healthy; search not measured during the restart"
  elif [ "$code" = "200" ]; then
    ok "identity:${identity_state:-stopped}"
  else
    ko "HTTP $code, identity:${identity_state:-stopped}"
  fi
  wait "$restart_pid"

  step "identity-service is healthy again within 120 seconds"
  wait_until 120 identity_healthy && ok || ko "$(identity_health)"

  step "The consumer registered before the restart logs in"
  code=$(http_json POST /api/v1/auth/login "" "{\"email\":\"${email}\",\"password\":\"${password}\"}" | tail -1)
  [ "$code" = "200" ] && ok "HTTP $code" || ko "HTTP $code"
}

case "${1:-all}" in
  us16) us16 ;;
  us17) us17 ;;
  us25) us25 ;;
  us27) us27 ;;
  us33) us33 ;;
  all) us16; us17; us25; us27; us33 ;;
  *) echo "Uso: $0 [us16|us17|us25|us27|us33|all]" >&2; exit 2 ;;
esac

echo "== ${passed} passed, ${failed} failed"
[ "$failed" -eq 0 ]
