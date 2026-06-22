package com.perfulandia.usuarios.model.dto;

public record LoginResponseDTO(
        String token,
        String rol) {
}