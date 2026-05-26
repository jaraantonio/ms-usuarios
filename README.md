# ms-usuarios

Microservicio para la gestión de identidades, autenticación, y control de acceso basado en roles. Provee seguridad mediante tokens JWT y encriptación de contraseñas.

## Stack Tecnológico

* **Lenguaje:** Java 25
* **Framework Core:** Spring Boot 4.0.6
* **Seguridad:** Spring Security, JJWT (0.12.5)
* **Persistencia:** Spring Data JPA, Hibernate
* **Base de Datos:** MySQL 8+
* **Validación:** Spring Boot Starter Validation (Jakarta)
* **Herramientas:** Maven, Lombok, Spring Boot Actuator

## Requisitos Previos

Asegúrate de tener instalado lo siguiente en tu entorno local:

* Java Development Kit (JDK) 25
* Apache Maven 3.8+ (O usar el wrapper incluido `./mvnw`)
* MySQL Server corriendo en el puerto `3306`

## Características Principales

* **Autenticación y Autorización:** Implementación de JWT (JSON Web Tokens) sin estado (Stateless). Filtros de seguridad personalizados (`JwtAuthFilter`).
* **Control de Acceso Basado en Roles:** Soporte para roles `ADMIN`, `GERENTE`, `EMPLEADO` y `CLIENTE`.
* **Gestión de Sesiones:** Invalidación explícita de tokens (Logout) persistida en base de datos.
* **Recuperación de Credenciales:** Generación y validación de tokens de recuperación con tiempo de expiración.
* **Seguridad de Datos:** Encriptación de contraseñas usando BCrypt.
* **Manejo Global de Excepciones:** Respuestas estandarizadas para errores de validación, credenciales inválidas y conflictos de recursos vía `@RestControllerAdvice`.

## Configuración

1. Clonar el repositorio y acceder al directorio.
2. Tener un servidor MySQL en ejecución. La base de datos `db_usuarios` se creará automáticamente.
Si necesitas modificar las credenciales, edita el archivo `application.properties`:

```properties
spring.datasource.username=tu_usuario
spring.datasource.password=tu_contrasena

```

3. Compilar y ejecutar:

```bash
./mvnw spring-boot:run

```

El microservicio estará disponible en `http://localhost:8081`.

4. **Verificar estado:**
Puedes verificar que el servicio levantó correctamente con:
`GET http://localhost:8081/actuator/health`

## Endpoints de la API

### Autenticación (Públicos)

| Método | Endpoint | Descripción |
| --- | --- | --- |
| `POST` | `/api/v1/usuarios/registro` | Registra un nuevo `CLIENTE`. |
| `POST` | `/api/v1/usuarios/login` | Autentica un usuario y retorna un token JWT. |
| `POST` | `/api/v1/usuarios/recuperar-password` | Inicia flujo de recuperación de clave. |
| `POST` | `/api/v1/usuarios/logout` | Invalida el token JWT actual (Requiere Auth). |

### Perfil (Requiere Auth - Rol: `CLIENTE`)

| Método | Endpoint | Descripción |
| --- | --- | --- |
| `PUT` | `/api/v1/usuarios/perfil` | Actualiza la información del perfil del cliente autenticado. |

### Administración (Requiere Auth - Rol: `ADMIN`)

| Método | Endpoint | Descripción |
| --- | --- | --- |
| `GET` | `/api/v1/usuarios/admin/usuarios` | Lista todos los usuarios (Paginado). |
| `POST` | `/api/v1/usuarios/admin/empleados` | Crea un usuario con rol administrativo/operativo y retorna contraseña temporal. |
| `PUT` | `/api/v1/usuarios/admin/usuarios/{id}` | Actualiza datos y rol de un empleado. |
| `DELETE` | `/api/v1/usuarios/admin/usuarios/{id}` | Desactiva (Soft Delete) la cuenta de un usuario. |