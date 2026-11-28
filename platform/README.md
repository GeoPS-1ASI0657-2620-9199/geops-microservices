# Plataforma

Docker Compose de GeoPS: gateway nginx, PostgreSQL con PostGIS, Redis, Kafka y los cinco
servicios. `docker-compose.yml` es el entorno local; `docker-compose.prod.yml` lo ajusta para la
VM; `docker-compose.loadtest.yml` solo se usa durante la prueba de carga en local.

## Variables

`.env` (plantilla `.env.example`, ignorado por git) no se monta en ningún contenedor. Compose lo
usa solo para interpolar, y cada servicio recibe únicamente lo suyo:

| Contenedor | Variables que recibe |
|---|---|
| `postgres` | `POSTGRES_PASSWORD` y las cinco `*_DB_PASSWORD`, que usa `init-databases.sh` para crear cada usuario |
| `identity-service` | `SPRING_DATASOURCE_USERNAME=identity_user`, `SPRING_DATASOURCE_PASSWORD` desde `IDENTITY_DB_PASSWORD`, rutas de las claves JWT (montadas en `/run/keys`), `OUTBOX_RELAY_ENABLED`, `GEOPS_SUNAT_SOURCE`, `NOMINATIM_CONTACT_EMAIL` |
| `catalog-service` | `catalog_user` y `CATALOG_DB_PASSWORD`, `IDENTITY_JWKS_URI`, `NOMINATIM_CONTACT_EMAIL` |
| `reservation-service` | `reservation_user` y `RESERVATION_DB_PASSWORD`, `IDENTITY_JWKS_URI`, `CATALOG_INTERNAL_URL`, `OUTBOX_RELAY_ENABLED` |
| `engagement-service` | `engagement_user` y `ENGAGEMENT_DB_PASSWORD`, `IDENTITY_JWKS_URI` |
| `notification-service` | `notification_user` y `NOTIFICATION_DB_PASSWORD`, `IDENTITY_JWKS_URI`, `SMTP_*`, `FCM_CREDENTIALS_PATH` |

Todos los servicios reciben además `SERVER_PORT`, `SPRING_DATASOURCE_URL`, `JAVA_TOOL_OPTIONS`,
`SERVER_TOMCAT_THREADS_MAX`, `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE`,
`KAFKA_BOOTSTRAP_SERVERS` y `REDIS_HOST`, ninguno secreto.

PostgreSQL, Redis y Kafka se publican solo en `127.0.0.1`. Si la máquina ya usa el 5432 o el
6379 (por ejemplo, un PostgreSQL o Redis dentro de WSL), `POSTGRES_HOST_PORT` y `REDIS_HOST_PORT`
cambian el puerto del host sin tocar la red interna.

La cuenta de administrador vive en `.env.admin` (plantilla `.env.admin.example`, ignorado por
git). Solo la lee `postgres/seed-admin.sh`; ningún servicio la recibe.

En la VM, `02-desplegar-en-vm.sh` genera el mismo `.env` con las mismas claves, así que el
despliegue no cambia.

## Comandos

```bash
cp .env.example .env                                     # y completar las contraseñas
docker compose --profile app --profile messaging up -d --build
./scripts/verificar-independencia.sh                     # US16: claves foráneas y conexiones cruzadas
./acceptance/run.sh all                                  # US16 y US17, con dos réplicas de Catalog
cp .env.admin.example .env.admin && ./postgres/seed-admin.sh
```

En una base ya creada (la VM), el retiro de `CONNECT` público se aplica una vez con
`postgres/revoke-public-connect.sql`. En la VM: `./scripts/swap-2g.sh` antes del primer despliegue.

## Copias

Cada base se copia con `pg_dump -Fc`, más los roles (`globals.sql`), en una carpeta con fecha UTC. Se conservan 7
días (`KEEP_DAYS`). La carpeta (`./backups`, o `BACKUP_DIR`) queda fuera de Git porque los roles llevan el hash de
sus contraseñas.

```bash
./scripts/backup-postgres.sh                       # backup.completed dir=./backups/<fecha>; falla si un dump queda vacío
./scripts/restore-postgres.sh --check              # restaura la última copia en <base>_restore_check, compara filas y la borra
./scripts/restore-postgres.sh backups/<fecha> engagement_db --replace   # reemplaza esa base
```

`--check` compara contra las bases vivas: conviene detener los servicios antes para que las filas no cambien
durante la comprobación. Sin `--replace` la restauración real no toca nada: solo avisa. Con `--replace` detiene el
servicio dueño, recrea la base con su dueño y sus permisos de conexión, restaura y vuelve a levantar el servicio.

En la VM, con `COMPOSE_ARGS="-f docker-compose.yml -f docker-compose.prod.yml"` y `BACKUP_DIR=/var/backups/geops`,
la línea de cron es:

```
0 3 * * * cd <ruta del repo>/platform && COMPOSE_ARGS="-f docker-compose.yml -f docker-compose.prod.yml" BACKUP_DIR=/var/backups/geops ./scripts/backup-postgres.sh >> /var/log/geops-backup.log 2>&1
```

## Proveedores de notificaciones

Correo y push se configuran solo con variables de `.env`; el código no cambia entre local y la VM.

| Canal | Local | VM |
|---|---|---|
| Correo | Mailpit (`SMTP_HOST` vacío); bandeja en `http://localhost:8025` | Brevo (plan gratuito): `SMTP_HOST=smtp-relay.brevo.com`, `SMTP_PORT=587`, `SMTP_AUTH=true`, `SMTP_STARTTLS=true`, `SMTP_USERNAME` y `SMTP_PASSWORD` de la clave SMTP, `NOTIFICATION_EMAIL_FROM` con un remitente verificado en Brevo |
| Push web | Apagado si `FCM_CREDENTIALS_PATH` está vacío | Firebase Cloud Messaging (plan Spark): la cuenta de servicio en `platform/keys/fcm-service-account.json` y `FCM_CREDENTIALS_PATH=/run/keys/fcm-service-account.json` |

La clave SMTP y la cuenta de servicio nunca se suben a Git: `.env` y `platform/keys/` están ignorados.
La configuración pública de Firebase y la clave VAPID van en los `environment*.ts` de la web.
