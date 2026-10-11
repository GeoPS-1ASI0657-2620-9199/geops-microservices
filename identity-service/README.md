# identity-service

Contexto identity de GeoPS: registra consumidores y dueños de negocio con su perfil, comprueba
credenciales, emite el token firmado con RS256 y publica las claves públicas con las que el
gateway y los demás servicios lo validan. También verifica el RUC del comercio contra su copia del
padrón reducido de SUNAT (US38), guarda el radio de búsqueda del consumidor (US46) y publica sus
eventos en `identity.events` mediante la bandeja de salida.

## Endpoints

| Operación | Acceso | Descripción |
|---|---|---|
| `POST /api/v1/auth/register` | Público | Registra un consumidor o un dueño de negocio con su perfil. Responde `201` con la cuenta creada, sin token |
| `POST /api/v1/auth/login` | Público | Comprueba las credenciales y emite el token. La respuesta lleva `role` y, según el rol, `consumerId` o `businessId` con `businessName` |
| `GET /.well-known/jwks.json` | Público | Publica las claves públicas que validan el token |
| `POST /api/v1/business-profiles/{id}/ruc-verifications` | `ROLE_BUSINESS_OWNER` con `businessId = {id}` | Verifica el RUC del perfil. Responde `201` con `result` (`APPROVED`, `REJECTED`, `PENDING`), `reason`, `message` con qué corregir y la dirección revisada. El cuerpo `{"address": "..."}` es opcional y reemplaza la dirección aunque el intento no se apruebe |
| `GET /api/v1/business-profiles/{id}` | `ROLE_BUSINESS_OWNER` con `businessId = {id}` | Perfil con `verificationStatus` y el último intento |
| `PUT /api/v1/consumer-profiles/me/search-radius` | `ROLE_CONSUMER` | Guarda el radio a pie, de 5 a 20 minutos |
| `GET /api/v1/consumer-profiles/me` | `ROLE_CONSUMER` | Perfil del consumidor con su radio |

Identity valida sus propios tokens en las rutas de perfiles con su clave pública, sin pedir el
JWKS por red. Registro, login, JWKS, Swagger y health siguen públicos e ignoran cualquier token que
llegue con la petición, así un token vencido en el navegador no bloquea un nuevo login.

Los errores responden `{"code": "...", "message": "..."}`. Los ejemplos de cada código están en
`/swagger-ui.html`.

| Código | HTTP | Cuándo |
|---|---|---|
| `INVALID_REQUEST` | 400 | Falta un dato o no tiene el formato esperado |
| `ROLE_NOT_ALLOWED` | 400 | Se pide crear una cuenta `ADMIN` |
| `BUSINESS_PROFILE_REQUIRED` | 400 | Una cuenta `BUSINESS_OWNER` llega sin `businessProfile` |
| `BUSINESS_PROFILE_NOT_ALLOWED` | 400 | Una cuenta `CONSUMER` llega con `businessProfile` |
| `INVALID_LOCATION` | 400 | Falta la dirección o las coordenadas, o están fuera de rango |
| `INVALID_RUC` | 400 | El RUC no tiene 11 dígitos |
| `EMAIL_ALREADY_REGISTERED`, `PHONE_ALREADY_REGISTERED`, `RUC_ALREADY_REGISTERED` | 409 | El dato ya pertenece a otra cuenta |
| `INVALID_CREDENTIALS` | 401 | Correo desconocido, contraseña errada o cuenta bloqueada |
| `UNAUTHORIZED` | 401 | Ruta de perfiles sin token o con un token inválido |
| `FORBIDDEN` | 403 | El rol no corresponde o el token es de otro negocio |
| `BUSINESS_PROFILE_NOT_FOUND`, `CONSUMER_PROFILE_NOT_FOUND` | 404 | No existe el perfil |
| `RADIUS_OUT_OF_RANGE` | 400 | El radio no está entre 5 y 20 minutos |
| `TAXPAYER_REGISTRY_EMPTY` | 503 | El padrón de SUNAT nunca se cargó |

Motivos de la verificación del RUC: `RUC_NOT_FOUND`, `TAXPAYER_NOT_ACTIVE`, `TAXPAYER_NOT_HABIDO` y
`ADDRESS_NOT_GEOCODABLE` (rechazo), `GEOCODER_UNAVAILABLE` (pendiente). Un comercio verificado
conserva el sello mientras Nominatim no responde; las verificaciones `PENDING` se reintentan cada
10 minutos.

## Padrón de SUNAT

`sunat_taxpayers` guarda solo Lima y Callao (ubigeo `1501*` y `0701*`) con todos los estados. Se
carga a las 04:00 de Lima y al arrancar si la última carga tiene más de 24 horas. La lectura es en
flujo (ISO-8859-1, separador `|`), con `COPY` a una tabla temporal y el reemplazo en una sola
transacción: si la descarga o la lectura fallan, queda el padrón anterior, se registra
`sunat.registry.load.failed` y se reintenta en una hora.

La posición de cada columna está solo en `geops.sunat.columns` de `application.yml`. El orden real
del archivo de SUNAT todavía no se confirmó con su cabecera; si cambia, se ajusta ahí.

