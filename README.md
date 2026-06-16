# Microservicio de Usuarios (ms-usuarios)

## Descripción
Microservicio de gestión de identidades, autenticación JWT, roles (ADMIN, CLIENTE, EMPLEADO, GERENTE) y recuperación de contraseña.  
Cumple con las historias de usuario HU-01 a HU-09, HU-44, HU-45.

## Estudiante
Antonio Jara

## Tecnologías
- Java 25, Spring Boot 4.0.6, Spring Security, JWT, JPA/Hibernate, MariaDB/MySQL, Maven.

## Endpoints principales

### Públicos
- `POST /api/v1/usuarios/registro` - Registrar cliente.
- `POST /api/v1/usuarios/login` - Iniciar sesión → retorna JWT.
- `POST /api/v1/usuarios/recuperar-password` - Solicitar token de recuperación.
- `POST /api/v1/usuarios/restablecer-password` - Restablecer contraseña con token.

### Autenticados (requieren token)
- `GET /api/v1/usuarios/perfil` - Ver perfil propio (rol CLIENTE).
- `PUT /api/v1/usuarios/perfil` - Actualizar nombre, dirección y método de pago.
- `POST /api/v1/usuarios/logout` - Invalidar token.

### Administración (rol ADMIN)
- `POST /admin/empleados` - Crear empleado (retorna contraseña temporal).
- `GET /admin/usuarios` - Listado paginado de usuarios.
- `PUT /admin/usuarios/{id}` - Actualizar empleado (nombre, correo, rol).
- `DELETE /admin/usuarios/{id}` - Desactivar usuario (borrado lógico).

## Configuración de base de datos
La BD se crea automáticamente con `ddl-auto=update`. Credenciales en `application.properties`.

## Ejecución
```bash
./mvnw spring-boot:run
```
El servidor corre en http://localhost:8081.

# PRUEBAS POSTMAN

- Microservicio `ms-usuarios` corriendo en `http://localhost:8081`.
- Ejecutar en orden

## Pruebas con Cliente

### 1. **Registro de cliente (HU-01)**
- **Endpoint:** `POST /api/v1/usuarios/registro`
- **Body:**
```json
{
  "nombre": "Luis Jara",
  "correo": "luchojara@prueba.cl",
  "contrasena": "Pass1234",
  "direccion": "Calle 123"
}
```
- **Respuesta esperada:**  
  `201 Created` y JSON con la siguiente estructura:
```json
{
  "idUsuario": 1,
  "nombre": "Luis Jara",
  "correo": "luchojara@prueba.cl",
  "rol": "CLIENTE",
  "direccion": "Calle 123",
  "metodoPagoOfuscado": "null"
}
```
- **Validaciones:**  
  - Contraseña con mayúscula, minúscula y número (patrón validado en DTO).  
  - Correo único.  
  - Dirección obligatoria.

### 2. **Registro con correo duplicado**
- **Mismo endpoint** con el mismo `correo: "luchojara@prueba.cl"`.
- **Respuesta esperada:** `409 Conflict` con mensaje `"El correo ya está registrado."`

### 3. **Registro con contraseña débil**
- **Body:** misma estructura pero `"contrasena": "1234"`.
- **Respuesta esperada:** `400 Bad Request` con error de validación indicando requisitos para la contraseña.

### 4. **Inicio de sesión correcto (HU-02)**
- **Endpoint:** `POST /api/v1/usuarios/login`
- **Body:**
```json
{
  "correo": "luchojara@prueba.cl",
  "contrasena": "Pass1234"
}
```
- **Respuesta esperada:** `200 OK` con `{ "token": "eyJhbGciOiJ..." }`

### 5. **Inicio de sesión con credenciales incorrectas (primer intento)**
- **Body:** misma estructura pero con contraseña incorrecta.
- **Respuesta esperada:** `401 Unauthorized` con mensaje `"Credenciales incorrectas."`

