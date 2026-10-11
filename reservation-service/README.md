# reservation-service

Contexto reservation de GeoPS: el consumidor reserva una oferta vigente sin pagar y recibe un
código de 8 caracteres con su plazo; el comercio dueño de la oferta consulta la reserva por ese
código y confirma la entrega (canje). La reserva guarda el título y el comercio de la oferta al
crearse. El canje publica `ReservationRedeemed` en Kafka a través de la bandeja de salida.

## Endpoints

| Operación | Acceso | Descripción |
|---|---|---|
| `POST /api/v1/reservations` | `ROLE_CONSUMER` | Recibe `{"offerId": 1052}`. Responde `201` con `reservationId`, `code` y `expiresAt`; si el consumidor ya tiene una reserva `ACTIVE` de esa oferta, responde `200` con la misma |
| `GET /api/v1/reservations/{id}` | `ROLE_CONSUMER` dueño de la reserva | Devuelve la reserva |
| `GET /api/v1/reservations/code/{code}` | `ROLE_BUSINESS_OWNER` con el `businessId` de la reserva | Devuelve la reserva que corresponde al código |
| `POST /api/v1/reservations/redeem` | `ROLE_BUSINESS_OWNER` con el `businessId` de la reserva | Recibe `{"code": "7KQ2M9XA"}` (se lee en mayúsculas y sin espacios). Responde `200` con `reservationId`, `code`, `offerId`, `offerTitle`, `status` = `REDEEMED` y `redeemedAt` |
| `GET /api/v1/reservations?status=ACTIVE` | `ROLE_CONSUMER` | Lista las reservas del consumidor del token; `status` es opcional |

El consumidor es el `sub` del token y el comercio, el claim `businessId`. El servicio valida el
token con el JWKS de Identity (`IDENTITY_JWKS_URI`), el emisor `geops-identity` y la audiencia
`geops-api`.

Los errores responden `{"code": "...", "message": "..."}`; `RESERVATION_ALREADY_REDEEMED` agrega
`redeemedAt` con la fecha del primer canje. Los ejemplos de cada código están en
`/swagger-ui.html`.

| Código | HTTP | Cuándo |
|---|---|---|
| `INVALID_REQUEST` | 400 | Falta `offerId` o `code`, el código no tiene el formato, el id no es un número o el estado no existe |
| `UNAUTHORIZED` | 401 | Sin token, o token vencido, de otro emisor, para otra audiencia o con otra firma |
| `FORBIDDEN` | 403 | El token no tiene el rol, o la reserva es de otro consumidor o de otro comercio |
| `OFFER_NOT_FOUND`, `RESERVATION_NOT_FOUND` | 404 | La oferta o la reserva no existen |
| `OFFER_NOT_AVAILABLE` | 409 | La oferta ya no está vigente |
| `RESERVATION_ALREADY_REDEEMED` | 409 | El código ya se canjeó; el cuerpo trae `redeemedAt` |
| `RESERVATION_REPORTED` | 409 | El consumidor reportó la reserva |
| `RESERVATION_EXPIRED` | 410 | Pasó `expiresAt` |
| `CATALOG_UNAVAILABLE` | 503 | Catalog no respondió dentro de 2 s |

## Plazo y código

La reserva vence al terminar el último día de vigencia de la oferta, hora de Lima: una oferta
vigente hasta el `2026-10-14` produce `expiresAt` = `2026-10-15T04:59:59Z`. El código usa
`ABCDEFGHJKMNPQRSTUVWXYZ23456789`, sin caracteres que se confunden.

## Canje

El canje es una sola sentencia que pasa la reserva a `REDEEMED` solo si sigue `ACTIVE`, es del
comercio del token y `expires_at` es posterior al instante del canje. Dos confirmaciones
simultáneas del mismo código producen un canje y un `409`. Si la sentencia no cambia ninguna
fila, el servicio lee la reserva para decir el motivo en este orden: no existe (404), es de otro
comercio (403), ya canjeada (409), reportada (409), vencida (410). Cada rechazo queda en el log
como `reservation.redeem.rejected reason=<motivo> businessId=<id> codeSuffix=<3 caracteres>`.

## Eventos

En la misma transacción del canje se escribe `ReservationRedeemed` en `outbox_events`, con el
sobre `{eventId, eventType, version, occurredAt, aggregateId, payload}`. Un proceso del servicio
lee cada segundo hasta 100 filas sin `published_at` (`FOR UPDATE SKIP LOCKED`), las envía al
tópico `reservation.events` con la clave `aggregateId` y marca `published_at` cuando Kafka
confirma. La entrega es al menos una vez: el consumidor descarta un `eventId` repetido. Si Kafka
no responde, el canje contesta igual y la fila espera a la siguiente vuelta.

| Variable | Valor por defecto | Uso |
|---|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:29092` | Kafka; en el compose, `kafka:9092` |
| `OUTBOX_RELAY_ENABLED` | `true` | Con `false` no se publica y los eventos esperan en la bandeja |
| `OUTBOX_RELAY_DELAY_MS` | `1000` | Pausa entre vueltas del relay |

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
PostgreSQL y Kafka en Testcontainers, con un Catalog simulado y tokens firmados con una clave de
prueba. `RedeemReservationConcurrencyTest` lanza diez canjes simultáneos del mismo código: uno
canjea y nueve reciben `ReservationAlreadyRedeemedException`.

| Historia | Archivo | Escenarios |
|---|---|---|
| US40 Reservar una oferta sin pagar | `US40-reserve-offer-without-payment.feature` | 15 |
| Consultas de reservas | `query-reservations.feature` | 8 |
| US41 Validar el código de reserva en el local | `US41-validate-reservation-code.feature` | 7 |
| US41 Publicación de la bandeja de salida | `outbox-relay.feature` | 2 |

## Ejecución local

Con la infraestructura de `platform/` levantada:

| Git Bash | PowerShell |
|---|---|
| `./mvnw spring-boot:run` | `.\mvnw.cmd spring-boot:run` |

El servicio escucha en `http://localhost:8083`. La colección
`postman/reservation.postman_collection.json` tiene un caso correcto y uno de error por endpoint,
y en la carpeta US41 un canje correcto y un caso por cada rechazo. Las peticiones de login de
Identity llenan `token`, `businessToken` y `otherBusinessToken`; los casos de reserva reportada y
vencida usan `reportedCode` y `expiredCode`.

Para ver el evento en Kafka con el compose levantado (perfiles `app` y `messaging`):

```bash
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh   --bootstrap-server localhost:9092 --topic reservation.events --from-beginning --max-messages 1
```