| Variable | Valor por defecto | Uso |
|---|---|---|
| `GEOPS_SUNAT_SOURCE` | `file` | `url` descarga el padrón real; `file` lee `GEOPS_SUNAT_FILE` |
| `GEOPS_SUNAT_URL` | `http://www2.sunat.gob.pe/padron_reducido_ruc.zip` | Origen de la descarga |
| `GEOPS_SUNAT_FILE` | `classpath:sunat/padron-muestra.txt` | Padrón de muestra (12 líneas: 10 de Lima y Callao, una de Arequipa y una mal formada) |
| `GEOPS_SUNAT_SCHEDULE_ENABLED` | `true` | `false` apaga la carga diaria y la del arranque |

RUC de la muestra para la demo: `20123456789` activo y habido, `20111111111` de baja,
`20222222222` no habido, `20333333333` de Arequipa (no se carga) y `20999999999` no existe.

## Nominatim

`NominatimGeocodingAdapter` espera 2 s como máximo, limita la búsqueda a Perú, manda un User-Agent
con `NOMINATIM_CONTACT_EMAIL` y deja un segundo entre peticiones. El cortacircuitos `nominatim`
abre con cinco fallos en 10 s y queda abierto 30 s. Un punto fuera de Lima y Callao cuenta como no
encontrado. `NOMINATIM_BASE_URL` cambia el servidor (las pruebas usan WireMock).

## Eventos

La bandeja `outbox_events` se publica en `identity.events` con la misma lógica que Reservation:
bloqueo consultivo por vuelta, orden por `seq`, lotes de `OUTBOX_BATCH_SIZE` (50). Solo los
rechazos de Kafka cuentan para `OUTBOX_MAX_ATTEMPTS` (10) y llevan la fila a `FAILED`; un tiempo
de espera agotado guarda el error pero no suma intentos. `OUTBOX_RELAY_ENABLED=false` deja los
eventos en la tabla.

| Evento | Clave | Cuándo |
|---|---|---|
| `BusinessVerified` | `businessId` | La verificación aprueba el comercio o le quita el sello |
| `ConsumerPreferencesChanged` | `consumerId` | El consumidor cambia su radio |

## Contenido del token

```json
{
  "sub": "42",
  "iss": "geops-identity",
  "aud": "geops-api",
  "roles": ["ROLE_BUSINESS_OWNER"],
  "businessId": 7,
  "iat": 1791050400,
  "exp": 1791054000
}
```

`sub` es el id del usuario, que los servicios usan como `consumerId` con `ROLE_CONSUMER`.
`businessId` es el id de `business_profiles` y solo va con `ROLE_BUSINESS_OWNER`. El token vence
a la hora.

## Escenarios de aceptación

Los `.feature` están en `src/test/resources/features/` y corren con `./mvnw -B verify` contra
PostgreSQL en Testcontainers.

| Historia | Archivo | Escenarios |
|---|---|---|
| US20 Registrar usuario consumidor | `US20-register-consumer-account.feature` | 2 |
| US21 Iniciar sesión como usuario | `US21-log-in-as-user.feature` | 3 |
| US22 Registrar un negocio | `US22-register-business.feature` | 4 |
| US23 Iniciar sesión como negocio | `US23-log-in-as-business.feature` | 1 |
| US38 Validar el RUC del comercio ante SUNAT | `US38-verify-business-ruc.feature` | 12 |
| US38 Carga del padrón | `sunat-registry-load.feature` | 2 |
| US46 Ajustar el radio de búsqueda | `US46-adjust-search-radius.feature` | 3 |
| Protección de rutas | `profile-routes-security.feature` | 5 |

Nominatim y la descarga de SUNAT son un servidor WireMock; ninguna prueba llama a un proveedor real.

## Llaves de firma

Identity firma el token con un par RSA de 2048 bits que nunca se sube al repositorio
(`platform/keys/` y `*.pem` están en `.gitignore`). La llave privada va en PKCS#8 y la pública en
X.509, que son los formatos que lee `RsaKeyProvider`. Se generan una vez desde la raíz del repo:

| Git Bash | PowerShell |
|---|---|
| `mkdir -p platform/keys` | `New-Item -ItemType Directory -Force platform\keys` |
| `openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out platform/keys/jwt-private.pem` | `& "C:\Program Files\Git\usr\bin\openssl.exe" genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out platform\keys\jwt-private.pem` |
| `openssl pkey -in platform/keys/jwt-private.pem -pubout -out platform/keys/jwt-public.pem` | `& "C:\Program Files\Git\usr\bin\openssl.exe" pkey -in platform\keys\jwt-private.pem -pubout -out platform\keys\jwt-public.pem` |

Con `docker compose` el servicio las lee de `/run/keys`. Al correrlo con el wrapper, en
`platform/.env` van `JWT_PRIVATE_KEY_PATH=../platform/keys/jwt-private.pem` y
`JWT_PUBLIC_KEY_PATH=../platform/keys/jwt-public.pem`.

## Ejecución local

Con la infraestructura de `platform/` levantada:

| Git Bash | PowerShell |
|---|---|
| `./mvnw spring-boot:run` | `.\mvnw.cmd spring-boot:run` |

El servicio escucha en `http://localhost:8081`. La colección `postman/identity.postman_collection.json`
tiene una petición por caso, con `baseUrl` apuntando al gateway.
