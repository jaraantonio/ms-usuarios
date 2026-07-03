package com.perfulandia.usuarios.model.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RolPermisoRequestDTO(
    @NotNull(message = "La lista de IDs de permiso es obligatoria")
    List<Long> permisoIds
) {}