### 6. **Dos intentos fallidos adicionales (segundo y tercero)**
- Repite el paso 5 dos veces más (total 3 intentos fallidos).
- **Después del tercer intento fallido**, 

### 7. **Intento de login con cuenta bloqueada**
- **Body:** credenciales correctas (`"Pass1234"`).
- **Respuesta esperada:** `401 Unauthorized` con mensaje `"Cuenta bloqueada."`

### 8. **Obtener perfil del cliente (HU-03)**
- Para continuar probando, necesitamos una cuenta activa. 
Vamos a registrar un segundo cliente:
  - `POST /registro` con
```json
{
  "nombre": "Luis Dos",
  "correo": "luchojarados@prueba.cl",
  "contrasena": "Contrasena1234",
  "direccion": "Calle 123"
}
```
  - Login con `luchojarados@prueba.cl` / `Contrasena1234` → guardar token.

- **Endpoint (requiere token):** `GET /api/v1/usuarios/perfil`  
  **Header:** `Authorization: Bearer token`
- **Respuesta esperada:** `200 OK` con los datos del perfil (incluye `metodoPagoOfuscado` inicial, null).

### 9. **Actualizar perfil (nombre, dirección y método de pago) (HU-04)**
- **Endpoint:** `PUT /api/v1/usuarios/perfil`  
  **Header:** `Authorization: Bearer token`
- **Body:**
```json
{
  "nombre": "Luis Cyr",
  "direccion": "Calle Mish 123",
  "nuevoMetodoPago": "1111222233334444"
}
```
- **Respuesta esperada:** `200 OK`
- **Verificar:** Vuelve a hacer `GET /perfil` y observar datos actualizados.

### 10. **Cerrar sesión (HU-45)**
- **Endpoint:** `POST /api/v1/usuarios/logout`  
  **Header:** `Authorization: Bearer token`
- **Respuesta esperada:** `200 OK`
- **Verificar:** Usa el mismo token en `GET /perfil` → debe dar `401 Unauthorized` (token invalidado), asimismo para actualizar el perfil.

Claro, a continuación tienes la sección reescrita sin emojis y adaptada para crear el usuario administrador directamente en la base de datos usando el hash bcrypt que proporcionaste. Puedes copiar este bloque y reemplazar desde "Pruebas con rol ADMIN" hasta el final de la recuperación de contraseña.

## Pruebas con rol ADMIN

Para probar los endpoints necesitas un usuario con rol ADMIN. Puedes crearlo directamente en la base de datos.

### Crear usuario administrador de prueba en la base de datos

Conéctate a tu base de datos `db_usuarios` y ejecuta la siguiente sentencia SQL:

```sql
INSERT INTO usuarios (nombre, correo, contrasena, rol, estado, intentos_fallidos, metodo_pago_ofuscado)
VALUES ('Administrador', 'admin@perfulandia.cl', '$2a$12$znkX4EyYutL4VdSsOjiaxu89boTQj/nu0T6zkebhCO88IQ3rfJ/AK', 'ADMIN', 'ACTIVO', 0, NULL);
```

La contraseña en texto plano es `admin1234`. El hash bcrypt ya está calculado.

### 11. Login como administrador

- **Endpoint:** `POST /api/v1/usuarios/login`
- **Body:**
```json
{
  "correo": "admin@perfulandia.cl",
  "contrasena": "admin1234"
}
```
- **Respuesta esperada:** `200 OK` con un token JWT. Guarda este token como `tokenAdmin`.

### 12. Crear empleado (HU-05)

- **Endpoint:** `POST /api/v1/usuarios/admin/empleados`
- **Header:** `Authorization: Bearer tokenAdmin`
- **Body:**
```json
{
  "nombre": "Carlos Soto",
  "correo": "carlos@empresa.cl",
  "rol": "EMPLEADO"
}
```
- **Respuesta esperada:** `201 Created` con un JSON que contiene la contraseña temporal, por ejemplo:
```json
{
  "passwordTemporal": "A1b2C3d4"
}
```

### 13. Listar usuarios paginados (HU-06)

