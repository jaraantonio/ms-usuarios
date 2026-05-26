# Microservicio de Usuarios (ms-usuarios)

## Descripción del Proyecto
Microservicio encargado de la gestión de identidades, perfiles, autenticación y autorización (basada en JWT) dentro del sistema.

## Estudiante
* Antonio Jara

## Funcionalidades Implementadas
- Registro y mantenimiento de usuarios (Clientes y Empleados).
- Autenticación y control de acceso mediante JSON Web Tokens (JWT).
- Gestión de roles de sistema.
- Mecanismos de recuperación de credenciales e invalidación de tokens.

## Pasos para Ejecutar

1. **Requisitos previos:**
   * Java Development Kit (JDK) 17 o superior.
   * Apache Maven.
   * Base de datos operativa y configurada en `application.properties`.

2. **Compilación del proyecto:**
   Abre una terminal en la raíz del proyecto y ejecuta:
   ```bash
   ./mvnw clean install
   ```
   *(En Windows utiliza `mvnw.cmd clean install`)*

3. **Ejecución del servicio:**
   Levanta la aplicación mediante Spring Boot:
   ```bash
   ./mvnw spring-boot:run
   ```
   *(En Windows utiliza `mvnw.cmd spring-boot:run`)*

## APIs para Pruebas en Postman

**Consideración de Seguridad:** El endpoint de `/login` genera un Bearer Token (`JWT`). Para las rutas protegidas, debes incluir este token en los Headers de tu petición HTTP en Postman:
`Authorization: Bearer <tu_token_jwt>`

### 1. Autenticación y Registro (Públicos)

* **Registro de Cliente**
    * **Método:** `POST`
    * **Ruta:** `/api/v1/usuarios/registro`
    * **Requiere Token:** No
    * **Payload (JSON crudo):**
        ```json
        {
          "nombre": "Juan Perez",
          "correo": "juan.perez@email.com",
          "contrasena": "Password123",
          "direccion": "Calle Falsa 123"
        }
        ```

* **Login (Generación de Token JWT)**
    * **Método:** `POST`
    * **Ruta:** `/api/v1/usuarios/login`
    * **Requiere Token:** No
    * **Payload (JSON crudo):**
        ```json
        {
          "correo": "juan.perez@email.com",
          "contrasena": "Password123"
        }
        ```

* **Recuperación de Contraseña**
    * **Método:** `POST`
    * **Ruta:** `/api/v1/usuarios/recuperar-password`
    * **Requiere Token:** No
    * **Payload (JSON crudo):**
        ```json
        {
          "correo": "juan.perez@email.com"
        }
        ```

### 2. Operaciones de Usuario (Protegidas)

* **Cerrar Sesión (Logout)**
    * **Método:** `POST`
    * **Ruta:** `/api/v1/usuarios/logout`
    * **Requiere Token:** Sí (Cualquier usuario autenticado)

* **Actualizar Perfil**
    * **Método:** `PUT`
    * **Ruta:** `/api/v1/usuarios/perfil`
    * **Requiere Token:** Sí
    * **Permiso/Rol Requerido:** `CLIENTE`
    * **Payload (JSON crudo):**
        ```json
        {
          "nombre": "Juan Perez Modificado",
          "direccion": "Nueva Direccion 456"
        }
        ```

### 3. Administración (Protegidas)

* **Crear Empleado**
    * **Método:** `POST`
    * **Ruta:** `/api/v1/usuarios/admin/empleados`
    * **Requiere Token:** Sí
    * **Permiso/Rol Requerido:** `ADMIN`
    * **Payload (JSON crudo):**
        ```json
        {
          "nombre": "Carlos Empleado",
          "correo": "carlos@empresa.com",
          "rol": "EMPLEADO"
        }
        ```

* **Listar Usuarios Paginados**
    * **Método:** `GET`
    * **Ruta:** `/api/v1/usuarios/admin/usuarios?page=0&size=10`
    * **Requiere Token:** Sí
    * **Permiso/Rol Requerido:** `ADMIN`

* **Actualizar Empleado**
    * **Método:** `PUT`
    * **Ruta:** `/api/v1/usuarios/admin/usuarios/{id}`
    * **Requiere Token:** Sí
    * **Permiso/Rol Requerido:** `ADMIN`
    * **Payload (JSON crudo):**
        ```json
        {
          "nombre": "Carlos Modificado",
          "correo": "carlos@empresa.com",
          "rol": "GERENTE"
        }
        ```

* **Desactivar Usuario**
    * **Método:** `DELETE`
    * **Ruta:** `/api/v1/usuarios/admin/usuarios/{id}`
    * **Requiere Token:** Sí
    * **Permiso/Rol Requerido:** `ADMIN`