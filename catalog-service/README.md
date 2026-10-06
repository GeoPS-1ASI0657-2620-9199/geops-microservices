# catalog-service

Contextos offers y campaign de GeoPS: las campañas publicitarias de cada comercio y las ofertas
que publican. Sale de los módulos `offers` y `campaign` del monolito por descomposición por
subdominio. Cualquier persona consulta una oferta; el dueño de negocio consulta sus campañas y
las ofertas de cada una.

## Endpoints

| Operación | Acceso | Descripción |
|---|---|---|
| `GET /api/v1/offers/{offerId}` | Pública | Devuelve la oferta con sus condiciones, precio, vigencia, dirección, fuente y estado |
| `GET /api/v1/campaigns` | `ROLE_BUSINESS_OWNER` | Lista las campañas del comercio del token, de la más antigua a la más reciente |
| `GET /api/v1/campaigns/{campaignId}` | `ROLE_BUSINESS_OWNER` dueño de la campaña | Devuelve la campaña con su periodo, zona, estado y presupuesto |
| `GET /api/v1/campaigns/{campaignId}/offers` | `ROLE_BUSINESS_OWNER` dueño de la campaña | Lista las ofertas de la campaña |

El comercio es el claim `businessId` del token. El servicio valida el token con el JWKS de Identity
(`IDENTITY_JWKS_URI`), el emisor `geops-identity` y la audiencia `geops-api`. Las rutas
`/internal/v1/**` quedan abiertas dentro de la red porque el gateway no las publica.

Los errores responden `{"code": "...", "message": "..."}`. Los ejemplos de cada código están en
`/swagger-ui.html`.

| Código | HTTP | Cuándo |
|---|---|---|
| `INVALID_REQUEST` | 400 | El id no es un número |
| `UNAUTHORIZED` | 401 | Sin token, o token vencido, de otro emisor, para otra audiencia o con otra firma |
| `FORBIDDEN` | 403 | El token no tiene el rol, o la campaña es de otro comercio |
| `OFFER_NOT_FOUND`, `CAMPAIGN_NOT_FOUND` | 404 | La oferta o la campaña no existen |

## Base de datos

`catalog_db` con PostGIS. El esquema solo cambia con migraciones de Flyway en
`src/main/resources/db/migration`; Hibernate se limita a validarlo (`ddl-auto: validate`).

| Versión | Archivo | Contenido |
|---|---|---|
| V1 | `V1__baseline.sql` | Tablas del diagrama de base de datos del informe, sin `offers.location` ni su índice espacial |
| V2 | `V2__add_offer_location_and_spatial_index.sql` | Reservada para US01 (`offers.location` e `ix_offers_location`) |
| V3 en adelante | — | Siguientes cambios de Catalog |

Reglas:

- Cada cambio de esquema es un archivo nuevo `V<n>__<descripcion_en_ingles>.sql`, con la versión siguiente a la última.
- Una migración que ya está en `develop` no se edita: Flyway compara su checksum y el servicio no arranca. Se corrige con otra nueva.
- `CREATE EXTENSION postgis` no va en las migraciones: la crea `platform/postgres/init-databases.sh` con el superusuario.
- `clean` está deshabilitado y no hay baseline automático: la base solo se borra desde `platform`.

Para revertir un cambio que ya está en `develop` se agrega otra migración que lo deshace; Flyway
Community no tiene `undo` y la historia de `flyway_schema_history` queda completa. Por ejemplo, si
`V3__add_offer_stock.sql` agregó `offers.stock`, la reversión es
`V4__revert_offer_stock.sql` con `ALTER TABLE offers DROP COLUMN stock;`, y la entidad JPA deja de
mapear esa columna en el mismo commit.

Para empezar de cero en local se borra el volumen de la plataforma, lo que borra todas las bases:
`docker compose down -v` y `docker compose up -d` en `platform`. Si aparece «Detected resolved
migration not applied» es porque la base tiene una versión posterior a otra que falta (por ejemplo,
una V3 aplicada antes de recibir la V2); se resuelve igual.

## Escenarios de aceptación

Los `.feature` están en `src/test/resources/features/` y corren con `./mvnw -B verify` contra
PostgreSQL con PostGIS en Testcontainers, con tokens firmados con una clave de prueba.

| Historia | Archivo | Escenarios |
|---|---|---|
| Consultas del catálogo (GEO-207) | `query-catalog.feature` | 11 |
| Migraciones versionadas (GEO-9, GEO-51) | `schema-migrations.feature` | 3 |

## Ejecución local

Con la infraestructura de `platform/` levantada:

| Git Bash | PowerShell |
|---|---|
| `./mvnw spring-boot:run` | `.\mvnw.cmd spring-boot:run` |

El servicio escucha en `http://localhost:8082`. Si Identity corre dentro del compose, su puerto no
se publica: se toma el JWKS del gateway con
`IDENTITY_JWKS_URI=http://localhost:8080/.well-known/jwks.json`.

La colección `postman/catalog.postman_collection.json` tiene un caso correcto y uno de error por
endpoint; las dos peticiones de login de Identity llenan `token` y `businessToken`, y
«List my campaigns» llena `campaignId`.

Hasta que llegue la creación de campañas (US05) no hay endpoint que cree datos. Para correr la
colección con la base vacía se carga `postman/seed-catalog.sql`, que deja dos comercios con una
campaña cada uno y una oferta. El comercio `1` es el primero que se registra en Identity, así que
su dueño ve la campaña «Almuerzos de octubre».

| Git Bash | PowerShell |
|---|---|
| `docker compose -f ../platform/docker-compose.yml exec -T postgres psql -U postgres -d catalog_db < postman/seed-catalog.sql` | `Get-Content postman\seed-catalog.sql \| docker compose -f ..\platform\docker-compose.yml exec -T postgres psql -U postgres -d catalog_db` |
