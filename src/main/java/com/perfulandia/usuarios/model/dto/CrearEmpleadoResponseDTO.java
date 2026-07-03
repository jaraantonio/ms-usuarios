package com.perfulandia.usuarios.model.dto;

import com.perfulandia.usuarios.model.enums.EstadoUsuario;
import com.perfulandia.usuarios.model.enums.Rol;

/**
 * DTO para la respuesta de creación de empleado.
 * Incluye la contraseña temporal generada (solo se entrega una vez).
 */
public record CrearEmpleadoResponseDTO(
        Long id,
        String nombre,
        String email,
        Rol rol,
        EstadoUsuario estado,
        String direccion,
        String metodoPagoOfuscado,
        String contrasenaTemporal) {
}
