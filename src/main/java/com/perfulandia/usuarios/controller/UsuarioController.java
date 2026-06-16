package com.perfulandia.usuarios.controller;

import com.perfulandia.usuarios.model.dto.*;
import com.perfulandia.usuarios.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @PostMapping("/registro")
    public ResponseEntity<PerfilResponseDTO> registrar(@Valid @RequestBody RegistroRequestDTO dto) {
        return new ResponseEntity<>(service.registrarCliente(dto), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> credenciales) {
        String token = service.iniciarSesion(credenciales.get("correo"), credenciales.get("contrasena"));
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/recuperar-password")
    public ResponseEntity<Void> solicitarRecuperacion(@RequestBody Map<String, String> request) {
        service.solicitarRecuperacion(request.get("correo"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/restablecer-password")
    public ResponseEntity<Void> restablecerPassword(@Valid @RequestBody RestablecerPasswordRequestDTO dto) {
        service.restablecerPassword(dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        service.cerrarSesion(authHeader.substring(7));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/perfil")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<PerfilResponseDTO> obtenerPerfil(Principal principal) {
        return ResponseEntity.ok(service.obtenerPerfil(principal.getName()));
    }

    @PutMapping("/perfil")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<Void> actualizarPerfil(@Valid @RequestBody ActualizarPerfilDTO dto, Principal principal) {
        service.actualizarPerfil(principal.getName(), dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/empleados")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> crearEmpleado(@Valid @RequestBody CrearEmpleadoDTO dto) {
        String temporal = service.crearEmpleado(dto);
        return new ResponseEntity<>(Map.of("passwordTemporal", temporal), HttpStatus.CREATED);
    }

    @GetMapping("/admin/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<PerfilResponseDTO>> listarUsuarios(Pageable pageable) {
        return ResponseEntity.ok(service.listarUsuarios(pageable));
    }

    @PutMapping("/admin/usuarios/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> actualizarEmpleado(@PathVariable Long id,
            @Valid @RequestBody ActualizarEmpleadoDTO dto,
            Principal principal) {
        service.actualizarEmpleado(id, dto, principal.getName());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/admin/usuarios/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desactivarUsuario(@PathVariable Long id, Principal principal) {
        service.desactivarCuenta(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}