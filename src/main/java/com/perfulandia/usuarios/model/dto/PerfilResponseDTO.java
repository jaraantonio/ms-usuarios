package com.perfulandia.usuarios.model.dto;

import com.perfulandia.usuarios.model.enums.EstadoUsuario;
import com.perfulandia.usuarios.model.enums.Rol;

public record PerfilResponseDTO(
        Long id,
        String nombre,
        String email,
        Rol rol,
        EstadoUsuario estado,
        String direccion,
        String metodoPagoOfuscado) {
}