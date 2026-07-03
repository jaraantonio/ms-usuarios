package com.perfulandia.usuarios.service;

import com.perfulandia.usuarios.model.entity.Permiso;
import com.perfulandia.usuarios.model.entity.RolPermiso;
import com.perfulandia.usuarios.repository.PermisoRepository;
import com.perfulandia.usuarios.repository.RolPermisoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermisoService {

    private final PermisoRepository permisoRepository;
    private final RolPermisoRepository rolPermisoRepository;

    @Transactional(readOnly = true)
    public List<Permiso> listarPermisos() {
        return permisoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Permiso> obtenerPermisosPorRol(String rol) {
        List<RolPermiso> asignaciones = rolPermisoRepository.findByRol(rol.toUpperCase());
        return asignaciones.stream().map(RolPermiso::getPermiso).toList();
    }

    @Transactional
    public void asignarPermisosARol(String rol, List<Long> permisoIds) {
        String rolUpper = rol.toUpperCase();
        rolPermisoRepository.deleteByRol(rolUpper);

        List<Permiso> permisos = permisoRepository.findAllById(permisoIds);
        for (Permiso permiso : permisos) {
            RolPermiso rp = RolPermiso.builder()
                .rol(rolUpper)
                .permiso(permiso)
                .build();
            rolPermisoRepository.save(rp);
        }
        log.info("Permisos asignados al rol {}: {}", rolUpper, permisoIds);
    }
}
