# Digital Money House 💳

Billetera virtual desarrollada con arquitectura de microservicios usando Java, Spring Boot y Keycloak.

## Requisitos previos
- Java 21+
- Docker y Docker Compose
- IntelliJ IDEA (recomendado)
- Maven (para correr la suite de testing automatizado)

## Configuración inicial

### 1. Clonar el repositorio
```bash
git clone https://github.com/IsaMontoya17/Digital-Money-House.git
cd Digital-Money-House
```

### 2. Crear archivo .env
Crear un archivo `.env` en la raíz del proyecto con las siguientes variables:

```env
DB_ROOT_PASSWORD=root1234
DB_NAME=digital_money_house
DB_USERNAME=dmh_user
DB_PASSWORD=dmh1234
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin1234
KEYCLOAK_CLIENT_SECRET=misuperclave123
```

### 3. Levantar MySQL y Keycloak
```bash
docker-compose up -d
```

> Si necesitas reiniciar el entorno desde cero (borrar BD y Keycloak), usá `docker compose down -v` y luego `docker compose up -d` nuevamente.

### 4. Levantar los microservicios
Correr en este orden desde IntelliJ:
1. `eureka-server`
2. `api-gateway`
3. `users-service`
4. `auth-service`
5. `account-service`

## Puertos
| Servicio | Puerto |
|----------|--------|
| Eureka Server | 8761 |
| API Gateway | 8080 |
| Auth Service | 8081 |
| Keycloak | 8082 |
| Users Service | 8083 |
| Account Service | 8084 |
| MySQL | 3307 |

## Endpoints principales

Todas las peticiones se hacen contra el API Gateway (`http://localhost:8080`).

### Usuarios y autenticación
| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| POST | /users/register | Registro de usuario | No |
| POST | /auth/login | Login | No |
| POST | /auth/logout | Logout | Bearer Token |
| GET | /users/{id} | Consultar perfil de usuario | Bearer Token |
| PATCH | /users/{id} | Actualizar datos de usuario | Bearer Token |

### Cuentas y transacciones
| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| GET | /accounts/user/{keycloakId} | Consultar cuenta por keycloak_id | Bearer Token |
| GET | /accounts/{id} | Consultar saldo de cuenta | Bearer Token |
| PATCH | /accounts/{id} | Actualizar alias de cuenta | Bearer Token |
| GET | /accounts/{id}/transactions | Listar transacciones (más reciente primero) | Bearer Token |
| GET | /accounts/{id}/activity | Historial completo de actividad | Bearer Token |
| GET | /accounts/{id}/activity/{transactionId} | Detalle de una transacción | Bearer Token |
| POST | /accounts/{id}/transferences | Ingresar dinero desde tarjeta | Bearer Token |

### Tarjetas
| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| POST | /accounts/{id}/cards | Crear y asociar tarjeta a una cuenta | Bearer Token |
| GET | /accounts/{id}/cards | Listar tarjetas de una cuenta | Bearer Token |
| GET | /accounts/{id}/cards/{cardId} | Detalle de una tarjeta | Bearer Token |
| DELETE | /accounts/{id}/cards/{cardId} | Eliminar tarjeta de una cuenta | Bearer Token |

> **Nota:** en Sprint 1, `GET /accounts/user/{id}` recibía el id numérico de usuario. Desde Sprint 2 recibe el `keycloak_id` (UUID), extraído del claim `sub` del JWT.

## Documentación API
- Users Service: http://localhost:8083/swagger-ui/index.html
- Auth Service: http://localhost:8081/swagger-ui/index.html
- Account Service: http://localhost:8084/swagger-ui/index.html

## Testing automatizado

La suite de pruebas automatizadas vive en el módulo `testing-automation/`, separado de los microservicios. Usa **Java + RestAssured + JUnit 5**, con reportes generados vía **Allure**.

### Requisitos
El stack completo (Eureka, Gateway, Keycloak, MySQL y los 3 microservicios) debe estar levantado antes de correr los tests, ya que ejecutan llamadas HTTP reales contra la API.

### Ejecutar la suite
```bash
cd testing-automation
mvn clean verify
```

Esto corre todos los tests de la carpeta `smoke` y genera automáticamente el reporte de Allure al finalizar (gracias al plugin configurado en la fase `post-integration-test`).

### Ver el reporte manualmente
Si necesitás regenerar o abrir el reporte sin volver a correr los tests:
```bash
mvn allure:serve
```

### Estructura del módulo
```
testing-automation/
└── src/test/java/com/digitalmoneyhouse/testing/
    ├── config/      → TestConfig: URLs y credenciales de prueba
    ├── utils/        → AuthHelper (login/token), DbHelper (seed de datos vía JDBC)
    └── smoke/        → SmokeTestSuite: suite única con 15 casos en orden
```

## Casos de prueba manuales

📋 [Ver planilla de casos de prueba en Google Sheets](https://docs.google.com/spreadsheets/d/1R1UBZtOCZ5q2UiehdgDJ0vn9M6bGuqkA/edit?usp=sharing&ouid=109883533115129275755&rtpof=true&sd=true)

La planilla incluye Smoke Test + Regression Test con la siguiente convención de IDs:
- `TC-SM-XXX` → Smoke Test
- `TC-RG-XXX` → Regression Test

Cada hoja incluye precondiciones, datos de prueba, pasos, resultado esperado/obtenido y estado de ejecución.

## Sprint 1 - Completado ✅
- Registro de usuarios con generación de CVU y alias
- Login con token JWT vía Keycloak
- Logout invalidando sesión en Keycloak
- Eureka Server para service discovery
- API Gateway con enrutamiento y seguridad

## Sprint 2 - Completado ✅
- Consulta de saldo de cuenta (`GET /accounts/{id}`)
- Listado de transacciones ordenadas por fecha (`GET /accounts/{id}/transactions`)
- Consulta de perfil de usuario con CVU y alias (`GET /users/{id}`)
- Actualización de datos de usuario con sincronización a Keycloak (`PATCH /users/{id}`)
- Actualización de alias de cuenta con validación de formato (`PATCH /accounts/{id}`)
- Gestión completa de tarjetas: alta, listado, detalle y eliminación
- Migración de `GET /accounts/user/{id}` a `GET /accounts/user/{keycloakId}`
- Validación a nivel de Gateway con `KeycloakJwtConverter` y `TokenRelay`
- Suite de testing automatizado con RestAssured + Allure

## Sprint 3 - Completado ✅
- Historial completo de actividad de cuenta ordenado por fecha (`GET /accounts/{id}/activity`)
- Detalle de una transacción específica (`GET /accounts/{id}/activity/{transactionId}`)
- Ingreso de dinero desde tarjeta asociada con actualización de saldo (`POST /accounts/{id}/transferences`)
- Suite de smoke tests refactorizada con IDs dinámicos (sin hardcoding)
- Testing exploratorio documentado con sesiones, charters y hallazgos
- Planilla de casos de prueba actualizada: 15 smoke tests, 65 regression tests