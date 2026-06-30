package com.perfulandia.usuarios.config;

import com.perfulandia.usuarios.model.dto.PerfilResponseDTO;
import com.perfulandia.usuarios.model.enums.EstadoUsuario;
import com.perfulandia.usuarios.model.enums.Rol;
import com.perfulandia.usuarios.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("SecurityConfig - Pruebas de seguridad de endpoints")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    // ==================================================================
    // Endpoints públicos (permitAll) — accesibles sin autenticación
    // ==================================================================

    @Test
    @DisplayName("POST /api/auth/registro debe ser accesible sin autenticación")
    void registro_SinAuth_DebeSerAccesible() throws Exception {
        // Given
        PerfilResponseDTO res = new PerfilResponseDTO(1L, "Juan", "juan@test.com",
                Rol.CLIENTE, EstadoUsuario.ACTIVO, "Calle 123", "****");
        when(usuarioService.registrarCliente(any())).thenReturn(res);

        String jsonValido = "{\"nombre\":\"Juan\",\"email\":\"juan@test.com\",\"password\":\"Juan12345\",\"direccion\":\"Calle 123\"}";

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonValido))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/auth/login debe ser accesible sin autenticación")
    void login_SinAuth_DebeSerAccesible() throws Exception {
        when(usuarioService.autenticar(any()))
                .thenReturn(new com.perfulandia.usuarios.model.dto.LoginResponseDTO("token", "CLIENTE"));

        String jsonValido = "{\"email\":\"juan@test.com\",\"password\":\"Juan12345\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonValido))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/auth/recuperar debe ser accesible sin autenticación")
    void recuperar_SinAuth_DebeSerAccesible() throws Exception {
        when(usuarioService.recuperarPassword("juan@test.com")).thenReturn("some-uuid");

        String jsonValido = "{\"correo\":\"juan@test.com\"}";

        mockMvc.perform(post("/api/auth/recuperar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonValido))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/auth/restablecer debe ser accesible sin autenticación")
    void restablecer_SinAuth_DebeSerAccesible() throws Exception {
        String jsonValido = "{\"token\":\"token123\",\"nuevaContrasena\":\"NuevaClave123\"}";

        mockMvc.perform(post("/api/auth/restablecer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonValido))
                .andExpect(status().isOk());
    }

    // ==================================================================
    // Endpoints protegidos — deben retornar 403 sin autenticación
    // (Spring Security asigna una autenticación anónima por defecto, por
    //  lo que la falta de autorización devuelve 403 Forbidden)
    // ==================================================================

    @Test
    @DisplayName("GET /api/usuarios/perfil debe retornar 403 sin autenticación")
    void perfil_SinAuth_DebeRetornar403() throws Exception {
        mockMvc.perform(get("/api/usuarios/perfil"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/usuarios/perfil debe retornar 403 sin autenticación")
    void actualizarPerfil_SinAuth_DebeRetornar403() throws Exception {
        mockMvc.perform(put("/api/usuarios/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Test\",\"direccion\":\"Test\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/auth/logout debe retornar 403 sin autenticación")
    void logout_SinAuth_DebeRetornar403() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/usuarios debe retornar 403 sin autenticación")
    void listarUsuarios_SinAuth_DebeRetornar403() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/usuarios debe retornar 403 sin autenticación")
    void crearUsuario_SinAuth_DebeRetornar403() throws Exception {
        String jsonValido = "{\"nombre\":\"Test\",\"email\":\"test@test.com\",\"rol\":\"EMPLEADO\"}";
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonValido))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/usuarios/{id} debe retornar 403 sin autenticación")
    void desactivarUsuario_SinAuth_DebeRetornar403() throws Exception {
        mockMvc.perform(delete("/api/usuarios/1"))
                .andExpect(status().isForbidden());
    }
}
