package com.perfulandia.usuarios.model.dto;

import com.perfulandia.usuarios.model.enums.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CrearEmpleadoDTO(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Formato de correo inválido")
        String email,

        @NotNull(message = "El rol es obligatorio")
        Rol rol) {
}