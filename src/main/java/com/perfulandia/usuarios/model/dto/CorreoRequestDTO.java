package com.perfulandia.usuarios.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CorreoRequestDTO(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Formato de correo inválido")
        String correo) {
}