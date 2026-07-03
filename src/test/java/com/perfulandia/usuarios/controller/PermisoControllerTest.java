package com.perfulandia.usuarios.controller;

import com.perfulandia.usuarios.model.entity.Permiso;
import com.perfulandia.usuarios.service.PermisoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PermisoController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PermisoController — Pruebas WebMvc")
class PermisoControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PermisoService permisoService;

    @Test
    @DisplayName("GET /api/usuarios/permisos → 200")
    void listarPermisos() throws Exception {
        when(permisoService.listarPermisos()).thenReturn(List.of(
            new Permiso(1L, "GESTIONAR_USUARIOS", "Desc", "USUARIOS")
        ));
        mockMvc.perform(get("/api/usuarios/permisos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nombre").value("GESTIONAR_USUARIOS"));
    }

    @Test
    @DisplayName("GET /api/usuarios/roles/ADMIN/permisos → 200")
    void obtenerPermisosPorRol() throws Exception {
        when(permisoService.obtenerPermisosPorRol("ADMIN")).thenReturn(List.of(
            new Permiso(1L, "GESTIONAR_USUARIOS", "Desc", "USUARIOS")
        ));
        mockMvc.perform(get("/api/usuarios/roles/ADMIN/permisos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nombre").value("GESTIONAR_USUARIOS"));
    }

    @Test
    @DisplayName("PUT /api/usuarios/roles/EMPLEADO/permisos → 200")
    void asignarPermisos() throws Exception {
        mockMvc.perform(put("/api/usuarios/roles/EMPLEADO/permisos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"permisoIds\":[1,2,3]}"))
            .andExpect(status().isOk());
    }
}
