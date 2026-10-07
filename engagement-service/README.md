# engagement-service

Contexto engagement de GeoPS: el consumidor guarda ofertas para volver a ellas y comenta un
comercio después de canjear una reserva en él. El servicio sale de los módulos `favorites` y
`reviews` del monolito y no llama a otros servicios: las ofertas, los comercios y los canjes son
copias locales que desde el Sprint 3 llegan por eventos de Catalog, Identity y Reservation.

## Endpoints

| Operación | Acceso | Descripción |
|---|---|---|
| `POST /api/v1/saved-offers` | `ROLE_CONSUMER` | Recibe `{"offerId": 1}`. Responde `201` con la oferta guardada; si ya estaba guardada, `200` con la misma |
| `GET /api/v1/saved-offers` | `ROLE_CONSUMER` | Lista las ofertas guardadas del consumidor, de la más reciente a la más antigua, con `expired` |
| `DELETE /api/v1/saved-offers/{offerId}` | `ROLE_CONSUMER` | Quita la oferta de las guardadas. Responde `204` |
| `POST /api/v1/reviews` | `ROLE_CONSUMER` | Recibe `{"businessId", "rating", "text"}`. Responde `201` con el comentario marcado `verifiedRedemption` |
| `GET /api/v1/reviews?businessId=1` | Cualquier token válido | Lista los comentarios del comercio, del más reciente al más antiguo |

El consumidor es el `sub` del token. El servicio valida el token con el JWKS de Identity
(`IDENTITY_JWKS_URI`), el emisor `geops-identity` y la audiencia `geops-api`.

Los errores responden `{"code": "...", "message": "..."}`. Los ejemplos de cada código están en
`/swagger-ui.html`.

| Código | HTTP | Cuándo |
|---|---|---|
| `INVALID_REQUEST` | 400 | Falta `offerId` o `businessId`, `rating` fuera de 1 a 5 o texto vacío o de más de 2 000 caracteres |
| `UNAUTHORIZED` | 401 | Sin token, o token vencido, de otro emisor, para otra audiencia o con otra firma |
| `FORBIDDEN` | 403 | El token no tiene el rol que pide la operación |
| `REDEMPTION_REQUIRED` | 403 | El consumidor no tiene un canje en ese comercio |
| `OFFER_NOT_FOUND`, `SAVED_OFFER_NOT_FOUND` | 404 | No hay copia local de la oferta, o el consumidor no la tenía guardada |
| `OFFER_NOT_AVAILABLE` | 409 | La oferta ya venció |
| `REVIEW_ALREADY_EXISTS` | 409 | Cada canje del consumidor en ese comercio ya tiene su comentario |

## Reglas

- Una oferta guardada está vencida (`expired`) si su `validTo` es anterior a la fecha de Lima o si
  la copia está `EXPIRED` o `REMOVED` (US12, criterio 2).
- Un comentario usa el canje más antiguo del consumidor en ese comercio que todavía no tiene
  comentario; cada canje admite un comentario (US49, criterios 1 y 2).
- Las copias locales se escriben con `upsert` y los canjes con `saveIfAbsent` sobre
  `reservation_id`, que son las operaciones que usarán los consumidores de eventos del Sprint 3.

## Datos de demostración

El perfil `demo` agrega `db/demo/R__demo_data.sql`: el comercio 1 «Bodega Doña Rosa», la oferta 1
«Menú ejecutivo a mitad de precio» vigente hasta el `2026-10-31`, la oferta 2 ya vencida y el canje
de la reserva 1 del consumidor 1 en el comercio 1. La oferta 1 y el comercio 1 coinciden con los
datos de demostración de Catalog. Las pruebas no usan este perfil.

## Escenarios de aceptación

Los `.feature` están en `src/test/resources/features/` y corren con `./mvnw -B verify` contra
PostgreSQL en Testcontainers, con tokens firmados con una clave de prueba.

| Historia | Archivo | Escenarios |
|---|---|---|
| US12 Guardar ofertas | `US12-save-offers.feature` | 15 |
| US49 Comentar un comercio tras un canje verificado | `US49-review-business-after-verified-redemption.feature` | 9 |

## Ejecución local

Con la infraestructura de `platform/` levantada:

| Git Bash | PowerShell |
|---|---|
| `./mvnw spring-boot:run -Dspring-boot.run.profiles=demo` | `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"` |

El servicio escucha en `http://localhost:8084`. La colección
`postman/engagement.postman_collection.json` tiene un caso correcto y uno de error por endpoint;
la petición de login de Identity llena `token` con `consumerEmail` y `consumerPassword`.
