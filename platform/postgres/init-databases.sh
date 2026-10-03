#!/bin/bash
set -euo pipefail

create_database() {
  local database="$1" owner="$2" password="$3"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<SQL
CREATE USER ${owner} WITH PASSWORD '${password}';
CREATE DATABASE ${database} OWNER ${owner};
SQL
}

enable_postgis() {
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$1" -c "CREATE EXTENSION IF NOT EXISTS postgis;"
}

create_database identity_db identity_user "$IDENTITY_DB_PASSWORD"
create_database catalog_db catalog_user "$CATALOG_DB_PASSWORD"
create_database reservation_db reservation_user "$RESERVATION_DB_PASSWORD"
create_database engagement_db engagement_user "$ENGAGEMENT_DB_PASSWORD"
create_database notification_db notification_user "$NOTIFICATION_DB_PASSWORD"

enable_postgis catalog_db
enable_postgis notification_db
