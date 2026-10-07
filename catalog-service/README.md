# catalog-service

Contextos offers y campaign de GeoPS: las campañas publicitarias de cada comercio y las ofertas
que publican. Sale de los módulos `offers` y `campaign` del monolito por descomposición por
subdominio. Cualquier persona consulta una oferta; el dueño de negocio consulta sus campañas y
las ofertas de cada una.

## Endpoints

| Operación | Acceso | Descripción |
|---|---|---|
| `GET /api/v1/offers/nearby` | Pública | Lista las ofertas vigentes a `radiusMinutes` (5 a 20) minutos a pie de `lat` y `lng`, con su distancia en metros y en minutos, ordenadas por tramos de 100 m y, dentro de cada tramo, primero los comercios sin reportes abiertos y luego los verificados. Paginada por `page` y `size` (hasta 20) |
| `GET /api/v1/offers/{offerId}` | Pública | Detalle de US04: precio, vigencia, condiciones, dirección, coordenadas, categoría, fuente, nombre y sello del comercio, y `available` |
| `GET /internal/v1/offers/{offerId}/availability` | Interna, sin token | Para reservation-service: `offerId`, `businessId`, `title`, `validTo` y `available` |
| `GET /api/v1/campaigns` | `ROLE_BUSINESS_OWNER` | Lista las campañas del comercio del token, de la más antigua a la más reciente |
| `GET /api/v1/campaigns/{campaignId}` | `ROLE_BUSINESS_OWNER` dueño de la campaña | Devuelve la campaña con su periodo, zona, estado y presupuesto |
| `GET /api/v1/campaigns/{campaignId}/offers` | `ROLE_BUSINESS_OWNER` dueño de la campaña | Lista las ofertas de la campaña |

Una oferta está disponible (`available`) mientras está `PUBLISHED` y hoy, en hora de Lima, no es
posterior a su `validTo`; es la misma regla que aplica reservation-service. Para reservar, además,
tiene que ser de un comercio afiliado: una oferta de fuente pública responde `available: false` y
sin `businessId`. Una oferta retirada (`REMOVED`) responde 404 en el detalle y `available: false`
en la disponibilidad.

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
| `RADIUS_OUT_OF_RANGE` | 400 | `nearby` con un radio fuera de 5 a 20 minutos |
| `INVALID_COORDINATES` | 400 | `nearby` con una latitud fuera de -90 a 90 o una longitud fuera de -180 a 180 |
| `MISSING_PARAMETER`, `INVALID_PARAMETER` | 400 | `nearby` sin `lat`, `lng` o `radiusMinutes`, o con un valor que no es número |
| `INVALID_PAGE` | 400 | `nearby` con `page` negativo o `size` fuera de 1 a 20 |

## Base de datos

`catalog_db` con PostGIS. El esquema solo cambia con migraciones de Flyway en
`src/main/resources/db/migration`; Hibernate se limita a validarlo (`ddl-auto: validate`).

| Versión | Archivo | Contenido |
|---|---|---|
| V1 | `V1__baseline.sql` | Tablas del diagrama de base de datos del informe, sin `offers.location` ni su índice espacial |
| V2 | `V2__add_offer_location_and_spatial_index.sql` | `offers.location` e `ix_offers_location` (US01) |
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
| US01 Modelar coordenadas e índice espacial (GEO-26) | `US01-model-offer-coordinates.feature` | 4 |
| US02 Consultar ofertas por radio (GEO-28) | `US02-query-offers-by-radius.feature` | 7 |
| US03 Buscar ofertas por ubicación (GEO-29) | `US03-search-offers-by-location.feature` | 4 |
| US04 Consultar el detalle de una oferta (GEO-23) | `US04-view-offer-detail.feature` | 4 |
| Disponibilidad para Reservation (Tabla 38) | `offer-availability.feature` | 3 |
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
«List my campaigns» llena `campaignId`. La carpeta `Internal` solo responde con `baseUrl` en el puerto
del servicio; su última petición comprueba que el gateway no publica `/internal`.

Hasta que llegue la creación de campañas (US05) no hay endpoint que cree datos. Para correr la
colección con la base vacía se carga `postman/seed-catalog.sql`, que deja dos comercios con una
campaña cada uno y dos ofertas en La Victoria: la `1` vigente, que la carpeta `Nearby` encuentra, y
la `2` vencida, para ver el detalle «no disponible». El comercio `1` es el primero que se registra en
Identity, así que su dueño ve la campaña «Almuerzos de octubre».

| Git Bash | PowerShell |
|---|---|
| `docker compose -f ../platform/docker-compose.yml exec -T postgres psql -U postgres -d catalog_db < postman/seed-catalog.sql` | `Get-Content postman\seed-catalog.sql \| docker compose -f ..\platform\docker-compose.yml exec -T postgres psql -U postgres -d catalog_db` |
