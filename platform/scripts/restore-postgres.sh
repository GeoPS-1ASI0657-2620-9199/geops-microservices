#!/bin/bash
# Comprueba o restaura una copia hecha con backup-postgres.sh. Desde platform/:
#   ./scripts/restore-postgres.sh --check [carpeta]
#       restaura cada base en <base>_restore_check, compara las filas de cada tabla y la borra
#   ./scripts/restore-postgres.sh <carpeta> <base> --replace
#       reemplaza <base> con la copia; sin --replace no toca nada y solo muestra qué haría
# En la VM: COMPOSE_ARGS="-f docker-compose.yml -f docker-compose.prod.yml" BACKUP_DIR=/var/backups/geops
set -euo pipefail
export MSYS_NO_PATHCONV=1
cd "$(dirname "$0")/.."
read -r -a COMPOSE <<< "docker compose ${COMPOSE_ARGS:--f docker-compose.yml}"
BACKUP_DIR=${BACKUP_DIR:-./backups}
DATABASES=(identity_db catalog_db reservation_db engagement_db notification_db)

usage() {
  echo "uso: $0 --check [carpeta] | $0 <carpeta> <base> --replace" >&2
  exit 2
}

psql_db() {
  "${COMPOSE[@]}" exec -T postgres psql -U postgres -d "$1" -v ON_ERROR_STOP=1 -At -c "$2" | tr -d '\r'
}

restore_dump() {
  "${COMPOSE[@]}" exec -T postgres pg_restore -U postgres --exit-on-error -d "$1" < "$2"
}

require_dump() {
  if [ ! -s "$1" ]; then echo "restore.failed missing-or-empty=$1" >&2; exit 1; fi
}

count_rows() {
  local q
  q=$(psql_db "$1" "select string_agg(format('select %L, count(*) from %I', table_name, table_name), ' union all ')
                    from information_schema.tables where table_schema = 'public' and table_type = 'BASE TABLE'")
  psql_db "$1" "$q order by 1"
}

latest_folder() {
  local last
  last=$(find "$BACKUP_DIR" -mindepth 1 -maxdepth 1 -type d | sort | tail -n 1)
  if [ -z "$last" ]; then echo "restore.failed no-backups-in=$BACKUP_DIR" >&2; exit 1; fi
  echo "$last"
}

check_backup() {
  local folder=${1:-} db scratch failed=0
  if [ -z "$folder" ]; then folder=$(latest_folder); fi
  for db in "${DATABASES[@]}"; do
    require_dump "$folder/$db.dump"
    scratch="${db}_restore_check"
    psql_db postgres "drop database if exists $scratch with (force)" > /dev/null
    psql_db postgres "create database $scratch" > /dev/null
    if restore_dump "$scratch" "$folder/$db.dump" \
      && [ "$(count_rows "$db")" = "$(count_rows "$scratch")" ]; then
      echo "restore.check.ok db=$db"
    else
      echo "restore.check.failed db=$db" >&2
      failed=1
    fi
    psql_db postgres "drop database if exists $scratch with (force)" > /dev/null
  done
  if [ "$failed" -ne 0 ]; then exit 1; fi
  echo "restore.check.passed dir=$folder"
}

replace_database() {
  local folder=$1 db=$2 confirmed=$3 prefix owner known=0 candidate
  for candidate in "${DATABASES[@]}"; do
    if [ "$candidate" = "$db" ]; then known=1; fi
  done
  if [ "$known" -ne 1 ]; then echo "restore.failed unknown-db=$db" >&2; exit 2; fi
  require_dump "$folder/$db.dump"
  prefix=${db%_db}
  service="$prefix-service"
  owner="${prefix}_user"
  if [ "$confirmed" != "--replace" ]; then
    echo "restore.refused db=$db: reemplazaría la base con $folder/$db.dump; repetir con --replace" >&2
    exit 2
  fi
  "${COMPOSE[@]}" exec -T postgres pg_restore -U postgres --list < "$folder/$db.dump" > /dev/null
  RESTART_SERVICE=$service
  trap '"${COMPOSE[@]}" start "$RESTART_SERVICE" > /dev/null || true' EXIT
  "${COMPOSE[@]}" stop "$service" > /dev/null
  psql_db postgres "drop database if exists $db with (force)" > /dev/null
  psql_db postgres "create database $db owner $owner" > /dev/null
  psql_db postgres "revoke connect on database $db from public" > /dev/null
  psql_db postgres "grant connect on database $db to $owner" > /dev/null
  restore_dump "$db" "$folder/$db.dump"
  echo "restore.completed db=$db dir=$folder"
}

case "${1:-}" in
  --check) check_backup "${2:-}" ;;
  "" | -*) usage ;;
  *) if [ $# -lt 2 ]; then usage; fi; replace_database "$1" "$2" "${3:-}" ;;
esac
