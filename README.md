# Microservicio Usuarios y Autenticación

## Descripción

Microservicio de gestión de identidades, autenticación JWT, roles y recuperación de contraseña para Perfulandia SPA.

- Historias de usuario: HU-01 a HU-09, HU-44 y HU-45.
- Swagger/OpenAPI disponible en: http://localhost:8081/swagger-ui.html

## Estudiante

Antonio Jara

## Tecnologías

- Java 25, Spring Boot 4.1.0, Spring Security, JWT (jjwt 0.12.5), JPA/Hibernate
- MariaDB 11.8 (compatible con MySQL 8.x para Duoc/XAMPP)
- Maven, Flyway (scripts en `db/migration/V1__perfulandia_usuarios.sql` — desactivado temporalmente porque MariaDB 11.8 no es compatible con Flyway 12.x; en Duoc con MySQL 8.x funciona activando `enabled: true` + `ddl-auto: validate`), Swagger/OpenAPI

## Usuarios de prueba (poblado de la BD con data.sql)

Al iniciar la aplicación se insertan automáticamente 5 usuarios, uno por cada rol. Contraseña de todos: **Pass1234** (para facilitar las pruebas).

| Nombre     | Email                     | Rol       |
|------------|---------------------------|-----------|
| Admin      | admin@perfulandia.cl      | ADMIN     |
| Gerente    | gerente@perfulandia.cl    | GERENTE   |
| Empleado   | empleado@perfulandia.cl   | EMPLEADO  |
| Logística  | logistica@perfulandia.cl  | LOGISTICA |
| Cliente    | cliente@gmail.com         | CLIENTE   |

## Endpoints

### Públicos (sin autenticación)

| Método | Ruta                    | HU    | Descripción                              |
|--------|-------------------------|-------|------------------------------------------|
| POST   | `/api/auth/registro`    | HU-01 | Registrar cliente                        |
| POST   | `/api/auth/login`       | HU-02 | Iniciar sesión → retorna JWT + rol       |
| POST   | `/api/auth/recuperar`   | HU-44a| Solicitar token de recuperación          |
| POST   | `/api/auth/restablecer` | HU-44b| Restablecer contraseña con token         |

### Autenticados (requieren token Bearer)

| Método | Ruta                    | HU    | Descripción                   |
|--------|-------------------------|-------|-------------------------------|
| GET    | `/api/usuarios/perfil`  | HU-03 | Ver perfil propio             |
| PUT    | `/api/usuarios/perfil`  | HU-04 | Actualizar nombre, dirección y método de pago |
| POST   | `/api/auth/logout`      | HU-45 | Cerrar sesión (invalida token) |

### Administración (requieren token Bearer con rol ADMIN)

| Método | Ruta                    | HU    | Descripción                        |
|--------|-------------------------|-------|------------------------------------|
| GET    | `/api/usuarios`         | HU-06 | Listar usuarios (paginado, filtros por `?rol=` y `?estado=`) |
| POST   | `/api/usuarios`         | HU-05 | Crear empleado (contraseña temporal automática) |
| PUT    | `/api/usuarios/{id}`    | HU-07 | Actualizar usuario (nombre, email, rol) |
| DELETE | `/api/usuarios/{id}`    | HU-08 | Desactivar usuario (borrado lógico) |

## Ejecución

```bash
./mvnw spring-boot:run
```

El servidor corre en **http://localhost:8081**.

## Pruebas automatizadas

### Tests unitarios (JUnit)

```bash
./mvnw test
```

### Tests de integración HTTP (todos los endpoints)

```bash
./http/run_tests.sh           # Ejecuta 18 requests, verifica códigos HTTP
./http/run_tests.sh --verbose # Muestra cuerpo de cada respuesta
```

Los 18 requests están documentados en [http/ms-usuarios.http](http/ms-usuarios.http), usables también manualmente desde VS Code con la extensión REST Client.

## Estructura de requests y respuestas

### POST /api/auth/registro

```json
// Request
{
  "nombre": "Andrea Vega",
  "email": "andrea.vega@gmail.com",
  "password": "AndreaV123",
  "direccion": "Av. Las Condes 456, Santiago"
}

// Response: 201 Created
{
  "id": 1,
  "nombre": "Andrea Vega",
  "email": "andrea.vega@gmail.com",
  "rol": "CLIENTE",
  "estado": "ACTIVO",
  "direccion": "Av. Las Condes 456, Santiago",
  "metodoPagoOfuscado": "****"
}
```

