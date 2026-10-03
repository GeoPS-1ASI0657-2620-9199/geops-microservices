# GeoPS · Microservicios

Backend de GeoPS, la plataforma de ofertas geolocalizadas de comercios locales en Lima.
Proyecto del curso 1ASI0657 Fundamentos de Arquitectura de Software (UPC, 2026-20), Grupo 10.

Cada carpeta `*-service/` es un contenedor del modelo C4 y un proyecto Spring Boot
independiente: se construye, se prueba y se despliega por separado. Los servicios salen de la
descomposición por subdominio del monolito
[OpenSourceDevUPC/geops-backend](https://github.com/OpenSourceDevUPC/geops-backend).

| Servicio | Bounded contexts | Puerto local | Base |
|---|---|---|---|
| `identity-service` | identity | 8081 | identity_db |
| `catalog-service` | offers, campaign | 8082 | catalog_db |
| `reservation-service` | coupons | 8083 | reservation_db |
| `engagement-service` | favorites, reviews | 8084 | engagement_db |
| `notification-service` | notifications | 8085 | notification_db |
| `platform/` | gateway nginx, PostgreSQL con PostGIS, Redis y Kafka | 8080 (gateway) | — |

## Tecnologías

Java 17, Spring Boot 3.5, Maven, PostgreSQL 16 con PostGIS, Redis 7, Apache Kafka 3.7 (KRaft),
Flyway, springdoc-openapi, JUnit 5, Mockito, Cucumber y Testcontainers. Cada servicio sigue
una arquitectura hexagonal: `domain`, `application`, `infrastructure` y `configuration`.

## Ejecución local

```bash
cd platform
cp .env.example .env
docker compose up -d
docker compose --profile app up -d --build
```

El primer `up` levanta solo la infraestructura para desarrollar un servicio con su wrapper
(`./mvnw spring-boot:run` en Git Bash o `.\mvnw.cmd spring-boot:run` en PowerShell). El segundo
levanta todo detrás del gateway en `http://localhost:8080`. Cada servicio publica su
documentación en `/swagger-ui.html`.

## Forma de trabajo

- GitFlow: `main` y `develop` protegidas; una rama `feature/GEO-<n>-<descripcion>` por
  tarjeta de Jira y pull request a `develop` con una aprobación.
- Conventional Commits con cuerpo que explica qué cambió y por qué.

## Equipo

- Gilbert Alonso Huarcaya Matias
- Jesús Iván Castillo Vidal
- Giorgio Marzouk Awad Vargas
