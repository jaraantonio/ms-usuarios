package com.perfulandia.usuarios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.perfulandia.usuarios.exception.CredencialesInvalidasException;
import com.perfulandia.usuarios.exception.GlobalExceptionHandler;
import com.perfulandia.usuarios.exception.RecursoDuplicadoException;
import com.perfulandia.usuarios.exception.RecursoNoEncontradoException;
import com.perfulandia.usuarios.model.dto.*;
import com.perfulandia.usuarios.model.enums.EstadoUsuario;
import com.perfulandia.usuarios.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsuarioController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("UsuarioController - Pruebas unitarias de endpoints")
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new JsonMapper();

    @MockitoBean
    private UsuarioService usuarioService;

    // ==================================================================
    // POST /api/auth/registro
    // ==================================================================
    @Test
    @DisplayName("POST /api/auth/registro debe retornar 201")
    void registrar_DebeRetornar201() throws Exception {
        RegistroRequestDTO req = new RegistroRequestDTO("Juan", "juan@test.com", "Juan12345", "Calle 123", "+56912345678");
        PerfilResponseDTO res = new PerfilResponseDTO(1L, "Juan", "juan@test.com", null,
                "CLIENTE", EstadoUsuario.ACTIVO, "Calle 123", "****");

        when(usuarioService.registrarCliente(any(RegistroRequestDTO.class))).thenReturn(res);

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("juan@test.com"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
    }

    @Test
    @DisplayName("POST /api/auth/registro debe retornar 400 cuando datos inválidos")
    void registrar_DebeRetornar400_CuandoDatosInvalidos() throws Exception {
        // Given: objeto vacío (falla validación @NotBlank, @Email)
        String jsonInvalido = "{\"nombre\":\"\",\"email\":\"email-invalido\",\"password\":\"debil\",\"direccion\":\"\"}";

        // When / Then
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Error de Validación"));
    }

    // ==================================================================
    // POST /api/auth/login
    // ==================================================================
    @Test
    @DisplayName("POST /api/auth/login debe retornar 200 con rol")
    void login_DebeRetornarRol() throws Exception {
        LoginRequestDTO req = new LoginRequestDTO("juan@test.com", "Juan12345");
        LoginResponseDTO res = new LoginResponseDTO("CLIENTE");

        when(usuarioService.autenticar(any(LoginRequestDTO.class))).thenReturn(res);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
    }

    @Test
    @DisplayName("POST /api/auth/login debe retornar 401 cuando credenciales inválidas")
    void login_DebeRetornar401_CuandoCredencialesInvalidas() throws Exception {
        // Given
        LoginRequestDTO req = new LoginRequestDTO("juan@test.com", "WrongPass");
        when(usuarioService.autenticar(any(LoginRequestDTO.class)))
                .thenThrow(new CredencialesInvalidasException("Correo o contraseña incorrectos"));

        // When / Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("No Autorizado"));
    }

    // ==================================================================
    // POST /api/auth/logout
    // ==================================================================
    @Test
    @DisplayName("POST /api/auth/logout debe retornar 200")
    void cerrarSesion_DebeRetornar200() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk());
    }

    // ==================================================================
    // POST /api/auth/recuperar
    // ==================================================================
    @Test
    @DisplayName("POST /api/auth/recuperar debe retornar 200")
    void recuperarPassword_DebeRetornar200() throws Exception {
        CorreoRequestDTO req = new CorreoRequestDTO("juan@test.com");
        when(usuarioService.recuperarPassword("juan@test.com")).thenReturn("some-uuid");

        mockMvc.perform(post("/api/auth/recuperar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // ==================================================================
    // POST /api/auth/restablecer
    // ==================================================================
    @Test
    @DisplayName("POST /api/auth/restablecer debe retornar 200 con token válido")
    void restablecerPassword_DebeRetornar200() throws Exception {
        RestablecerPasswordRequestDTO req = new RestablecerPasswordRequestDTO("token123", "NuevaClave123");
        doNothing().when(usuarioService).restablecerPassword(any(RestablecerPasswordRequestDTO.class));

        mockMvc.perform(post("/api/auth/restablecer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/auth/restablecer debe retornar 400 cuando token inválido o expirado")
    void restablecerPassword_DebeRetornar400_CuandoTokenInvalido() throws Exception {
        // Given
        RestablecerPasswordRequestDTO req = new RestablecerPasswordRequestDTO("token-expirado", "NuevaClave123");
        doThrow(new IllegalArgumentException("El token ha expirado"))
                .when(usuarioService).restablecerPassword(any(RestablecerPasswordRequestDTO.class));

        // When / Then
        mockMvc.perform(post("/api/auth/restablecer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Error de negocio"));
    }

    // ==================================================================
    // GET /api/usuarios/perfil
    // ==================================================================
    @Test
    @DisplayName("GET /api/usuarios/{id}/perfil debe retornar 200")
    void obtenerPerfil_DebeRetornar200() throws Exception {
        PerfilResponseDTO res = new PerfilResponseDTO(1L, "Juan", "juan@test.com", null,
                "CLIENTE", EstadoUsuario.ACTIVO, "Calle 123", "**** 1234");

        when(usuarioService.obtenerPerfil(1L)).thenReturn(res);

        mockMvc.perform(get("/api/usuarios/1/perfil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("juan@test.com"));
    }

    @Test
    @DisplayName("GET /api/usuarios/{id}/perfil debe retornar 404 cuando usuario no existe")
    void obtenerPerfil_DebeRetornar404() throws Exception {
        // Given
        when(usuarioService.obtenerPerfil(99L))
                .thenThrow(new RecursoNoEncontradoException("Usuario no encontrado con ID: 99"));

        // When / Then
        mockMvc.perform(get("/api/usuarios/99/perfil"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("No Encontrado"));
    }

    // ==================================================================
    // PUT /api/usuarios/{id}/perfil
    // ==================================================================
    @Test
    @DisplayName("PUT /api/usuarios/{id}/perfil debe retornar 200")
    void actualizarPerfil_DebeRetornar200() throws Exception {
        ActualizarPerfilDTO req = new ActualizarPerfilDTO("Nuevo Nombre", "Nueva Direccion", "1234567890123456");
        PerfilResponseDTO res = new PerfilResponseDTO(1L, "Nuevo Nombre", "juan@test.com", null,
                "CLIENTE", EstadoUsuario.ACTIVO, "Nueva Direccion", "**** 3456");

        when(usuarioService.actualizarPerfil(eq(1L), any(ActualizarPerfilDTO.class))).thenReturn(res);

        mockMvc.perform(put("/api/usuarios/1/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nuevo Nombre"))
                .andExpect(jsonPath("$.direccion").value("Nueva Direccion"));
    }

    @Test
    @DisplayName("PUT /api/usuarios/{id}/perfil debe retornar 400 cuando datos inválidos")
    void actualizarPerfil_DebeRetornar400_CuandoDatosInvalidos() throws Exception {
        // Given: nombre vacío (falla @NotBlank)
        String jsonInvalido = "{\"nombre\":\"\",\"direccion\":\"\"}";

        // When / Then
        mockMvc.perform(put("/api/usuarios/1/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Error de Validación"));
    }

    // ==================================================================
    // GET /api/usuarios (ADMIN)
    // ==================================================================
    @Test
    @DisplayName("GET /api/usuarios debe retornar 200 con lista")
    void listarUsuarios_DebeRetornar200() throws Exception {
        when(usuarioService.listarUsuarios(isNull(), isNull())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk());
    }

    // ==================================================================
    // POST /api/usuarios (ADMIN)
    // ==================================================================
    @Test
    @DisplayName("POST /api/usuarios (ADMIN) debe retornar 201 con contraseña temporal")
    void crearUsuarioAdmin_DebeRetornar201() throws Exception {
        CrearEmpleadoDTO req = new CrearEmpleadoDTO("Empleado", "emp@test.com", "EMPLEADO", null);
        CrearEmpleadoResponseDTO res = new CrearEmpleadoResponseDTO(10L, "Empleado", "emp@test.com",
                "EMPLEADO", EstadoUsuario.ACTIVO, null, null, "TempPass123");

        when(usuarioService.crearUsuarioAdmin(any(CrearEmpleadoDTO.class))).thenReturn(res);

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("emp@test.com"))
                .andExpect(jsonPath("$.rol").value("EMPLEADO"))
                .andExpect(jsonPath("$.contrasenaTemporal").value("TempPass123"));
    }

    @Test
    @DisplayName("POST /api/usuarios (ADMIN) debe retornar 400 cuando email duplicado")
    void crearUsuarioAdmin_DebeRetornar400_CuandoEmailDuplicado() throws Exception {
        // Given
        CrearEmpleadoDTO req = new CrearEmpleadoDTO("Empleado", "dup@test.com", "EMPLEADO", null);
        when(usuarioService.crearUsuarioAdmin(any(CrearEmpleadoDTO.class)))
                .thenThrow(new RecursoDuplicadoException("El correo ya está registrado"));

        // When / Then
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Conflicto"));
    }

    // ==================================================================
    // PUT /api/usuarios/{id} (ADMIN)
    // ==================================================================
    @Test
    @DisplayName("PUT /api/usuarios/{id} (ADMIN) debe retornar 200")
    void actualizarUsuarioAdmin_DebeRetornar200() throws Exception {
        ActualizarEmpleadoDTO req = new ActualizarEmpleadoDTO("Modificado", "mod@test.com", "GERENTE", null);
        PerfilResponseDTO res = new PerfilResponseDTO(5L, "Modificado", "mod@test.com", null,
                "GERENTE", EstadoUsuario.ACTIVO, null, "****");

        when(usuarioService.actualizarUsuarioAdmin(eq(5L), any(ActualizarEmpleadoDTO.class))).thenReturn(res);

        mockMvc.perform(put("/api/usuarios/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("mod@test.com"))
                .andExpect(jsonPath("$.rol").value("GERENTE"));
    }

    @Test
    @DisplayName("PUT /api/usuarios/{id} (ADMIN) debe retornar 404 cuando usuario no existe")
    void actualizarUsuarioAdmin_DebeRetornar404() throws Exception {
        // Given
        ActualizarEmpleadoDTO req = new ActualizarEmpleadoDTO("Nadie", "nadie@test.com", "EMPLEADO", null);
        when(usuarioService.actualizarUsuarioAdmin(eq(99L), any(ActualizarEmpleadoDTO.class)))
                .thenThrow(new RecursoNoEncontradoException("Usuario no encontrado con ID: 99"));

        // When / Then
        mockMvc.perform(put("/api/usuarios/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("No Encontrado"));
    }

    // ==================================================================
    // DELETE /api/usuarios/{id} (ADMIN)
    // ==================================================================
    @Test
    @DisplayName("DELETE /api/usuarios/{id} (ADMIN) debe retornar 200")
    void desactivarUsuario_DebeRetornar200() throws Exception {
        doNothing().when(usuarioService).desactivarUsuario(5L);

        mockMvc.perform(delete("/api/usuarios/5"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/usuarios/{id} (ADMIN) debe retornar 400 si es ADMIN")
    void desactivarUsuario_DebeRetornar400_CuandoAdmin() throws Exception {
        // Given
        doThrow(new IllegalArgumentException("No se puede desactivar a un usuario ADMIN"))
                .when(usuarioService).desactivarUsuario(1L);

        // When / Then
        mockMvc.perform(delete("/api/usuarios/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Error de negocio"));
    }
}
