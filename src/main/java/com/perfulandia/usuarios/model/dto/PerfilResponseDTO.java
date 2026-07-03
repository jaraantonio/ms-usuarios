package com.perfulandia.usuarios.model.dto;

import com.perfulandia.usuarios.model.enums.EstadoUsuario;

public record PerfilResponseDTO(
        Long id,
        String nombre,
        String email,
        String telefono,
        String rol,
        EstadoUsuario estado,
        String direccion,
        String metodoPagoOfuscado) {
}