# identity-service

Contexto identity de GeoPS: registra consumidores y dueños de negocio con su perfil, comprueba
credenciales, emite el token firmado con RS256 y publica las claves públicas con las que el
gateway y los demás servicios lo validan.

## Endpoints

| Operación | Acceso | Descripción |
|---|---|---|
| `POST /api/v1/auth/register` | Público | Registra un consumidor o un dueño de negocio con su perfil. Responde `201` con la cuenta creada, sin token |
| `POST /api/v1/auth/login` | Público | Comprueba las credenciales y emite el token. La respuesta lleva `role` y, según el rol, `consumerId` o `businessId` |
| `GET /.well-known/jwks.json` | Público | Publica las claves públicas que validan el token |

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
