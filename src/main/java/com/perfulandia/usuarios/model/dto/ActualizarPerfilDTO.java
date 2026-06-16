package com.perfulandia.usuarios.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarPerfilDTO(
                @NotBlank(message = "El nombre es obligatorio") @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres") String nombre,

                @NotBlank(message = "La direccion es obligatoria") @Size(max = 255, message = "La direccion no puede exceder 255 caracteres") String direccion,

                String nuevoMetodoPago
) {
}