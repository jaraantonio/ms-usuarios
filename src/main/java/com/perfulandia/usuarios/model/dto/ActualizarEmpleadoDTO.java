package com.perfulandia.usuarios.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActualizarEmpleadoDTO(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Formato de correo inválido")
        @Size(max = 150, message = "El correo no puede exceder 150 caracteres")
        String email,

        @NotNull(message = "El rol es obligatorio")
        String rol,

        Long idSucursalAsignada) {
}