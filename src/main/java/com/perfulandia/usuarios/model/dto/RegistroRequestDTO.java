package com.perfulandia.usuarios.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroRequestDTO(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Formato de correo inválido")
        @Size(max = 150, message = "El correo no puede exceder los 150 caracteres")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,64}$",
                message = "La contraseña debe tener entre 8 y 64 caracteres, al menos una mayúscula, una minúscula y un número"
        )
        String password,

        @NotBlank(message = "La dirección de envío es obligatoria")
        @Size(max = 255, message = "La dirección no puede exceder los 255 caracteres")
        String direccion,

        @Pattern(regexp = "^\\+569\\d{8}$", message = "El teléfono debe tener formato +569XXXXXXXX (ej: +56912345678)")
        String telefono) {
}