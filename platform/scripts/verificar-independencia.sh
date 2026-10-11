#!/bin/bash
# US16 / BRD-10: comprueba que ninguna base tiene claves foráneas hacia otra y que cada
# usuario de servicio solo entra a su base. Se corre desde platform/ con la plataforma arriba:
#   ./scripts/verificar-independencia.sh [-f docker-compose.yml -f docker-compose.prod.yml]
set -uo pipefail

cd "$(dirname "$0")/.."
SERVICES=(identity catalog reservation engagement notification)
psql_as() { docker compose "${COMPOSE_ARGS[@]}" exec -T postgres psql -X -A -t -v ON_ERROR_STOP=1 "$@"; }
COMPOSE_ARGS=("$@")

failures=0
total_fks=0
cross_fks=0

echo "== Claves foráneas por base"
for service in "${SERVICES[@]}"; do
  db="${service}_db"
  # PostgreSQL no admite claves foráneas entre bases; se comprueba además que la tabla
  # referenciada pertenezca a la propia base y que su nombre no sea el de una tabla de otra base.
  fks=$(psql_as -U postgres -d "$db" -c "
    SELECT c.conrelid::regclass || ' -> ' || c.confrelid::regclass
    FROM pg_constraint c
    JOIN pg_class t ON t.oid = c.confrelid
    JOIN pg_namespace n ON n.oid = t.relnamespace
    WHERE c.contype = 'f' AND n.nspname NOT IN ('pg_catalog', 'information_schema', 'topology', 'tiger')
    ORDER BY 1") || { echo "  $db: no se pudo consultar"; failures=$((failures + 1)); continue; }
  count=$(printf '%s' "$fks" | grep -c . || true)
  total_fks=$((total_fks + count))
  echo "  $db: $count clave(s) foránea(s)"
  [ -n "$fks" ] && printf '%s\n' "$fks" | sed 's/^/    /'

  own_tables=$(psql_as -U postgres -d "$db" -c "SELECT tablename FROM pg_tables WHERE schemaname = 'public'")
  while IFS= read -r line; do
    [ -z "$line" ] && continue
    target=${line##* -> }
    target=${target#public.}
    if ! printf '%s\n' "$own_tables" | grep -qx "$target"; then
      echo "    CRUZADA: $line"
      cross_fks=$((cross_fks + 1))
    fi
  done <<< "$fks"
done

echo "== Conexiones entre bases"
rejected=0
attempts=0
for user_service in "${SERVICES[@]}"; do
  for db_service in "${SERVICES[@]}"; do
    [ "$user_service" = "$db_service" ] && continue
    attempts=$((attempts + 1))
    # Sin contraseña: el rechazo esperado es por privilegio CONNECT; la conexión local del
    # contenedor confía en el usuario, así que lo único que la detiene es el REVOKE.
    if psql_as -U "${user_service}_user" -d "${db_service}_db" -c "SELECT 1" > /dev/null 2>&1; then
      echo "  ACEPTADA: ${user_service}_user -> ${db_service}_db"
      failures=$((failures + 1))
    else
      rejected=$((rejected + 1))
    fi
  done
done
echo "  $rejected de $attempts conexiones cruzadas rechazadas"

own_ok=0
for service in "${SERVICES[@]}"; do
  psql_as -U "${service}_user" -d "${service}_db" -c "SELECT 1" > /dev/null 2>&1 && own_ok=$((own_ok + 1))
done
echo "  $own_ok de ${#SERVICES[@]} servicios entran a su propia base"
[ "$own_ok" -eq "${#SERVICES[@]}" ] || failures=$((failures + 1))

echo "== Resumen BRD-10"
echo "  Claves foráneas: $total_fks, cruzadas: $cross_fks"
echo "  Conexiones cruzadas rechazadas: $rejected/$attempts"
if [ "$failures" -eq 0 ] && [ "$cross_fks" -eq 0 ]; then
  echo "  RESULTADO: OK"
else
  echo "  RESULTADO: FALLA"
  exit 1
fi
