package com.perfulandia.usuarios.repository;

import com.perfulandia.usuarios.model.entity.RolPermiso;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RolPermisoRepository extends JpaRepository<RolPermiso, Long> {
    List<RolPermiso> findByRol(String rol);
    void deleteByRol(String rol);
}
