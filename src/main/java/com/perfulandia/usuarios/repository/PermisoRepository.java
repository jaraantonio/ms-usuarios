package com.perfulandia.usuarios.repository;

import com.perfulandia.usuarios.model.entity.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PermisoRepository extends JpaRepository<Permiso, Long> {
    Optional<Permiso> findByNombre(String nombre);
}
