# notification-service

Contexto notification de GeoPS: el consumidor elige por qué canal y con qué máximo diario recibe
avisos, y la plataforma guarda su última ubicación conocida con su hora de captura para los avisos
por proximidad. El servicio sale del módulo `notifications` del monolito. El aviso deja de ser una
bandeja con leído y no leído y pasa a ser el registro de entregas del informe, por canal y con su
estado. Los avisos se crearán a partir de eventos desde el Sprint 3; ningún servicio llama a
Notification.

## Endpoints

| Operación | Acceso | Descripción |
|---|---|---|
| `PUT /api/v1/notification-preferences` | `ROLE_CONSUMER` | Recibe `{"pushEnabled", "emailEnabled", "dailyLimit"}`. Crea o reemplaza la preferencia y responde `200` con `updatedAt` |
| `PUT /api/v1/locations/last` | `ROLE_CONSUMER` | Recibe `{"latitude", "longitude", "accuracyMeters", "capturedAt"}`. Responde `200` con la ubicación vigente y `fresh` |

El consumidor es el `sub` del token. El servicio valida el token con el JWKS de Identity
(`IDENTITY_JWKS_URI`), el emisor `geops-identity` y la audiencia `geops-api`.

Los errores responden `{"code": "...", "message": "..."}`. Los ejemplos de cada código están en
`/swagger-ui.html`.

| Código | HTTP | Cuándo |
|---|---|---|
| `INVALID_REQUEST` | 400 | Falta un canal, `dailyLimit` fuera de 1 a 10, coordenadas fuera de rango o `accuracyMeters` no positivo |
| `INVALID_CAPTURE_TIME` | 400 | `capturedAt` está más de 2 minutos en el futuro |
| `UNAUTHORIZED` | 401 | Sin token, o token vencido, de otro emisor, para otra audiencia o con otra firma |
| `FORBIDDEN` | 403 | El token no tiene `ROLE_CONSUMER` |
| `RECIPIENT_NOT_FOUND` | 404 | Aún no llegó de Identity la copia del destinatario |

## Reglas

- `NotificationPreference.allows(channel, sentToday)` niega el aviso si el canal está apagado o
  si ya se alcanzó el máximo diario, que suma todos los canales (US48, QAS11).
- La ubicación se guarda como `geography(Point, 4326)` con índice GIST. Una lectura más antigua no
  reemplaza a la guardada, y `fresh` es verdadero mientras tenga menos de 60 minutos (CON06, US29).
- La primera ubicación crea la preferencia con los dos canales apagados y máximo diario 3, para
  que no salga ningún aviso sin consentimiento. El radio inicial es 800 m (10 minutos a 80 m por
  minuto) hasta que llegue el evento `ConsumerPreferencesChanged`.
- Las coordenadas no se escriben en los logs.

## Datos de demostración

El perfil `demo` agrega `db/demo/R__demo_data.sql` con la copia del destinatario del usuario 1
(`lucia.fernandez@ejemplo.pe`, `CONSUMER`). Las pruebas no usan este perfil.

## Escenarios de aceptación

Los `.feature` están en `src/test/resources/features/` y corren con `./mvnw -B verify` contra
PostgreSQL con PostGIS en Testcontainers, con tokens firmados con una clave de prueba.

| Historia | Archivo | Escenarios |
|---|---|---|
| US48 Configurar las preferencias de notificación | `US48-configure-notification-preferences.feature` | 12 |
| US29 Última ubicación conocida | `last-known-location.feature` | 8 |

## Ejecución local

Con la infraestructura de `platform/` levantada:

| Git Bash | PowerShell |
|---|---|
| `./mvnw spring-boot:run -Dspring-boot.run.profiles=demo` | `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"` |

El servicio escucha en `http://localhost:8085`. La colección
`postman/notification.postman_collection.json` tiene un caso correcto y uno de error por endpoint;
la petición de login de Identity llena `token` y las de ubicación ponen la hora actual antes de
enviarse.


## Sprint 3: correo y web push

`GET /api/v1/notification-preferences` lee la elecci?n actual. `POST /api/v1/email-unsubscriptions`
valida el token firmado del enlace sin exigir una sesi?n y conserva la cuenta y la preferencia general.
`POST /api/v1/push-subscriptions` registra el navegador del consumidor; repetir su token devuelve
la suscripci?n existente. `DELETE /api/v1/push-subscriptions/{subscriptionId}` solo elimina filas propias.
Los contratos, ejemplos y errores est?n en Swagger y en la colecci?n Postman del servicio.

Los consumidores aplican `UserRegistered`, `ConsumerPreferencesChanged`, `BusinessFollowed`,
`BusinessUnfollowed`, `OfferPublished`, `CampaignPaused`, `OfferExpired` y los tres eventos de reportes.
El planificador bloquea las preferencias antes de reservar un cupo diario, da precedencia al seguidor
y evita repetir una oferta entre canales. El env?o revisa de nuevo consentimiento, ubicaci?n reciente
y cuota vigente. Reducir la cuota antes de enviar descarta los avisos que excedan la nueva elecci?n.

SMTP usa `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH` y `SMTP_STARTTLS`.
El compose apunta a Mailpit en local. `NOTIFICATION_UNSUBSCRIBE_SECRET` es obligatorio y se entrega
por variable de entorno; nunca se guarda en Git. `NOTIFICATION_EMAIL_WINDOW` admite, por ejemplo,
`60m` o `2m` para una demostraci?n. `WEB_BASE_URL` define el destino del enlace de baja.

El correo se agrupa por ventanas ancladas al inicio del d?a en Lima. Solo se env?an avisos todav?a
permitidos, con correo confirmado; una oferta pausada o vencida cancela los avisos que no salieron.
Un fallo SMTP conserva el resumen para otro intento, hasta 24 horas despu?s del cierre de su ventana.
Los avisos de reportes salen de inmediato y no consumen cupos de ofertas.

FCM utiliza Firebase Admin 9.11.0. `FCM_CREDENTIALS_PATH` referencia un archivo de cuenta de servicio
fuera de Git; el compose monta `platform/keys` en `/run/keys` en modo de solo lectura. Sin ruta
configurada el canal push queda desactivado y el planificador puede elegir correo. Un token no
registrado o inv?lido se elimina; si el push falla y el usuario permite correo, se reutiliza el mismo
aviso en el resumen. Las credenciales SMTP y Firebase reales se configuran en el entorno del equipo.

Las pruebas de aceptaci?n cubren las tres historias de Notification: US29, US47 y US48, adem?s de
la ubicaci?n. `RecordingPushChannel` y `RecordingEmailSender` son dobles exclusivos de las pruebas;
los adaptadores de producci?n son `FcmWebPushChannel` y `SmtpEmailChannel`. GreenMail verifica el
transporte SMTP real y `DailyLimitConcurrencyTest` prueba diez publicaciones concurrentes y una
reducci?n posterior del m?ximo diario. Ejecutar `./mvnw verify` requiere Docker con PostgreSQL/PostGIS
y Kafka; la CI ejecuta esa suite completa.
