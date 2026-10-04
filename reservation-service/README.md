# reservation-service

Contexto reservation de GeoPS: el consumidor reserva una oferta vigente sin pagar y recibe un
código de 8 caracteres con su plazo; el comercio dueño de la oferta consulta la reserva por ese
código. La reserva guarda el título y el comercio de la oferta al crearse.

## Endpoints

| Operación | Acceso | Descripción |
|---|---|---|
| `POST /api/v1/reservations` | `ROLE_CONSUMER` | Recibe `{"offerId": 1052}`. Responde `201` con `reservationId`, `code` y `expiresAt`; si el consumidor ya tiene una reserva `ACTIVE` de esa oferta, responde `200` con la misma |
| `GET /api/v1/reservations/{id}` | `ROLE_CONSUMER` dueño de la reserva | Devuelve la reserva |
| `GET /api/v1/reservations/code/{code}` | `ROLE_BUSINESS_OWNER` con el `businessId` de la reserva | Devuelve la reserva que corresponde al código |
| `GET /api/v1/reservations?status=ACTIVE` | `ROLE_CONSUMER` | Lista las reservas del consumidor del token; `status` es opcional |

El consumidor es el `sub` del token y el comercio, el claim `businessId`. El servicio valida el
token con el JWKS de Identity (`IDENTITY_JWKS_URI`), el emisor `geops-identity` y la audiencia
`geops-api`.

Los errores responden `{"code": "...", "message": "..."}`. Los ejemplos de cada código están en
`/swagger-ui.html`.

| Código | HTTP | Cuándo |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Falta `offerId`, el id no es un número o el estado no existe |
| `UNAUTHORIZED` | 401 | Sin token, o token vencido, de otro emisor, para otra audiencia o con otra firma |
| `FORBIDDEN` | 403 | El token no tiene el rol, o la reserva es de otro consumidor o de otro comercio |
| `OFFER_NOT_FOUND`, `RESERVATION_NOT_FOUND` | 404 | La oferta o la reserva no existen |
| `OFFER_NOT_AVAILABLE` | 409 | La oferta ya no está vigente |
| `CATALOG_UNAVAILABLE` | 503 | Catalog no respondió dentro de 2 s |

## Plazo y código

La reserva vence al terminar el último día de vigencia de la oferta, hora de Lima: una oferta
vigente hasta el `2026-10-14` produce `expiresAt` = `2026-10-15T04:59:59Z`. El código usa
`ABCDEFGHJKMNPQRSTUVWXYZ23456789`, sin caracteres que se confunden.

## Consulta a Catalog

Al crear una reserva el servicio llama a
`GET {CATALOG_INTERNAL_URL}/internal/v1/offers/{id}/availability`, con 2 s para conectar y 2 s
para responder. Catalog debe contestar:

```json
{
  "offerId": 1052,
  "businessId": 301,
  "title": "Menú ejecutivo a mitad de precio",
  "validTo": "2026-10-14",
  "available": true
}
```

| Respuesta de Catalog | Resultado en Reservation |
|---|---|
| `200` con `available: true` y `validTo` igual o posterior a hoy en Lima | Crea la reserva |
| `200` con `available: false`, o `validTo` anterior a hoy en Lima | `409 OFFER_NOT_AVAILABLE` |
| `404` | `404 OFFER_NOT_FOUND` |
| Otro estado, un campo faltante, error de red o más de 2 s | `503 CATALOG_UNAVAILABLE` |

`available` es `true` cuando la oferta está `PUBLISHED` y hoy está dentro de su vigencia.
`validTo` va en formato `yyyy-MM-dd`.

## Escenarios de aceptación

Los `.feature` están en `src/test/resources/features/` y corren con `./mvnw -B verify` contra
PostgreSQL en Testcontainers, con un Catalog simulado y tokens firmados con una clave de prueba.

| Historia | Archivo | Escenarios |
|---|---|---|
| US40 Reservar una oferta sin pagar | `US40-reserve-offer-without-payment.feature` | 15 |
| Consultas de reservas | `query-reservations.feature` | 8 |

## Ejecución local

Con la infraestructura de `platform/` levantada:

| Git Bash | PowerShell |
|---|---|
| `./mvnw spring-boot:run` | `.\mvnw.cmd spring-boot:run` |

El servicio escucha en `http://localhost:8083`. La colección
`postman/reservation.postman_collection.json` tiene un caso correcto y uno de error por endpoint;
las dos peticiones de login de Identity llenan `token` y `businessToken`.
