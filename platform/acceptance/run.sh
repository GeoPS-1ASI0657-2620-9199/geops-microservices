#!/bin/bash
# Pasos de US16 y US17 contra la plataforma levantada con Docker Compose, en local (C2-01).
# Uso, desde platform/:
#   docker compose --profile app --profile messaging up -d --build
#   ./acceptance/run.sh [us16|us17|all]
# Variables opcionales:
#   COMPOSE_ARGS  archivos de compose (por defecto "-f docker-compose.yml")
#   GATEWAY_URL   puerta de entrada (por defecto http://localhost:8080)
#   SKIP_MAVEN=1  no corre las pruebas de ArchUnit (si ya pasaron en CI)
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
      if (cd "../${service}-service" && ./mvnw -q test -Dtest=ContextBoundariesArchTest \
            -Dsurefire.failIfNoSpecifiedTests=false > /dev/null 2>&1); then ok; else ko; fi
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
  if [ "${SKIP_MAVEN:-0}" = "1" ]; then echo "SKIP"; elif (cd ../catalog-service && ./mvnw -q test \
      -Dtest=ContextBoundariesArchTest -Dsurefire.failIfNoSpecifiedTests=false > /dev/null 2>&1); then ok; else ko; fi

  echo " Scenario: Identity keeps working while the catalog restarts"
  local suffix email phone password login_code
  suffix=$(date +%s)
  email="us17.${suffix}@ejemplo.pe"
  phone="9$(printf '%08d' $((suffix % 100000000)))"
  password="Us17#${suffix}"
  curl -s -o /dev/null -X POST "${GATEWAY_URL}/api/v1/auth/register" -H 'Content-Type: application/json' \
    -d "{\"role\":\"CONSUMER\",\"fullName\":\"Prueba US17\",\"email\":\"${email}\",\"phone\":\"${phone}\",\"password\":\"${password}\"}"
  "${COMPOSE[@]}" restart catalog-service > /dev/null 2>&1 &
  local restart_pid=$!
  sleep 2
  step "The login answers 200 while catalog-service restarts"
  login_code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "${GATEWAY_URL}/api/v1/auth/login" \
    -H 'Content-Type: application/json' -d "{\"email\":\"${email}\",\"password\":\"${password}\"}")
  local catalog_state
  catalog_state=$(docker inspect -f '{{.State.Health.Status}}' $(catalog_ids) | sort | uniq -c | paste -sd, - | tr -s ' ')
  [ "$login_code" = "200" ] && ok "catalog:${catalog_state}" || ko "HTTP $login_code, catalog:${catalog_state}"
  wait "$restart_pid"

  "${COMPOSE[@]}" up -d --no-deps --scale catalog-service=1 catalog-service > /dev/null 2>&1
}

case "${1:-all}" in
  us16) us16 ;;
  us17) us17 ;;
  all) us16; us17 ;;
  *) echo "Uso: $0 [us16|us17|all]" >&2; exit 2 ;;
esac

echo "== ${passed} passed, ${failed} failed"
[ "$failed" -eq 0 ]
