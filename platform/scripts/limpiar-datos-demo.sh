#!/bin/bash
# Limpia de las bases de la demo lo que dejaron las pruebas (cuentas us17.*, us33.*, revision*,
# lucia12345@..., comercios "Restaurante Don Pepe 98372", campañas "Prueba US33 ..." y
# "Aviso Sprint 3 ...") sin tocar los datos curados (Lucía Fernández, los comercios de
# negocios-demo.pe con foto, Rosa Quispe, Pepe Ramos, Carmen Ruiz).
#
# Uso:  ./scripts/limpiar-datos-demo.sh [--dry-run] [proyecto-compose]   (por defecto: geops)
#   --dry-run  corre todo dentro de BEGIN ... ROLLBACK y solo muestra cuántas filas cambiaría.
#
# Cada servicio tiene su base y las referencias entre servicios son solo por id, así que:
#   - Las filas de prueba se eligen primero (solo lectura) y los ids viajan a cada base.
#   - Campañas y ofertas de prueba no se borran: quedan FINISHED / EXPIRED, porque reservas,
#     reseñas, reportes y notificaciones de otras bases las citan por id.
#   - Se borran las reservas y cuentas de consumidores de prueba, y los comercios de prueba que no
#     tienen campañas ni reservas; los que sí tienen quedan SUSPENDED.
#   - Las ofertas de prueba de un comercio curado (las "aviso Sprint 3" de la Cevichería) se
#     mueven a la dirección de su propia oferta curada y pierden la etiqueta de la prueba.
#   - Un nombre que solo trae un sufijo numérico se recorta si no choca con otra fila.
# Nunca usa DROP ni TRUNCATE. Es idempotente: una segunda corrida no cambia nada.
set -euo pipefail
export MSYS_NO_PATHCONV=1

DRY_RUN=0
PROJECT=geops
for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY_RUN=1 ;;
    -h|--help) sed -n '2,9p' "$0"; exit 0 ;;
    -*) echo "Opción desconocida: $arg" >&2; exit 2 ;;
    *) PROJECT=$arg ;;
  esac
done
END_TX=COMMIT
[ "$DRY_RUN" -eq 1 ] && END_TX=ROLLBACK

container_of() {
  docker ps -q --filter "label=com.docker.compose.project=${PROJECT}" \
    --filter "label=com.docker.compose.service=$1" | head -n 1
}
PG=$(container_of postgres)
REDIS=$(container_of redis)
[ -n "$PG" ] || { echo "No hay contenedor postgres en el proyecto '${PROJECT}'" >&2; exit 1; }

# Cuentas creadas por pruebas: run.sh (us17.<ts>, us33.*, us41.smoke.*), revisar_web.py
# (revision<N>), corridas con sufijo (lucia98372, pepe98372, rosa98372, lucia.fernandez607687),
# registros web de prueba (*.prueba@, ariana.<ts>@) y la cuenta del Sprint 2 pepe.demo.s2.
TEST_EMAIL_RE='^(us[0-9]+\.|revision[0-9]+@|(lucia|pepe|rosa)[0-9]{4,}@|lucia\.fernandez[0-9]+@|ariana\.[0-9]+@|pepe\.demo\.s[0-9]+@)|\.prueba@'
# Campañas de prueba de comercios curados.
TEST_CAMPAIGN_RE='(^|[[:space:]])Sprint[[:space:]]+[0-9]+|^Prueba US[0-9]+'
# Etiqueta que las pruebas del Sprint 3 pegaron al título de la oferta ("Ceviche 2x1 aviso Sprint 3").
LABEL_RE='[[:space:]]+(aviso|correo)[[:space:]]+Sprint[[:space:]]+[0-9]+'

