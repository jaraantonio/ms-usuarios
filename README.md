# Microservicio Usuarios y Autenticación

## Descripción

Microservicio de gestión de identidades, autenticación, roles y recuperación de contraseña para Perfulandia SPA. Gestiona el registro de clientes, inicio de sesión con bloqueo tras 3 intentos fallidos, perfiles de usuario, administración de empleados por parte de ADMIN, y un sistema completo de permisos por rol. Se integra con ms-notificaciones para registrar notificaciones de recuperación de contraseña (HU-44).

- Historias de usuario: HU-01 a HU-09, HU-44, HU-45, HU-52, HU-58, HU-59.
- Swagger/OpenAPI disponible en: http://localhost:8081/swagger-ui.html

## Estudiantes

- Antonio Jara

## Microservicios del proyecto

| Microservicio | Puerto | BD |
|--------------|--------|----|
| **ms-usuarios** | 8081 | perfulandia_usuarios |
| **ms-notificaciones** | 8089 | perfulandia_notificaciones |
| **ms-reportes** | 8090 | perfulandia_reportes |
| **ms-atencion-cliente** | 8088 | perfulandia_atencion |
| MS_ProductoYStock | 8082 | db_productos_stock |
| MS_Proveedor | 8084 | db_proveedor |
| MS_Ventas | 8095 | db_ventas |
| ms-envios | 8091 | db_perfulandia_envios |
| ms-pagos | 8086 | db_perfulandia_pagos |
| ms-sucursales | 8087 | db_perfulandia_logistica |

> **Nota:** Todos los microservicios usan MySQL driver + XAMPP (root sin contraseña), se conectan al mismo servidor `localhost:3306` y crean su BD automáticamente al arrancar.

## Tecnologías

- Java 25, Spring Boot 4.0.6, JPA/Hibernate, WebClient (Spring WebFlux)
- MySQL 8+ / MariaDB 10.4+ (XAMPP)
- Maven, Swagger/OpenAPI (Springdoc)

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

| Método | Ruta | HU | Descripción |
|--------|------|----|-------------|
| POST | `/api/auth/registro` | HU-01 | Registrar cliente (rol CLIENTE) |
| POST | `/api/auth/login` | HU-02 | Iniciar sesión (bloqueo tras 3 intentos fallidos) |
| GET | `/api/usuarios/{id}/perfil` | HU-03 | Obtener perfil por ID de usuario |
| PUT | `/api/usuarios/{id}/perfil` | HU-04 | Actualizar nombre, dirección y método de pago |
| PUT | `/api/usuarios/{id}/password` | HU-CP | Cambiar contraseña (usuario autenticado) |
| POST | `/api/auth/logout` | HU-45 | Cerrar sesión |
| POST | `/api/auth/recuperar` | HU-44 | Solicitar recuperación de contraseña (registra notificación con token en ms-notificaciones) |
| POST | `/api/auth/restablecer` | HU-44 | Restablecer contraseña con token (válido 15 min) |
| GET | `/api/usuarios` | HU-06 | Listar usuarios con filtros `?rol=` y `?estado=` |
| POST | `/api/usuarios` | HU-05 | Crear empleado (ADMIN) — devuelve contraseña temporal |
| PUT | `/api/usuarios/{id}` | HU-07 | Actualizar usuario (ADMIN): nombre, email, rol |
| DELETE | `/api/usuarios/{id}` | HU-08 | Desactivar usuario (borrado lógico a INACTIVO) |
| PUT | `/api/usuarios/{id}/desbloquear` | HU-02 | Desbloquear cuenta bloqueada (solo ADMIN) |
| GET | `/api/usuarios/permisos` | HU-09 | Listar todos los permisos disponibles |
| GET | `/api/usuarios/roles/{rol}/permisos` | HU-09 | Obtener permisos asignados a un rol |
| PUT | `/api/usuarios/roles/{rol}/permisos` | HU-09 | Asignar permisos a un rol |

## Gateway

El API Gateway (Puerto 8000) expone las rutas de este microservicio bajo los siguientes predicados:

| Ruta del Gateway | Destino |
|-----------------|---------|
| `/api/auth/**` | `http://localhost:8081` (ms-usuarios) |
| `/api/usuarios/**` | `http://localhost:8081` (ms-usuarios) |

