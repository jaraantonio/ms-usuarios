package com.perfulandia.usuarios.controller;

import com.perfulandia.usuarios.model.dto.RolPermisoRequestDTO;
import com.perfulandia.usuarios.model.entity.Permiso;
import com.perfulandia.usuarios.service.PermisoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(name = "Permisos", description = "Gestión de permisos por rol — HU-09")
public class PermisoController {

    private final PermisoService permisoService;

    @GetMapping("/permisos")
    @Operation(summary = "Listar permisos", description = "Retorna todos los permisos disponibles en el sistema.")
    public ResponseEntity<List<Permiso>> listarPermisos() {
        return ResponseEntity.ok(permisoService.listarPermisos());
    }

    @GetMapping("/roles/{rol}/permisos")
    @Operation(summary = "Obtener permisos de un rol", description = "Retorna los permisos asignados a un rol específico.")
    public ResponseEntity<List<Permiso>> obtenerPermisosPorRol(@PathVariable String rol) {
        return ResponseEntity.ok(permisoService.obtenerPermisosPorRol(rol));
    }

    @PutMapping("/roles/{rol}/permisos")
    @Operation(summary = "Asignar permisos a un rol", description = "Reemplaza todos los permisos de un rol por los especificados.")
    public ResponseEntity<Void> asignarPermisosARol(
            @PathVariable String rol,
            @Valid @RequestBody RolPermisoRequestDTO dto) {
        permisoService.asignarPermisosARol(rol, dto.permisoIds());
        return ResponseEntity.ok().build();
    }
}
