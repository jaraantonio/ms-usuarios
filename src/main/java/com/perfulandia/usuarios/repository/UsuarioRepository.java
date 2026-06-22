package com.perfulandia.usuarios.repository;

import com.perfulandia.usuarios.model.entity.Usuario;
import com.perfulandia.usuarios.model.enums.EstadoUsuario;
import com.perfulandia.usuarios.model.enums.Rol;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Page<Usuario> findByRol(Rol rol, Pageable pageable);

    Page<Usuario> findByEstado(EstadoUsuario estado, Pageable pageable);

    Page<Usuario> findByRolAndEstado(Rol rol, EstadoUsuario estado, Pageable pageable);
}