Todas las requests deben hacerse a `http://localhost:8000/api/auth/...` o `http://localhost:8000/api/usuarios/...`.

## Ejecución

```bash
./mvnw spring-boot:run
```

El servidor corre en **http://localhost:8081**.

## Pruebas automatizadas

### Tests unitarios (JUnit + Mockito)

```bash
./mvnw test
```

Reporte en `target/surefire-reports/`.

## Estructura de requests y respuestas

### POST /api/auth/registro

```json
// Request
{
  "nombre": "Andrea Vega",
  "email": "andrea.vega@gmail.com",
  "password": "AndreaV123",
  "direccion": "Av. Las Condes 456, Santiago",
  "telefono": "+56912345678"
}

// Response: 201 Created
{
  "id": 1,
  "nombre": "Andrea Vega",
  "email": "andrea.vega@gmail.com",
  "telefono": "+56912345678",
  "rol": "CLIENTE",
  "estado": "ACTIVO",
  "direccion": "Av. Las Condes 456, Santiago",
  "metodoPagoOfuscado": null
}
```

**Validaciones:**
- Email único (409 Conflict si duplicado)
- Password: mínimo 8 caracteres, debe incluir mayúscula, minúscula y número (400 Bad Request si no cumple)
- Todos los campos obligatorios: nombre, email, password, dirección
- Teléfono opcional, pero si se entrega debe tener formato +569XXXXXXXX (400 Bad Request si no cumple)

### POST /api/auth/login

```json
// Request
{
  "email": "admin@perfulandia.cl",
  "password": "Pass1234"
}

// Response: 200 OK
{
  "rol": "ADMIN"
}
```

**Reglas de negocio:**
- 3 intentos fallidos consecutivos → cuenta bloqueada (estado INACTIVO)
- Login exitoso resetea el contador de intentos fallidos
- Credenciales incorrectas → 401 Unauthorized

### GET /api/usuarios/{id}/perfil

```
GET /api/usuarios/1/perfil

Response: 200 OK
{
  "id": 1,
  "nombre": "Admin",
  "email": "admin@perfulandia.cl",
  "telefono": null,
  "rol": "ADMIN",
  "estado": "ACTIVO",
  "direccion": null,
  "metodoPagoOfuscado": null
}
```

### PUT /api/usuarios/{id}/perfil

```json
// Request
PUT /api/usuarios/1/perfil
{
  "nombre": "Nuevo Nombre",
  "direccion": "Nueva Dirección 123",
  "nuevoMetodoPago": "1111222233334444"
}

// Response: 200 OK → PerfilResponseDTO
// El método de pago se ofusca: "**** **** **** 4444"
```

### PUT /api/usuarios/{id}/password — Cambiar contraseña (HU-CP)

```json
// Request
PUT /api/usuarios/1/password
{
  "passwordActual": "Pass1234",
  "nuevaPassword": "NuevaClave1"
}

// Response: 200 OK
```

**Validaciones:**
- Contraseña actual incorrecta → 401 Unauthorized
- Nueva contraseña debe cumplir con la política de contraseñas (mínimo 8 caracteres, mayúscula, minúscula y número)

### POST /api/auth/recuperar (HU-44)

```json
// Request
{ "correo": "andrea.vega@gmail.com" }

// Response: 200 OK — mismo mensaje exista o no el correo (seguridad)
"Si el correo está registrado, recibirás un enlace de recuperación."
```

**Integración con notificaciones:**
- Si el correo existe, se genera un token UUID y se registra una notificación de tipo `RECUPERACION_CLAVE` en ms-notificaciones, incluyendo el token en el cuerpo de la notificación
- El token se puede visualizar consultando `GET /api/notificaciones/ultimo?destinatario=...` en ms-notificaciones
- Por seguridad, la API de usuarios nunca devuelve el token en la respuesta

### POST /api/auth/restablecer (HU-44)

```json
// Request
{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "nuevaContrasena": "NuevaClave1"
}

// Response: 200 OK
```

**Validaciones:**
- Token inválido → 400 Bad Request
- Token ya usado → 400 Bad Request
- Token expirado (15 min) → 400 Bad Request
- Nueva contraseña debe cumplir con política de contraseñas

### POST /api/usuarios — Crear empleado (HU-05)

