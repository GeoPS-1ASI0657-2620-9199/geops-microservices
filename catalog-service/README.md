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

`catalog_db` con PostGIS. `V1__baseline.sql` crea las tablas del diagrama de base de datos del
informe sin `offers.location` ni su índice espacial, que llegan en
`V2__add_offer_location_and_spatial_index.sql` con US01. Las migraciones de este servicio siguen
desde `V3`. Una migración que ya está en `develop` no se edita: se corrige con otra nueva.

## Escenarios de aceptación

Los `.feature` están en `src/test/resources/features/` y corren con `./mvnw -B verify` contra
PostgreSQL con PostGIS en Testcontainers, con tokens firmados con una clave de prueba.

| Historia | Archivo | Escenarios |
|---|---|---|
| Consultas del catálogo (GEO-207) | `query-catalog.feature` | 11 |

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
