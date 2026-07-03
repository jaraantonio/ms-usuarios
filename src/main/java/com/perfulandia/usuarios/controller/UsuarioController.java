package com.perfulandia.usuarios.controller;

import com.perfulandia.usuarios.model.dto.*;
import com.perfulandia.usuarios.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "API de gestión de usuarios y autenticación - Perfulandia SPA")
public class UsuarioController {

    private final UsuarioService usuarioService;

    // ============================================================
    // HU-01: Registro de cliente
    // ============================================================
    @Operation(
            summary = "Registrar nuevo cliente",
            description = "Crea una cuenta de usuario con rol CLIENTE. La contraseña debe tener al menos 8 caracteres, incluyendo mayúsculas, minúsculas y números."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o correo duplicado")
    })
    @PostMapping("/auth/registro")
    public ResponseEntity<PerfilResponseDTO> registrarCliente(@Valid @RequestBody RegistroRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.registrarCliente(dto));
    }

    // ============================================================
    // HU-02: Login
    // ============================================================
    @Operation(
            summary = "Iniciar sesión",
            description = "Autentica al usuario con email y contraseña. Bloquea la cuenta tras 3 intentos fallidos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login exitoso"),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas o cuenta bloqueada")
    })
    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(usuarioService.autenticar(dto));
    }

    // ============================================================
    // HU-03: Obtener perfil
    // ============================================================
    @Operation(
            summary = "Obtener perfil",
            description = "Obtiene los datos del perfil de un usuario por su ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil obtenido exitosamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @GetMapping("/usuarios/{id}/perfil")
    public ResponseEntity<PerfilResponseDTO> obtenerPerfil(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtenerPerfil(id));
    }

    // ============================================================
    // HU-04: Actualizar perfil
    // ============================================================
    @Operation(
            summary = "Actualizar perfil",
            description = "Actualiza nombre, dirección y método de pago de un usuario por su ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    @PutMapping("/usuarios/{id}/perfil")
    public ResponseEntity<PerfilResponseDTO> actualizarPerfil(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarPerfilDTO dto) {
        return ResponseEntity.ok(usuarioService.actualizarPerfil(id, dto));
    }

    // ============================================================
    // HU-CP: Cambiar contraseña
    // ============================================================
    @Operation(
            summary = "Cambiar contraseña",
            description = "Permite al usuario cambiar su contraseña proporcionando la actual y una nueva."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contraseña cambiada exitosamente"),
            @ApiResponse(responseCode = "401", description = "La contraseña actual no es correcta")
    })
    @PutMapping("/usuarios/{id}/password")
    public ResponseEntity<Void> cambiarPassword(
            @PathVariable Long id,
            @Valid @RequestBody CambiarPasswordRequestDTO dto) {
        usuarioService.cambiarPassword(id, dto);
        return ResponseEntity.ok().build();
    }

    // ============================================================
    // HU-45: Cerrar sesión
    // ============================================================
    @Operation(
            summary = "Cerrar sesión",
            description = "Cierra la sesión del usuario."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión cerrada exitosamente")
    })
    @PostMapping("/auth/logout")
    public ResponseEntity<Void> cerrarSesion() {
        return ResponseEntity.ok().build();
    }

    // ============================================================
    // HU-44a: Recuperar contraseña
    // ============================================================
    @Operation(
            summary = "Recuperar contraseña",
            description = "Solicita un token de recuperación de contraseña. Si el correo existe, se envía un enlace de recuperación al correo registrado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud procesada (revisa tu correo electrónico)")
    })
    @PostMapping("/auth/recuperar")
    public ResponseEntity<String> recuperarPassword(@Valid @RequestBody CorreoRequestDTO dto) {
        return ResponseEntity.ok(usuarioService.recuperarPassword(dto.correo()));
    }

    // ============================================================
    // HU-44b: Restablecer contraseña
    // ============================================================
    @Operation(
            summary = "Restablecer contraseña",
            description = "Restablece la contraseña usando un token de recuperación válido y no expirado. El token se invalida tras su uso."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contraseña restablecida exitosamente"),
            @ApiResponse(responseCode = "400", description = "Token inválido, expirado o ya usado")
    })
    @PostMapping("/auth/restablecer")
    public ResponseEntity<String> restablecerPassword(@Valid @RequestBody RestablecerPasswordRequestDTO dto) {
        usuarioService.restablecerPassword(dto);
        return ResponseEntity.ok("Contraseña restablecida exitosamente");
    }

    // ============================================================
    // HU-06: Listar usuarios (ADMIN)
    // ============================================================
    @Operation(
            summary = "Listar usuarios",
            description = "Lista todos los usuarios del sistema. Filtros opcionales por ?rol= y ?estado=."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de usuarios retornada exitosamente")
    })
    @GetMapping("/usuarios")
    public ResponseEntity<List<PerfilResponseDTO>> listarUsuarios(
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(usuarioService.listarUsuarios(rol, estado));
    }

    // ============================================================
    // HU-05: Crear usuario (ADMIN) — devuelve contraseña temporal
    // ============================================================
    @Operation(
            summary = "Crear usuario",
            description = "Crea un nuevo usuario en el sistema asignando rol y generando contraseña temporal. La contraseña se devuelve en la respuesta."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente. Incluye contraseña temporal."),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o correo duplicado")
    })
    @PostMapping("/usuarios")
    public ResponseEntity<CrearEmpleadoResponseDTO> crearUsuarioAdmin(@Valid @RequestBody CrearEmpleadoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crearUsuarioAdmin(dto));
    }

    // ============================================================
    // HU-07: Actualizar usuario (ADMIN)
    // ============================================================
    @Operation(
            summary = "Actualizar usuario",
            description = "Modifica nombre, email y rol de un usuario. Previene que un ADMIN se quite su propio rol."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o restricción de negocio"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @PutMapping("/usuarios/{id}")
    public ResponseEntity<PerfilResponseDTO> actualizarUsuarioAdmin(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarEmpleadoDTO dto) {
        return ResponseEntity.ok(usuarioService.actualizarUsuarioAdmin(id, dto));
    }

    // ============================================================
    // HU-08: Desactivar usuario (ADMIN) - borrado lógico
    // ============================================================
    @Operation(
            summary = "Desactivar usuario",
            description = "Realiza un borrado lógico (cambia estado a INACTIVO). No se puede desactivar a un ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario desactivado exitosamente"),
            @ApiResponse(responseCode = "400", description = "No se puede desactivar ADMIN"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<Void> desactivarUsuario(@PathVariable Long id) {
        usuarioService.desactivarUsuario(id);
        return ResponseEntity.ok().build();
    }

    // ============================================================
    // HU-02 complemento: Desbloquear usuario (ADMIN)
    // ============================================================
    @Operation(
            summary = "Desbloquear usuario",
            description = "Reactivar una cuenta bloqueada por intentos fallidos. Solo ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario desbloqueado exitosamente"),
            @ApiResponse(responseCode = "400", description = "El usuario no está bloqueado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @PutMapping("/usuarios/{id}/desbloquear")
    public ResponseEntity<Void> desbloquearUsuario(@PathVariable Long id) {
        usuarioService.desbloquearUsuario(id);
        return ResponseEntity.ok().build();
    }
}