```json
// Request
{
  "nombre": "Pedro Lopez",
  "email": "pedro.lopez@perfulandia.cl",
  "rol": "EMPLEADO",
  "idSucursalAsignada": 2
}

// Response: 201 Created → CrearEmpleadoResponseDTO
// El backend genera una contraseña temporal y la devuelve en la respuesta (solo se ve una vez)
{
  "id": 106,
  "nombre": "Pedro Lopez",
  "email": "pedro.lopez@perfulandia.cl",
  "rol": "EMPLEADO",
  "estado": "ACTIVO",
  "direccion": null,
  "metodoPagoOfuscado": null,
  "contrasenaTemporal": "a1b2c3d4-e5f6"
}
```

Rol puede ser: `ADMIN`, `CLIENTE`, `EMPLEADO`, `GERENTE`, `LOGISTICA`.

### GET /api/usuarios — Listar usuarios (HU-06)

```
GET /api/usuarios?rol=EMPLEADO&estado=ACTIVO

Response: 200 OK → List<PerfilResponseDTO>
```

Parámetros opcionales: `rol`, `estado`.

### PUT /api/usuarios/{id} — Actualizar usuario (HU-07)

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

### DELETE /api/usuarios/{id} — Desactivar usuario (HU-08)

```
Response: 200 OK (borrado lógico: cambia estado a INACTIVO)
```

**Reglas:**
- No se puede desactivar a un ADMIN
- No se puede desactivar a un usuario que ya está INACTIVO (400 Bad Request)
- El usuario desactivado no puede iniciar sesión

### PUT /api/usuarios/{id}/desbloquear — Desbloquear usuario (HU-02)

```
PUT /api/usuarios/1/desbloquear

Response: 200 OK (cambia estado a ACTIVO, resetea intentos fallidos a 0)
```

**Reglas:**
- Solo funciona si el usuario está INACTIVO (bloqueado por intentos fallidos)
- Usuario no bloqueado → 400 Bad Request
- Usuario inexistente → 404 Not Found

### GET /api/usuarios/permisos (HU-09)

```
Response: 200 OK
[
  {
    "id": 1,
    "nombre": "GESTIONAR_USUARIOS",
    "descripcion": "Crear, editar y desactivar usuarios",
    "modulo": "USUARIOS"
  },
  ...
]
```

### PUT /api/usuarios/roles/{rol}/permisos (HU-09)

```json
// Request: Asignar permisos a GERENTE
PUT /api/usuarios/roles/GERENTE/permisos
{
  "permisoIds": [2, 4, 5, 6, 11]
}

// Response: 200 OK
```

**Permisos disponibles (12):**

| ID | Permiso | Módulo |
|----|---------|--------|
| 1 | GESTIONAR_USUARIOS | USUARIOS |
| 2 | VER_REPORTES | REPORTES |
| 3 | GESTIONAR_TICKETS | ATENCION_CLIENTE |
| 4 | GESTIONAR_PRODUCTOS | PRODUCTOS |
| 5 | GESTIONAR_INVENTARIO | PRODUCTOS |
| 6 | GESTIONAR_PEDIDOS | ENVIOS |
| 7 | GESTIONAR_ENVIOS | ENVIOS |
| 8 | GESTIONAR_VENTAS | VENTAS |
| 9 | GESTIONAR_PROVEEDORES | PROVEEDORES |
| 10 | GESTIONAR_PAGOS | PAGOS |
| 11 | GESTIONAR_SUCURSALES | SUCURSALES |
| 12 | CONFIGURAR_PERMISOS | USUARIOS |

## Configuración de base de datos

La aplicación usa MySQL driver (compatible con MariaDB vía XAMPP). La base de datos `perfulandia_usuarios` se crea automáticamente al arrancar (`createDatabaseIfNotExist=true`). Las tablas se crean vía Hibernate (`ddl-auto=update`). Los usuarios de prueba se insertan con `data.sql` al iniciar (`spring.sql.init.mode=always`).

Credenciales por defecto en [application.yml](src/main/resources/application.yml):
- Usuario: `root`
- Contraseña: *(vacío — default XAMPP)*

## Swagger / OpenAPI

Documentación interactiva disponible en:
- Swagger UI: http://localhost:8081/swagger-ui.html
- API Docs (JSON): http://localhost:8081/v3/api-docs

## Actuator

- Health: http://localhost:8081/actuator/health
- Info: http://localhost:8081/actuator/info