**Validaciones:**
- Email único (409 Conflict si duplicado)
- Password: mínimo 8 caracteres, debe incluir mayúscula, minúscula y número (400 Bad Request si no cumple)
- Todos los campos obligatorios: nombre, email, password, dirección

### POST /api/auth/login

```json
// Request
{
  "email": "admin@perfulandia.cl",
  "password": "Pass1234"
}

// Response: 200 OK
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "rol": "ADMIN"
}
```

**Reglas de negocio:**
- 3 intentos fallidos consecutivos → cuenta bloqueada (estado INACTIVO)
- Login exitoso resetea el contador de intentos fallidos
- Credenciales incorrectas → 401 Unauthorized

### GET /api/usuarios/perfil

```
Header: Authorization: Bearer <token>
Response: 200 OK → PerfilResponseDTO (misma estructura que registro)
```

### PUT /api/usuarios/perfil

```json
// Request
{
  "nombre": "Nuevo Nombre",
  "direccion": "Nueva Dirección 123",
  "nuevoMetodoPago": "1111222233334444"
}

// Response: 200 OK → PerfilResponseDTO
// El método de pago se ofusca: "**** **** **** 4444"
```

### POST /api/auth/recuperar

```json
// Request
{ "correo": "andrea.vega@gmail.com" }

// Response: 200 OK — devuelve el token UUID en texto plano
"550e8400-e29b-41d4-a716-446655440000"
```

### POST /api/auth/restablecer

```json
// Request
{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "nuevaContrasena": "NuevaClave1"
}

// Response: 200 OK
```

**Validaciones:**
- Token inválido → 404 Not Found
- Token ya usado → 400 Bad Request
- Token expirado (15 min) → 400 Bad Request
- Nueva contraseña debe cumplir con política de contraseñas

### POST /api/usuarios (ADMIN) — Crear empleado

```json
// Request
{
  "nombre": "Pedro Lopez",
  "email": "pedro.lopez@perfulandia.cl",
  "rol": "EMPLEADO"
}

// Response: 201 Created → PerfilResponseDTO
// El backend genera una contraseña temporal automáticamente
```

Rol puede ser: `ADMIN`, `CLIENTE`, `EMPLEADO`, `GERENTE`, `LOGISTICA`

### GET /api/usuarios (ADMIN) — Listar usuarios

```
GET /api/usuarios?page=0&size=10&rol=EMPLEADO&estado=ACTIVO
Header: Authorization: Bearer <token_admin>

Response: 200 OK → Page<PerfilResponseDTO>
```

Parámetros opcionales: `rol`, `estado`, `page` (default 0), `size` (default 20)

### PUT /api/usuarios/{id} (ADMIN) — Actualizar usuario

```json
// Request
{
  "nombre": "Pedro Modificado",
  "email": "pedro.mod@perfulandia.cl",
  "rol": "LOGISTICA"
}

// Response: 200 OK → PerfilResponseDTO
```

**Regla:** No se puede cambiar el rol de un usuario ADMIN a otro rol.

### DELETE /api/usuarios/{id} (ADMIN) — Desactivar usuario

```
Response: 200 OK (borrado lógico: cambia estado a INACTIVO)
```

**Reglas:**
- No se puede desactivar a un ADMIN
- No se puede desactivar a un usuario que ya está INACTIVO (400 Bad Request)
- El usuario desactivado no puede iniciar sesión

### POST /api/auth/logout

```
Header: Authorization: Bearer <token>
Response: 200 OK
```

El token se agrega a una blacklist y no puede usarse nuevamente.

## Configuración de base de datos

La aplicación usa MariaDB. La base de datos `perfulandia_usuarios` se crea automáticamente (`createDatabaseIfNotExist=true`). Las tablas se crean vía Hibernate (`ddl-auto=update`). Los usuarios de prueba se insertan con `data.sql` al iniciar (`spring.sql.init.mode=always`).

Credenciales por defecto en [application.yml](src/main/resources/application.yml):
- Usuario: `root`
- Contraseña: `1234`

Para Duoc/XAMPP (MySQL nativo), descomentar las líneas indicadas en el archivo.

## Swagger / OpenAPI

Documentación interactiva disponible en:
- Swagger UI: http://localhost:8081/swagger-ui.html
- API Docs (JSON): http://localhost:8081/v3/api-docs

## Actuator

- Health: http://localhost:8081/actuator/health
- Info: http://localhost:8081/actuator/info