- **Endpoint:** `GET /api/v1/usuarios/admin/usuarios?page=0&size=10`
- **Header:** `Authorization: Bearer tokenAdmin`
- **Respuesta esperada:** `200 OK` con una página de objetos `PerfilResponseDTO`. Verifica que no se exponga el campo `contrasena`.

### 14. Actualizar empleado (HU-07)

- Primero obtén el `id` del empleado recién creado (aparece en la lista del paso anterior o en la respuesta de creación, aunque ahí no viene el id, así que usa el listado).
- **Endpoint:** `PUT /api/v1/usuarios/admin/usuarios/{id}`
- **Header:** `Authorization: Bearer tokenAdmin`
- **Body:**
```json
{
  "nombre": "Carlos Soto Actualizado",
  "correo": "carlos.nuevo@empresa.cl",
  "rol": "GERENTE"
}
```
- **Respuesta esperada:** `200 OK`.
- **Verificación:** Vuelve a listar usuarios y confirma que los datos del empleado hayan cambiado.

### 15. Intentar que el administrador se degrade a sí mismo

- Obtén el `id` del propio administrador (búscalo en el listado por su correo `admin@perfulandia.cl`).
- **Endpoint:** `PUT /api/v1/usuarios/admin/usuarios/{idAdmin}`
- **Header:** `Authorization: Bearer tokenAdmin`
- **Body:**
```json
{
  "nombre": "Administrador",
  "correo": "admin@perfulandia.cl",
  "rol": "CLIENTE"
}
```
- **Respuesta esperada:** `500 Internal Server Error` con el mensaje `"Un administrador no puede cambiar su propio rol."`

### 16. Desactivar un empleado (HU-08)

- Usa el `id` del empleado creado en el paso 12.
- **Endpoint:** `DELETE /api/v1/usuarios/admin/usuarios/{idEmpleado}`
- **Header:** `Authorization: Bearer tokenAdmin`
- **Respuesta esperada:** `204 No Content`.
- **Verificación:** Intenta iniciar sesión con el empleado (correo `carlos@empresa.cl` y la contraseña temporal que se generó). Debe devolver `401 Unauthorized` con el mensaje `"Cuenta bloqueada."`

### 17. Administrador no puede desactivarse a sí mismo

- **Endpoint:** `DELETE /api/v1/usuarios/admin/usuarios/{idAdmin}`
- **Header:** `Authorization: Bearer tokenAdmin`
- **Respuesta esperada:** `500 Internal Server Error` con el mensaje `"Un administrador no puede desactivar su propia cuenta."`

## Recuperación de contraseña (HU-44)

Usaremos un cliente activo (por ejemplo, el que registraste antes, como `luis@testdos.cl`).

### 18. Solicitar token de recuperación

- **Endpoint:** `POST /api/v1/usuarios/recuperar-password`
- **Body:**
```json
{
  "correo": "luis@testdos.cl"
}
```
- **Respuesta esperada:** `200 OK`.
- **Revisa los logs de la consola** del microservicio. Busca una línea similar a:
  `Token de recuperación generado para luis@testdos.cl: 123e4567-e89b-12d3-a456-426614174000`
  Copia ese UUID.

### 19. Restablecer contraseña con token válido

- **Endpoint:** `POST /api/v1/usuarios/restablecer-password`
- **Body:**
```json
{
  "token": "uuid-copiado",
  "nuevaContrasena": "NuevaPass456"
}
```
- **Respuesta esperada:** `200 OK`.
- **Verificación:** Inicia sesión con `luis@testdos.cl` y la nueva contraseña `NuevaPass456`. Debe funcionar correctamente.

### 20. Usar token inválido

- **Endpoint:** `POST /api/v1/usuarios/restablecer-password`
- **Body:**
```json
{
  "token": "cualquier-cosa",
  "nuevaContrasena": "Pass123"
}
```
- **Respuesta esperada:** `404 Not Found` con el mensaje `"Token inválido o expirado."`