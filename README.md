# Digital Money House 💳

Billetera virtual desarrollada con arquitectura de microservicios usando Java, Spring Boot y Keycloak.

## Requisitos previos
- Java 21+
- Docker y Docker Compose
- IntelliJ IDEA (recomendado)

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
| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| POST | /users/register | Registro de usuario | No |
| POST | /auth/login | Login | No |
| POST | /auth/logout | Logout | Bearer Token |
| GET | /accounts/user/{userId} | Consultar cuenta | Bearer Token |

## Documentación API
- Users Service: http://localhost:8083/swagger-ui/index.html
- Auth Service: http://localhost:8081/swagger-ui/index.html
- Account Service: http://localhost:8084/swagger-ui/index.html

## Sprint 1 - Completado ✅
- Registro de usuarios con generación de CVU y alias
- Login con token JWT via Keycloak
- Logout invalidando sesión en Keycloak
- Eureka Server para service discovery
- API Gateway con enrutamiento y seguridad