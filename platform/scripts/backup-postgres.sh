#!/bin/bash
# Copia de las cinco bases (ajuste 7): pg_dump en formato custom por base, más los roles, en una carpeta con
# fecha. Borra las carpetas de más de KEEP_DAYS días. Uso, desde platform/: ./scripts/backup-postgres.sh
# En la VM: COMPOSE_ARGS="-f docker-compose.yml -f docker-compose.prod.yml" BACKUP_DIR=/var/backups/geops
set -euo pipefail
export MSYS_NO_PATHCONV=1
cd "$(dirname "$0")/.."
read -r -a COMPOSE <<< "docker compose ${COMPOSE_ARGS:--f docker-compose.yml}"
BACKUP_DIR=${BACKUP_DIR:-./backups}
KEEP_DAYS=${KEEP_DAYS:-7}
DATABASES=(identity_db catalog_db reservation_db engagement_db notification_db)

target="$BACKUP_DIR/$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$target"
chmod 700 "$BACKUP_DIR"
for db in "${DATABASES[@]}"; do
  "${COMPOSE[@]}" exec -T postgres pg_dump -U postgres -Fc "$db" > "$target/$db.dump"
  if [ ! -s "$target/$db.dump" ]; then echo "backup.failed db=$db" >&2; exit 1; fi
done
"${COMPOSE[@]}" exec -T postgres pg_dumpall -U postgres --globals-only > "$target/globals.sql"
if [ ! -s "$target/globals.sql" ]; then echo "backup.failed db=globals" >&2; exit 1; fi
find "$BACKUP_DIR" -mindepth 1 -maxdepth 1 -type d -mtime +"$KEEP_DAYS" -exec rm -rf {} +
echo "backup.completed dir=$target"
