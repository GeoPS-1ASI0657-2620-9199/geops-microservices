# Mensajería entre servicios

Contrato de los eventos del Sprint 3 (BRD-11, sección 7). Los JSON de `events/` son el contrato:
quien publica prueba contra ellos y quien consume los copia a `src/test/resources/events/`.

## Tópicos

| Tópico | Publica | Mensajes muertos |
|---|---|---|
| `identity.events` | Identity | `identity.events.DLT` |
| `catalog.events` | Catalog | `catalog.events.DLT` |
| `reservation.events` | Reservation | `reservation.events.DLT` |
| `engagement.events` | Engagement | `engagement.events.DLT` |

Tres particiones cada uno. La clave del mensaje es el `aggregateId` como texto.

## Sobre común

```json
{ "eventId": "uuid", "eventType": "ReportOpened", "version": 1, "occurredAt": "2026-10-28T19:10:00Z",
  "aggregateId": 5013, "correlationId": "opcional", "payload": { } }
```

Reglas de interoperabilidad (iteración 5 del informe):

1. **Lector tolerante.** El consumidor toma solo los campos que usa e ignora los que no conoce. Un campo
   opcional nuevo, como `correlationId`, no cambia `version`.
2. **Cambio incompatible = versión nueva.** Quitar, renombrar o cambiar el tipo de un campo sube `version`
   y se publica en un tópico paralelo (`reservation.events.v2`) hasta que todos migran.
3. **Versión desconocida.** Un evento con `version` mayor a la que el consumidor conoce se trata como
   ilegible: va a `<tópico>.DLT` con su `eventId`, sin reintentos.
4. **`correlationId`.** Es el `X-Correlation-ID` de la petición que originó el evento, para seguir una
   reserva hasta el aviso que genera. Lo escribe el publicador cuando la petición lo trae.
5. **Contrato probado por ambos lados.** Cada publicador tiene `<EventType>ContractTest` contra el JSON de
   `events/` y cada consumidor lo lee desde `src/test/resources/events/`.

## Agregar la bandeja de salida a un servicio

1. Copiar de Reservation `DomainEvent`, `OutboxPort`, `OutboxEventJpaEntity`, `OutboxEventJpaRepository`,
   `OutboxEventStatus`, `OutboxJpaAdapter`, `OutboxRelay`, `OutboxRelayProperties` y
   `MessagingConfiguration` (como `OutboxRelayConfiguration`), cambiando el paquete.
2. `RELAY_LOCK = "<servicio>.outbox-relay"`.
3. Migración con las columnas del relay (`V2__outbox_relay_columns.sql` de Identity), con el siguiente
   número libre.
4. `application.yml`: `spring.kafka.producer` (`acks: all`, `enable.idempotence: true`, `max.block.ms: 5000`,
   `request.timeout.ms: 5000`, `delivery.timeout.ms: 10000`) y `geops.outbox.topic: <servicio>.events`.
5. Cada evento es un `record` que implementa `DomainEvent` con `EVENT_TYPE`, `VERSION` y un `Payload`
   anidado en el orden del BRD. Se escribe con `outbox.append(...)` dentro de la transacción del caso de uso.
6. Un `<EventType>ContractTest` por evento.

## Agregar un consumidor

1. Tener `EventEnvelopeReader`, `UnreadableEventException` y `MessagingConfiguration` del servicio.
2. Copiar a `src/test/resources/events/` los JSON que consume.
3. Un `@KafkaListener` por tópico de origen que lee el sobre, elige el caso de uso por `eventType` y
   confirma. La regla va en el servicio de aplicación.
4. Prueba del servicio con el JSON, prueba del listener con Kafka en Testcontainers y una de idempotencia.

## Mensajes muertos

- Evento ilegible o de una versión desconocida: directo a `<tópico>.DLT`.
- Falla de la base o de Redis: se reintenta sin límite (1 s, duplicando, hasta 60 s).
- Cualquier otra falla: 5 intentos (1, 2, 4 y 8 s) y a `<tópico>.DLT`.
- Cada evento que pasa a mensajes muertos se registra como `<servicio>.event.dead-lettered` con su `eventId`.

Reinyectar lo que quedó en un tópico de mensajes muertos (los consumidores son idempotentes):

```bash
cd platform && ./scripts/replay-dlt.sh reservation.events
```
