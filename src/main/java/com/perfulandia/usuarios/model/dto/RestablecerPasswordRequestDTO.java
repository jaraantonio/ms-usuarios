package com.perfulandia.usuarios.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RestablecerPasswordRequestDTO(
        @NotBlank(message = "El token es obligatorio") String token,

        @NotBlank(message = "La nueva contrasena es obligatoria") @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,64}$", message = "La contrasena debe tener entre 8 y 64 caracteres, mayuscula, minuscula y numero") String nuevaContrasena) {
}