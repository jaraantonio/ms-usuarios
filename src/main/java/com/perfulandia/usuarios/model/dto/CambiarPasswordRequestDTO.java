package com.perfulandia.usuarios.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO para cambio de contraseña de un usuario autenticado.
 */
public record CambiarPasswordRequestDTO(
        @NotBlank(message = "La contraseña actual es obligatoria")
        String passwordActual,

        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,64}$",
                message = "La contraseña debe tener entre 8 y 64 caracteres, al menos una mayúscula, una minúscula y un número"
        )
        String nuevaPassword) {
}
