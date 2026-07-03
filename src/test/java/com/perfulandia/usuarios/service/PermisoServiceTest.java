package com.perfulandia.usuarios.service;

import com.perfulandia.usuarios.model.entity.Permiso;
import com.perfulandia.usuarios.model.entity.RolPermiso;
import com.perfulandia.usuarios.repository.PermisoRepository;
import com.perfulandia.usuarios.repository.RolPermisoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermisoService — Pruebas unitarias")
class PermisoServiceTest {

    @Mock private PermisoRepository permisoRepository;
    @Mock private RolPermisoRepository rolPermisoRepository;
    @InjectMocks private PermisoService permisoService;

    private Permiso permiso;

    @BeforeEach
    void setUp() {
        permiso = new Permiso(1L, "GESTIONAR_USUARIOS", "Descripción", "USUARIOS");
    }

    @Test
    @DisplayName("listarPermisos retorna todos los permisos")
    void testListarPermisos() {
        when(permisoRepository.findAll()).thenReturn(List.of(permiso));
        List<Permiso> result = permisoService.listarPermisos();
        assertEquals(1, result.size());
        assertEquals("GESTIONAR_USUARIOS", result.get(0).getNombre());
    }

    @Test
    @DisplayName("asignarPermisosARol elimina previos y asigna nuevos")
    void testAsignarPermisosARol() {
        when(permisoRepository.findAllById(List.of(1L))).thenReturn(List.of(permiso));
        permisoService.asignarPermisosARol("EMPLEADO", List.of(1L));
        verify(rolPermisoRepository, times(1)).deleteByRol("EMPLEADO");
        verify(rolPermisoRepository, times(1)).save(any(RolPermiso.class));
    }

    @Test
    @DisplayName("obtenerPermisosPorRol retorna lista vacía si no hay asignaciones")
    void testObtenerPermisosPorRol_SinPermisos() {
        when(rolPermisoRepository.findByRol("CLIENTE")).thenReturn(List.of());
        List<Permiso> result = permisoService.obtenerPermisosPorRol("CLIENTE");
        assertTrue(result.isEmpty());
    }
}
