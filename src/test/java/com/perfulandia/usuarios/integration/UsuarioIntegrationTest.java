package com.perfulandia.usuarios.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.perfulandia.usuarios.model.dto.RegistroRequestDTO;
import com.perfulandia.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("UsuarioIntegrationTest - Pruebas de integración")
class UsuarioIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private final ObjectMapper objectMapper = new JsonMapper();

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
    }

    @Test
    @DisplayName("Flujo completo: registro exitoso seguido de login exitoso")
    void flujoRegistroYLoginExitoso() throws Exception {
        // Given: datos de registro válidos
        RegistroRequestDTO req = new RegistroRequestDTO("Ana López", "ana@test.com", "Ana123456", "Av. Siempre Viva 742", "+56912345678");

        // When: registrar cliente
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                // Then: HTTP 201
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ana@test.com"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"));

        // Given: credenciales correctas
        String loginJson = "{\"email\":\"ana@test.com\",\"password\":\"Ana123456\"}";

        // When: login
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                // Then: HTTP 200 con rol
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
    }

    @Test
    @DisplayName("Registro con email duplicado debe fallar")
    void registroEmailDuplicado_DebeFallar() throws Exception {
        // Given: primer registro exitoso
        RegistroRequestDTO req = new RegistroRequestDTO("Pedro", "pedro@test.com", "Pedro12345", "Calle Falsa 123", "+56987654321");
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // When: intentar registrar el mismo email
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                // Then: HTTP 400
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Login con credenciales incorrectas debe fallar")
    void loginCredencialesIncorrectas_DebeFallar() throws Exception {
        // Given: usuario registrado
        RegistroRequestDTO req = new RegistroRequestDTO("Luis", "luis@test.com", "Luis12345", "Calle 456", "+56955555555");
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // When: login con contraseña incorrecta
        String loginJson = "{\"email\":\"luis@test.com\",\"password\":\"WrongPassword\"}";
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                // Then: HTTP 401
                .andExpect(status().isUnauthorized());
    }
}