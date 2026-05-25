package com.perfulandia.usuarios.repository;

import com.perfulandia.usuarios.model.entity.TokenInvalidado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenInvalidadoRepository extends JpaRepository<TokenInvalidado, String> {
}