package com.perfulandia.usuarios.controller;

import tools.jackson.databind.json.JsonMapper;
import com.perfulandia.usuarios.model.dto.*;
import com.perfulandia.usuarios.model.enums.EstadoUsuario;
import com.perfulandia.usuarios.model.enums.Rol;
import com.perfulandia.usuarios.security.JwtAuthFilter;
import com.perfulandia.usuarios.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsuarioController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UsuarioController - Pruebas unitarias de endpoints")
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    @DisplayName("POST /api/auth/registro debe retornar 201")
    void registrar_DebeRetornar201() throws Exception {
        RegistroRequestDTO req = new RegistroRequestDTO("Juan", "juan@test.com", "Juan12345", "Calle 123");
        PerfilResponseDTO res = new PerfilResponseDTO(1L, "Juan", "juan@test.com",
                Rol.CLIENTE, EstadoUsuario.ACTIVO, "Calle 123", "****");

        when(usuarioService.registrarCliente(any(RegistroRequestDTO.class))).thenReturn(res);

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("juan@test.com"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
    }

    @Test
    @DisplayName("POST /api/auth/login debe retornar 200 con token")
    void login_DebeRetornarToken() throws Exception {
        LoginRequestDTO req = new LoginRequestDTO("juan@test.com", "Juan12345");
        LoginResponseDTO res = new LoginResponseDTO("fake-jwt-token", "CLIENTE");

        when(usuarioService.autenticar(any(LoginRequestDTO.class))).thenReturn(res);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake-jwt-token"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
    }

    @Test
    @DisplayName("GET /api/usuarios/perfil debe retornar 200")
    void obtenerPerfil_DebeRetornar200() throws Exception {
        PerfilResponseDTO res = new PerfilResponseDTO(1L, "Juan", "juan@test.com",
                Rol.CLIENTE, EstadoUsuario.ACTIVO, "Calle 123", "**** 1234");

        when(usuarioService.obtenerPerfil(1L)).thenReturn(res);

        mockMvc.perform(get("/api/usuarios/perfil")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("juan@test.com"));
    }

    @Test
    @DisplayName("POST /api/auth/recuperar debe retornar 200")
    void recuperarPassword_DebeRetornar200() throws Exception {
        CorreoRequestDTO req = new CorreoRequestDTO("juan@test.com");
        when(usuarioService.recuperarPassword("juan@test.com")).thenReturn("mensaje simulado");

        mockMvc.perform(post("/api/auth/recuperar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/usuarios (ADMIN) debe retornar 200 con página")
    void listarUsuarios_DebeRetornar200() throws Exception {
        Page<PerfilResponseDTO> page = new PageImpl<>(Collections.emptyList());
        when(usuarioService.listarUsuarios(any(Pageable.class), isNull(), isNull())).thenReturn(page);

        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/usuarios (ADMIN) debe retornar 201")
    void crearUsuarioAdmin_DebeRetornar201() throws Exception {
        CrearEmpleadoDTO req = new CrearEmpleadoDTO("Empleado", "emp@test.com", Rol.EMPLEADO);
        PerfilResponseDTO res = new PerfilResponseDTO(10L, "Empleado", "emp@test.com",
                Rol.EMPLEADO, EstadoUsuario.ACTIVO, null, "****");

        when(usuarioService.crearUsuarioAdmin(any(CrearEmpleadoDTO.class))).thenReturn(res);

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("emp@test.com"))
                .andExpect(jsonPath("$.rol").value("EMPLEADO"));
    }

    @Test
    @DisplayName("PUT /api/usuarios/perfil debe retornar 200")
    void actualizarPerfil_DebeRetornar200() throws Exception {
        ActualizarPerfilDTO req = new ActualizarPerfilDTO("Nuevo Nombre", "Nueva Direccion", "1234567890123456");
        PerfilResponseDTO res = new PerfilResponseDTO(1L, "Nuevo Nombre", "juan@test.com",
                Rol.CLIENTE, EstadoUsuario.ACTIVO, "Nueva Direccion", "**** 3456");

        when(usuarioService.actualizarPerfil(eq(1L), any(ActualizarPerfilDTO.class))).thenReturn(res);

        mockMvc.perform(put("/api/usuarios/perfil")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nuevo Nombre"))
                .andExpect(jsonPath("$.direccion").value("Nueva Direccion"));
    }

    @Test
    @DisplayName("POST /api/auth/logout debe retornar 200")
    void cerrarSesion_DebeRetornar200() throws Exception {
        doNothing().when(usuarioService).cerrarSesion("fake-token");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/auth/restablecer debe retornar 200")
    void restablecerPassword_DebeRetornar200() throws Exception {
        RestablecerPasswordRequestDTO req = new RestablecerPasswordRequestDTO("token123", "NuevaClave123");
        doNothing().when(usuarioService).restablecerPassword(any(RestablecerPasswordRequestDTO.class));

        mockMvc.perform(post("/api/auth/restablecer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/usuarios/{id} (ADMIN) debe retornar 200")
    void actualizarUsuarioAdmin_DebeRetornar200() throws Exception {
        ActualizarEmpleadoDTO req = new ActualizarEmpleadoDTO("Modificado", "mod@test.com", Rol.GERENTE);
        PerfilResponseDTO res = new PerfilResponseDTO(5L, "Modificado", "mod@test.com",
                Rol.GERENTE, EstadoUsuario.ACTIVO, null, "****");

        when(usuarioService.actualizarUsuarioAdmin(eq(5L), any(ActualizarEmpleadoDTO.class))).thenReturn(res);

        mockMvc.perform(put("/api/usuarios/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("mod@test.com"))
                .andExpect(jsonPath("$.rol").value("GERENTE"));
    }

    @Test
    @DisplayName("DELETE /api/usuarios/{id} (ADMIN) debe retornar 200")
    void desactivarUsuario_DebeRetornar200() throws Exception {
        doNothing().when(usuarioService).desactivarUsuario(5L);

        mockMvc.perform(delete("/api/usuarios/5"))
                .andExpect(status().isOk());
    }
}