# psql_db base [-v x=...] [-c SQL]: los ids viajan como variables de psql y se citan con :'x'.
# psql no interpola variables en -c, así que el SQL entra siempre por stdin.
psql_db() {
  local db=$1 sql="" args=()
  shift
  while [ $# -gt 0 ]; do
    if [ "$1" = "-c" ]; then sql=$2; shift 2; else args+=("$1"); shift; fi
  done
  set -- docker exec -i "$PG" psql -X -q -A -t -F'|' -v ON_ERROR_STOP=1 -U postgres -d "$db" \
    -v test_email_re="$TEST_EMAIL_RE" -v test_campaign_re="$TEST_CAMPAIGN_RE" -v label_re="$LABEL_RE" "${args[@]}"
  if [ -n "$sql" ]; then "$@" <<< "$sql"; else "$@"; fi
}
ids() { psql_db "$1" -c "SELECT coalesce(array_agg(x ORDER BY x), '{}') FROM ($2) q(x)"; }
count_of() { local s=${1#\{}; s=${s%\}}; [ -z "$s" ] && echo 0 || echo $(( $(tr -cd ',' <<< "$s" | wc -c) + 1 )); }

echo "== Proyecto ${PROJECT}$([ "$DRY_RUN" -eq 1 ] && echo ' (dry-run: nada se guarda)')"

# ---------------------------------------------------------------- selección (solo lectura)
ALL_USERS=$(ids identity_db "SELECT id FROM users")
[ "$ALL_USERS" != "{}" ] || { echo "identity_db no tiene usuarios; no se sigue" >&2; exit 1; }
TEST_CONSUMERS=$(psql_db identity_db -c "SELECT coalesce(array_agg(id ORDER BY id), '{}') FROM users
  WHERE role = 'CONSUMER' AND email ~* :'test_email_re'")
TEST_OWNERS=$(psql_db identity_db -c "SELECT coalesce(array_agg(id ORDER BY id), '{}') FROM users
  WHERE role = 'BUSINESS_OWNER' AND email ~* :'test_email_re'")
TEST_BUSINESSES=$(psql_db identity_db -v owners="$TEST_OWNERS" -c "SELECT coalesce(array_agg(id ORDER BY id), '{}')
  FROM business_profiles WHERE user_id = ANY(:'owners'::bigint[])")

TEST_CAMPAIGNS=$(psql_db catalog_db -v tb="$TEST_BUSINESSES" -c "SELECT coalesce(array_agg(id ORDER BY id), '{}')
  FROM campaigns WHERE business_id = ANY(:'tb'::bigint[]) OR name ~* :'test_campaign_re'")
TEST_OFFERS=$(psql_db catalog_db -v tk="$TEST_CAMPAIGNS" -c "SELECT coalesce(array_agg(id ORDER BY id), '{}')
  FROM offers WHERE campaign_id = ANY(:'tk'::bigint[])")
TEST_BUSINESS_OFFERS=$(psql_db catalog_db -v tb="$TEST_BUSINESSES" -c "SELECT coalesce(array_agg(id ORDER BY id), '{}')
  FROM offers WHERE business_id = ANY(:'tb'::bigint[])")
CAMPAIGN_BUSINESSES=$(ids catalog_db "SELECT DISTINCT business_id FROM campaigns")

# Reservas de consumidores de prueba o de consumidores que ya no existen, salvo las que tienen reporte.
TEST_RESERVATIONS=$(psql_db reservation_db -v tc="$TEST_CONSUMERS" -v users="$ALL_USERS" -c "
  SELECT coalesce(array_agg(r.id ORDER BY r.id), '{}') FROM reservations r
  WHERE (r.consumer_id = ANY(:'tc'::bigint[]) OR NOT r.consumer_id = ANY(:'users'::bigint[]))
    AND NOT EXISTS (SELECT 1 FROM noncompliance_reports n WHERE n.reservation_id = r.id)")
RESERVATION_BUSINESSES=$(psql_db reservation_db -v tr="$TEST_RESERVATIONS" -c "
  SELECT coalesce(array_agg(DISTINCT business_id), '{}') FROM reservations WHERE NOT id = ANY(:'tr'::bigint[])")
# Cuentas que se borran: consumidores de prueba y dueños de prueba cuyo comercio no tiene campañas ni
# reservas en otras bases (esos comercios quedan SUSPENDED con su cuenta). Nunca un ADMIN.
GONE_USERS=$(psql_db identity_db -v used="$CAMPAIGN_BUSINESSES" -v reserved="$RESERVATION_BUSINESSES" -c "
  SELECT coalesce(array_agg(u.id ORDER BY u.id), '{}') FROM users u LEFT JOIN business_profiles b ON b.user_id = u.id
  WHERE u.email ~* :'test_email_re'
    AND (u.role = 'CONSUMER' OR (u.role = 'BUSINESS_OWNER'
         AND (b.id IS NULL OR NOT (b.id = ANY(:'used'::bigint[]) OR b.id = ANY(:'reserved'::bigint[])))))")

echo "-- Selección"
echo "  consumidores de prueba:         $(count_of "$TEST_CONSUMERS")"
echo "  dueños de comercio de prueba:   $(count_of "$TEST_OWNERS")"
echo "  comercios de prueba:            $(count_of "$TEST_BUSINESSES")"
echo "  campañas de prueba:             $(count_of "$TEST_CAMPAIGNS")"
echo "  ofertas de esas campañas:       $(count_of "$TEST_OFFERS")"
echo "  reservas de prueba:             $(count_of "$TEST_RESERVATIONS")"
echo "  cuentas que se borran:          $(count_of "$GONE_USERS")"
psql_db catalog_db -v tk="$TEST_CAMPAIGNS" -c "SELECT '    campaña ' || c.id || ' «' || c.name || '» de ' ||
  coalesce(m.business_name, c.business_id::text) FROM campaigns c LEFT JOIN merchant_standings m USING (business_id)
  WHERE c.id = ANY(:'tk'::bigint[]) ORDER BY c.id"

summary() { while IFS='|' read -r label n; do if [ -n "$label" ]; then printf '  %s: %s\n' "$label" "$n"; fi; done; }

# ---------------------------------------------------------------- catalog_db
echo "-- catalog_db"
CATALOG_OUT=$(psql_db catalog_db -v tb="$TEST_BUSINESSES" -v tk="$TEST_CAMPAIGNS" <<SQL
BEGIN;
-- Sufijo numérico en un comercio curado: se recorta solo si nadie más tiene ya ese nombre.
WITH c AS (
  SELECT business_id, business_name, regexp_replace(business_name, '[[:space:]]+[0-9]{4,}\$', '') AS base
  FROM merchant_standings
  WHERE business_name ~ '[^[:space:]][[:space:]]+[0-9]{4,}\$' AND NOT business_id = ANY(:'tb'::bigint[])),
ok AS (
  SELECT c.* FROM c
  WHERE NOT EXISTS (SELECT 1 FROM merchant_standings m WHERE m.business_name = c.base AND m.business_id <> c.business_id)
    AND (SELECT count(*) FROM c c2 WHERE c2.base = c.base) = 1),
u AS (UPDATE merchant_standings m SET business_name = ok.base FROM ok WHERE m.business_id = ok.business_id RETURNING 1)
SELECT 'merchant_standings renombrados', count(*) FROM u;
SELECT 'merchant_standings sin renombrar (choca): ' || business_name, 1 FROM merchant_standings m
WHERE business_name ~ '[^[:space:]][[:space:]]+[0-9]{4,}\$' AND NOT business_id = ANY(:'tb'::bigint[])
  AND EXISTS (SELECT 1 FROM merchant_standings o
              WHERE o.business_name = regexp_replace(m.business_name, '[[:space:]]+[0-9]{4,}\$', '') AND o.business_id <> m.business_id);
WITH c AS (
  SELECT id, business_id, regexp_replace(title, '[[:space:]]+[0-9]{4,}\$', '') AS base FROM offers
  WHERE title ~ '[^[:space:]][[:space:]]+[0-9]{4,}\$' AND NOT coalesce(campaign_id, 0) = ANY(:'tk'::bigint[])),
ok AS (
  SELECT c.* FROM c
  WHERE NOT EXISTS (SELECT 1 FROM offers o WHERE o.business_id = c.business_id AND o.title = c.base AND o.id <> c.id)
    AND (SELECT count(*) FROM c c2 WHERE c2.business_id = c.business_id AND c2.base = c.base) = 1),
u AS (UPDATE offers o SET title = ok.base FROM ok WHERE o.id = ok.id RETURNING 1)
SELECT 'offers renombradas (sufijo numérico)', count(*) FROM u;

-- Ofertas de prueba de un comercio curado: a la dirección y ubicación de su oferta curada.
WITH ref AS (
  SELECT DISTINCT ON (business_id) business_id, address, location FROM offers
  WHERE NOT coalesce(campaign_id, 0) = ANY(:'tk'::bigint[]) AND location IS NOT NULL AND business_id IS NOT NULL
  ORDER BY business_id, id),
u AS (
  UPDATE offers o SET address = ref.address, location = ref.location FROM ref
  WHERE o.campaign_id = ANY(:'tk'::bigint[]) AND NOT o.business_id = ANY(:'tb'::bigint[]) AND o.business_id = ref.business_id
    AND (o.address IS DISTINCT FROM ref.address OR o.location IS NULL
         OR NOT ST_Equals(o.location::geometry, ref.location::geometry))
  RETURNING 1)
SELECT 'offers reubicadas en la dirección del comercio', count(*) FROM u;
WITH ref AS (
  SELECT DISTINCT ON (business_id) business_id, zone_center FROM campaigns
  WHERE NOT id = ANY(:'tk'::bigint[]) AND zone_center IS NOT NULL ORDER BY business_id, id),
u AS (
  UPDATE campaigns c SET zone_center = ref.zone_center FROM ref
  WHERE c.id = ANY(:'tk'::bigint[]) AND NOT c.business_id = ANY(:'tb'::bigint[]) AND c.business_id = ref.business_id
    AND (c.zone_center IS NULL OR NOT ST_Equals(c.zone_center::geometry, ref.zone_center::geometry))
  RETURNING 1)
SELECT 'campaigns reubicadas en la zona del comercio', count(*) FROM u;

-- Sin la etiqueta de la prueba; la campaña toma el título de su primera oferta.
WITH u AS (UPDATE offers SET title = regexp_replace(title, :'label_re', '', 'gi') WHERE title ~* :'label_re' RETURNING 1)
SELECT 'offers sin la etiqueta "aviso/correo Sprint N"', count(*) FROM u;
WITH first_offer AS (SELECT DISTINCT ON (campaign_id) campaign_id, title FROM offers ORDER BY campaign_id, id),
u AS (
  UPDATE campaigns c SET name = f.title FROM first_offer f
  WHERE c.id = f.campaign_id AND c.id = ANY(:'tk'::bigint[]) AND NOT c.business_id = ANY(:'tb'::bigint[])
    AND c.name ~* :'test_campaign_re'
  RETURNING 1)
SELECT 'campaigns renombradas con el título de su oferta', count(*) FROM u;

-- Fuera de la búsqueda: la cercanía solo muestra ofertas PUBLISHED de campañas ACTIVE.
WITH u AS (UPDATE campaigns SET status = 'FINISHED' WHERE id = ANY(:'tk'::bigint[]) AND status <> 'FINISHED' RETURNING 1)
SELECT 'campaigns de prueba -> FINISHED', count(*) FROM u;
WITH u AS (UPDATE offers SET status = 'EXPIRED' WHERE campaign_id = ANY(:'tk'::bigint[]) AND status = 'PUBLISHED' RETURNING 1)
SELECT 'offers de prueba -> EXPIRED', count(*) FROM u;
${END_TX};
SQL
)
summary <<< "$CATALOG_OUT"
OFFERS_CHANGED=$(awk -F'|' '$1 ~ /^offers/ { s += $2 } END { print s + 0 }' <<< "$CATALOG_OUT")

# ---------------------------------------------------------------- reservation_db
echo "-- reservation_db"
psql_db reservation_db -v tr="$TEST_RESERVATIONS" <<SQL | summary
BEGIN;
WITH d AS (DELETE FROM reservations WHERE id = ANY(:'tr'::bigint[]) RETURNING 1)
SELECT 'reservations de prueba borradas', count(*) FROM d;
WITH u AS (UPDATE reservations SET offer_title = regexp_replace(offer_title, :'label_re', '', 'gi')
           WHERE offer_title ~* :'label_re' RETURNING 1)
SELECT 'reservations.offer_title sin la etiqueta', count(*) FROM u;
${END_TX};
SQL

# ---------------------------------------------------------------- engagement_db
echo "-- engagement_db"
psql_db engagement_db -v tc="$TEST_CONSUMERS" -v users="$ALL_USERS" -v tb="$TEST_BUSINESSES" \
  -v tbo="$TEST_BUSINESS_OFFERS" -v to="$TEST_OFFERS" <<SQL | summary
BEGIN;
CREATE TEMP TABLE gone_consumers ON COMMIT DROP AS
  SELECT DISTINCT consumer_id FROM (
    SELECT consumer_id FROM reviews UNION SELECT consumer_id FROM redeemed_reservations
    UNION SELECT consumer_id FROM follows UNION SELECT consumer_id FROM saved_businesses
    UNION SELECT consumer_id FROM saved_offers) a
  WHERE consumer_id = ANY(:'tc'::bigint[]) OR NOT consumer_id = ANY(:'users'::bigint[]);
WITH d AS (DELETE FROM review_replies WHERE review_id IN (
             SELECT id FROM reviews WHERE consumer_id IN (SELECT consumer_id FROM gone_consumers)) RETURNING 1)
SELECT 'review_replies de consumidores de prueba', count(*) FROM d;
WITH d AS (DELETE FROM reviews WHERE consumer_id IN (SELECT consumer_id FROM gone_consumers) RETURNING 1)
SELECT 'reviews de consumidores de prueba', count(*) FROM d;
WITH d AS (DELETE FROM redeemed_reservations r WHERE consumer_id IN (SELECT consumer_id FROM gone_consumers)
             AND NOT EXISTS (SELECT 1 FROM reviews v WHERE v.reservation_id = r.reservation_id) RETURNING 1)
SELECT 'redeemed_reservations de consumidores de prueba', count(*) FROM d;
WITH d AS (DELETE FROM follows WHERE consumer_id IN (SELECT consumer_id FROM gone_consumers)
             OR business_id = ANY(:'tb'::bigint[]) RETURNING 1)
SELECT 'follows de prueba', count(*) FROM d;
WITH d AS (DELETE FROM saved_businesses WHERE consumer_id IN (SELECT consumer_id FROM gone_consumers)
             OR business_id = ANY(:'tb'::bigint[]) RETURNING 1)
SELECT 'saved_businesses de prueba', count(*) FROM d;
WITH d AS (DELETE FROM saved_offers WHERE consumer_id IN (SELECT consumer_id FROM gone_consumers)
             OR offer_id = ANY(:'tbo'::bigint[]) RETURNING 1)
SELECT 'saved_offers de prueba', count(*) FROM d;
WITH u AS (UPDATE offer_snapshots SET status = 'EXPIRED', updated_at = now()
           WHERE offer_id = ANY(:'to'::bigint[]) AND status = 'PUBLISHED' RETURNING 1)
SELECT 'offer_snapshots de prueba -> EXPIRED', count(*) FROM u;
WITH u AS (UPDATE offer_snapshots SET title = regexp_replace(title, :'label_re', '', 'gi')
           WHERE title ~* :'label_re' RETURNING 1)
SELECT 'offer_snapshots sin la etiqueta', count(*) FROM u;
WITH c AS (
  SELECT business_id, regexp_replace(business_name, '[[:space:]]+[0-9]{4,}\$', '') AS base FROM business_snapshots
  WHERE business_name ~ '[^[:space:]][[:space:]]+[0-9]{4,}\$' AND NOT business_id = ANY(:'tb'::bigint[])),
ok AS (
  SELECT c.* FROM c
  WHERE NOT EXISTS (SELECT 1 FROM business_snapshots b WHERE b.business_name = c.base AND b.business_id <> c.business_id)
    AND (SELECT count(*) FROM c c2 WHERE c2.base = c.base) = 1),
u AS (UPDATE business_snapshots b SET business_name = ok.base FROM ok WHERE b.business_id = ok.business_id RETURNING 1)
SELECT 'business_snapshots renombrados', count(*) FROM u;
${END_TX};
SQL

# ---------------------------------------------------------------- notification_db
echo "-- notification_db"
psql_db notification_db -v tu="$GONE_USERS" -v users="$ALL_USERS" -v tb="$TEST_BUSINESSES" \
  -v tbo="$TEST_BUSINESS_OFFERS" <<SQL | summary
BEGIN;
CREATE TEMP TABLE gone ON COMMIT DROP AS
  SELECT user_id FROM recipients WHERE user_id = ANY(:'tu'::bigint[]) OR NOT user_id = ANY(:'users'::bigint[]);
WITH d AS (DELETE FROM push_subscriptions WHERE consumer_id IN (SELECT user_id FROM gone) RETURNING 1)
SELECT 'push_subscriptions de cuentas de prueba', count(*) FROM d;
WITH d AS (DELETE FROM last_known_locations WHERE consumer_id IN (SELECT user_id FROM gone) RETURNING 1)
SELECT 'last_known_locations de cuentas de prueba', count(*) FROM d;
WITH d AS (DELETE FROM email_unsubscriptions WHERE consumer_id IN (SELECT user_id FROM gone) RETURNING 1)
SELECT 'email_unsubscriptions de cuentas de prueba', count(*) FROM d;
WITH d AS (DELETE FROM notification_preferences WHERE consumer_id IN (SELECT user_id FROM gone) RETURNING 1)
SELECT 'notification_preferences de cuentas de prueba', count(*) FROM d;
WITH d AS (DELETE FROM notifications WHERE recipient_id IN (SELECT user_id FROM gone)
             OR (notification_type = 'NEARBY_OFFER' AND related_entity_id = ANY(:'tbo'::text[])) RETURNING 1)
SELECT 'notifications de prueba', count(*) FROM d;
WITH d AS (DELETE FROM email_digests WHERE consumer_id IN (SELECT user_id FROM gone) RETURNING 1)
SELECT 'email_digests de cuentas de prueba', count(*) FROM d;
WITH d AS (DELETE FROM followed_businesses WHERE consumer_id IN (SELECT user_id FROM gone)
             OR business_id = ANY(:'tb'::bigint[]) RETURNING 1)
SELECT 'followed_businesses de prueba', count(*) FROM d;
WITH d AS (DELETE FROM recipients WHERE user_id IN (SELECT user_id FROM gone) RETURNING 1)
SELECT 'recipients de cuentas de prueba', count(*) FROM d;
WITH u AS (UPDATE notifications SET title = regexp_replace(title, :'label_re', '', 'gi'),
                                    message = regexp_replace(message, :'label_re', '', 'gi')
           WHERE title ~* :'label_re' OR message ~* :'label_re' RETURNING 1)
SELECT 'notifications sin la etiqueta', count(*) FROM u;
${END_TX};
SQL

# ---------------------------------------------------------------- identity_db (al final: los usuarios se borran después que lo que los cita)
echo "-- identity_db"
psql_db identity_db -v gone="$GONE_USERS" -v tb="$TEST_BUSINESSES" <<SQL | summary
BEGIN;
-- Un comercio de prueba con campañas o reservas en otras bases no se borra: queda suspendido.
WITH u AS (UPDATE business_profiles SET account_status = 'SUSPENDED'
           WHERE id = ANY(:'tb'::bigint[]) AND NOT user_id = ANY(:'gone'::bigint[]) AND account_status <> 'SUSPENDED'
           RETURNING 1)
SELECT 'business_profiles de prueba -> SUSPENDED', count(*) FROM u;
WITH d AS (DELETE FROM ruc_verifications WHERE business_profile_id IN (
             SELECT id FROM business_profiles WHERE user_id = ANY(:'gone'::bigint[])) RETURNING 1)
SELECT 'ruc_verifications de comercios de prueba', count(*) FROM d;
WITH d AS (DELETE FROM business_profiles WHERE user_id = ANY(:'gone'::bigint[]) RETURNING 1)
SELECT 'business_profiles de prueba borrados', count(*) FROM d;
WITH d AS (DELETE FROM consumer_profiles WHERE user_id = ANY(:'gone'::bigint[]) RETURNING 1)
SELECT 'consumer_profiles de prueba borrados', count(*) FROM d;
WITH d AS (DELETE FROM users WHERE id = ANY(:'gone'::bigint[]) AND role <> 'ADMIN' RETURNING 1)
SELECT 'users de prueba borrados', count(*) FROM d;
${END_TX};
SQL

# ---------------------------------------------------------------- caché de cercanía de catalog
# RedisCellCacheAdapter guarda cada celda en catalog:cell:v1:<geohash> y su versión en ...:ver.
# Se borran las celdas (no las versiones): la siguiente búsqueda las vuelve a leer de PostgreSQL.
echo "-- redis"
if [ -z "$REDIS" ]; then
  echo "  sin contenedor redis en el proyecto; no hay caché que invalidar"
else
  cells=$(docker exec "$REDIS" redis-cli --scan --pattern 'catalog:cell:v1:*' | grep -v ':ver$' || true)
  n_cells=$(grep -c . <<< "$cells" || true)
  if [ "$OFFERS_CHANGED" -eq 0 ]; then
    echo "  ninguna oferta cambió; la caché se deja como está (${n_cells} celdas)"
  elif [ "$DRY_RUN" -eq 1 ]; then
    echo "  se borrarían ${n_cells} celdas catalog:cell:v1:*"
  else
    [ "$n_cells" -gt 0 ] && xargs -r -n 100 docker exec "$REDIS" redis-cli DEL <<< "$cells" > /dev/null
    echo "  celdas borradas: ${n_cells}"
  fi
fi

echo "== $([ "$DRY_RUN" -eq 1 ] && echo 'dry-run terminado: se hizo ROLLBACK en todas las bases' || echo 'limpieza terminada')"
