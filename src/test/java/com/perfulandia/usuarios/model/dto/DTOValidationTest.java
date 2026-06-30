package com.perfulandia.usuarios.model.dto;

import com.perfulandia.usuarios.model.enums.Rol;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("DTO - Pruebas de validación con Jakarta Validation")
class DTOValidationTest {

    private static Validator validator;
    private static ValidatorFactory factory;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        if (factory != null) {
            factory.close();
        }
    }

    // ==================================================================
    // RegistroRequestDTO
    // ==================================================================
    @Test
    @DisplayName("RegistroRequestDTO con datos válidos no debe tener violaciones")
    void registroRequestDTO_Valido() {
        // Given
        RegistroRequestDTO dto = new RegistroRequestDTO(
                "Juan Pérez", "juan@test.com", "Juan12345", "Av. Siempre Viva 742");

        // When
        Set<ConstraintViolation<RegistroRequestDTO>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("RegistroRequestDTO con nombre vacío debe tener violación")
    void registroRequestDTO_NombreVacio() {
        // Given
        RegistroRequestDTO dto = new RegistroRequestDTO(
                "", "juan@test.com", "Juan12345", "Dirección");

        // When
        Set<ConstraintViolation<RegistroRequestDTO>> violations = validator.validate(dto);

        // Then
        assertEquals(1, violations.size());
        assertEquals("El nombre es obligatorio", violations.iterator().next().getMessage());
    }

    @Test
    @DisplayName("RegistroRequestDTO con email inválido debe tener violación")
    void registroRequestDTO_EmailInvalido() {
        // Given
        RegistroRequestDTO dto = new RegistroRequestDTO(
                "Juan", "email-invalido", "Juan12345", "Dirección");

        // When
        Set<ConstraintViolation<RegistroRequestDTO>> violations = validator.validate(dto);

        // Then
        assertEquals(1, violations.size());
        assertEquals("Formato de correo inválido", violations.iterator().next().getMessage());
    }

    @Test
    @DisplayName("RegistroRequestDTO con password sin mayúscula debe tener violación")
    void registroRequestDTO_PasswordSinMayuscula() {
        // Given
        RegistroRequestDTO dto = new RegistroRequestDTO(
                "Juan", "juan@test.com", "solominusculasynumeros123", "Dirección");

        // When
        Set<ConstraintViolation<RegistroRequestDTO>> violations = validator.validate(dto);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("mayúscula"));
    }

    @Test
    @DisplayName("RegistroRequestDTO con password sin número debe tener violación")
    void registroRequestDTO_PasswordSinNumero() {
        // Given
        RegistroRequestDTO dto = new RegistroRequestDTO(
                "Juan", "juan@test.com", "SoloLetrasSinNumeros", "Dirección");

        // When
        Set<ConstraintViolation<RegistroRequestDTO>> violations = validator.validate(dto);

        // Then
        assertEquals(1, violations.size());
    }

    @Test
    @DisplayName("RegistroRequestDTO con password muy corta debe tener violación")
    void registroRequestDTO_PasswordMuyCorta() {
        // Given
        RegistroRequestDTO dto = new RegistroRequestDTO(
                "Juan", "juan@test.com", "Ab1", "Dirección");

        // When
        Set<ConstraintViolation<RegistroRequestDTO>> violations = validator.validate(dto);

        // Then
        assertEquals(1, violations.size());
    }

    @Test
    @DisplayName("RegistroRequestDTO con dirección vacía debe tener violación")
    void registroRequestDTO_DireccionVacia() {
        // Given
        RegistroRequestDTO dto = new RegistroRequestDTO(
                "Juan", "juan@test.com", "Juan12345", "");

        // When
        Set<ConstraintViolation<RegistroRequestDTO>> violations = validator.validate(dto);

        // Then
        assertEquals(1, violations.size());
        assertEquals("La dirección de envío es obligatoria", violations.iterator().next().getMessage());
    }

    // ==================================================================
    // LoginRequestDTO
    // ==================================================================
    @Test
    @DisplayName("LoginRequestDTO con datos válidos no debe tener violaciones")
    void loginRequestDTO_Valido() {
        // Given
        LoginRequestDTO dto = new LoginRequestDTO("juan@test.com", "miPassword");

        // When
        Set<ConstraintViolation<LoginRequestDTO>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("LoginRequestDTO con email vacío debe tener violación")
    void loginRequestDTO_EmailVacio() {
        // Given
        LoginRequestDTO dto = new LoginRequestDTO("", "password");

        // When
        Set<ConstraintViolation<LoginRequestDTO>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("correo es obligatorio")));
    }

    @Test
    @DisplayName("LoginRequestDTO con email inválido debe tener violación")
    void loginRequestDTO_EmailInvalido() {
        // Given
        LoginRequestDTO dto = new LoginRequestDTO("invalido", "password");

        // When
        Set<ConstraintViolation<LoginRequestDTO>> violations = validator.validate(dto);

        // Then
        assertEquals(1, violations.size());
        assertEquals("Formato de correo inválido", violations.iterator().next().getMessage());
    }

    @Test
    @DisplayName("LoginRequestDTO con password vacío debe tener violación")
    void loginRequestDTO_PasswordVacia() {
        // Given
        LoginRequestDTO dto = new LoginRequestDTO("juan@test.com", "");

        // When
        Set<ConstraintViolation<LoginRequestDTO>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("contraseña es obligatoria")));
    }

    // ==================================================================
    // ActualizarPerfilDTO
    // ==================================================================
    @Test
    @DisplayName("ActualizarPerfilDTO con datos válidos no debe tener violaciones")
    void actualizarPerfilDTO_Valido() {
        // Given
        ActualizarPerfilDTO dto = new ActualizarPerfilDTO("Juan", "Dirección", "1234567890123456");

        // When
        Set<ConstraintViolation<ActualizarPerfilDTO>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("ActualizarPerfilDTO con nombre vacío debe tener violación")
    void actualizarPerfilDTO_NombreVacio() {
        // Given
        ActualizarPerfilDTO dto = new ActualizarPerfilDTO("", "Dirección", null);

        // When
        Set<ConstraintViolation<ActualizarPerfilDTO>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.stream().anyMatch(v -> "El nombre es obligatorio".equals(v.getMessage())));
    }

    @Test
    @DisplayName("ActualizarPerfilDTO con nombre muy corto debe tener violación")
    void actualizarPerfilDTO_NombreMuyCorto() {
        // Given
        ActualizarPerfilDTO dto = new ActualizarPerfilDTO("AB", "Dirección", null);

        // When
        Set<ConstraintViolation<ActualizarPerfilDTO>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("entre 3 y 100")));
    }

    @Test
    @DisplayName("ActualizarPerfilDTO con dirección vacía debe tener violación")
    void actualizarPerfilDTO_DireccionVacia() {
        // Given
        ActualizarPerfilDTO dto = new ActualizarPerfilDTO("Juan", "", null);

        // When
        Set<ConstraintViolation<ActualizarPerfilDTO>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("dirección es obligatoria")));
    }

    // ==================================================================
    // CorreoRequestDTO
    // ==================================================================
    @Test
    @DisplayName("CorreoRequestDTO con datos válidos no debe tener violaciones")
    void correoRequestDTO_Valido() {
        // Given
        CorreoRequestDTO dto = new CorreoRequestDTO("juan@test.com");

        // When
        Set<ConstraintViolation<CorreoRequestDTO>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("CorreoRequestDTO con correo vacío debe tener violación")
    void correoRequestDTO_CorreoVacio() {
        // Given
        CorreoRequestDTO dto = new CorreoRequestDTO("");

        // When
        Set<ConstraintViolation<CorreoRequestDTO>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("correo es obligatorio")));
    }

    @Test
    @DisplayName("CorreoRequestDTO con correo inválido debe tener violación")
    void correoRequestDTO_CorreoInvalido() {
        // Given
        CorreoRequestDTO dto = new CorreoRequestDTO("no-es-email");

        // When
        Set<ConstraintViolation<CorreoRequestDTO>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Formato de correo")));
    }

    // ==================================================================
    // RestablecerPasswordRequestDTO
    // ==================================================================
    @Test
    @DisplayName("RestablecerPasswordRequestDTO con datos válidos no debe tener violaciones")
    void restablecerPasswordDTO_Valido() {
        // Given
        RestablecerPasswordRequestDTO dto = new RestablecerPasswordRequestDTO("token123", "NuevaClave123");

        // When
        Set<ConstraintViolation<RestablecerPasswordRequestDTO>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("RestablecerPasswordRequestDTO con token vacío debe tener violación")
    void restablecerPasswordDTO_TokenVacio() {
        // Given
        RestablecerPasswordRequestDTO dto = new RestablecerPasswordRequestDTO("", "NuevaClave123");

        // When
        Set<ConstraintViolation<RestablecerPasswordRequestDTO>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("token es obligatorio")));
    }

    @Test
    @DisplayName("RestablecerPasswordRequestDTO con contraseña sin requisitos debe tener violación")
    void restablecerPasswordDTO_PasswordInvalida() {
        // Given
        RestablecerPasswordRequestDTO dto = new RestablecerPasswordRequestDTO("token123", "solo-minusculas");

        // When
        Set<ConstraintViolation<RestablecerPasswordRequestDTO>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("mayuscula")));
    }

    // ==================================================================
    // CrearEmpleadoDTO
    // ==================================================================
    @Test
    @DisplayName("CrearEmpleadoDTO con datos válidos no debe tener violaciones")
    void crearEmpleadoDTO_Valido() {
        // Given
        CrearEmpleadoDTO dto = new CrearEmpleadoDTO("Empleado", "emp@test.com", Rol.EMPLEADO, null);

        // When
        Set<ConstraintViolation<CrearEmpleadoDTO>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("CrearEmpleadoDTO con nombre vacío debe tener violación")
    void crearEmpleadoDTO_NombreVacio() {
        // Given
        CrearEmpleadoDTO dto = new CrearEmpleadoDTO("", "emp@test.com", Rol.EMPLEADO, null);

        // When
        Set<ConstraintViolation<CrearEmpleadoDTO>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("nombre es obligatorio")));
    }

    @Test
    @DisplayName("CrearEmpleadoDTO con email inválido debe tener violación")
    void crearEmpleadoDTO_EmailInvalido() {
        // Given
        CrearEmpleadoDTO dto = new CrearEmpleadoDTO("Emp", "invalido", Rol.EMPLEADO, null);

        // When
        Set<ConstraintViolation<CrearEmpleadoDTO>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Formato de correo")));
    }

    @Test
    @DisplayName("CrearEmpleadoDTO con rol nulo debe tener violación")
    void crearEmpleadoDTO_RolNulo() {
        // Given
        CrearEmpleadoDTO dto = new CrearEmpleadoDTO("Emp", "emp@test.com", null, null);

        // When
        Set<ConstraintViolation<CrearEmpleadoDTO>> violations = validator.validate(dto);

        // Then
        assertEquals(1, violations.size());
        assertEquals("El rol es obligatorio", violations.iterator().next().getMessage());
    }

    // ==================================================================
    // ActualizarEmpleadoDTO
    // ==================================================================
    @Test
    @DisplayName("ActualizarEmpleadoDTO con datos válidos no debe tener violaciones")
    void actualizarEmpleadoDTO_Valido() {
        // Given
        ActualizarEmpleadoDTO dto = new ActualizarEmpleadoDTO("Emp", "emp@test.com", Rol.GERENTE, null);

        // When
        Set<ConstraintViolation<ActualizarEmpleadoDTO>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("ActualizarEmpleadoDTO con nombre vacío debe tener violación")
    void actualizarEmpleadoDTO_NombreVacio() {
        // Given
        ActualizarEmpleadoDTO dto = new ActualizarEmpleadoDTO("", "emp@test.com", Rol.EMPLEADO, null);

        // When
        Set<ConstraintViolation<ActualizarEmpleadoDTO>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("nombre es obligatorio")));
    }

    @Test
    @DisplayName("ActualizarEmpleadoDTO con nombre muy corto debe tener violación")
    void actualizarEmpleadoDTO_NombreMuyCorto() {
        // Given
        ActualizarEmpleadoDTO dto = new ActualizarEmpleadoDTO("AB", "emp@test.com", Rol.EMPLEADO, null);

        // When
        Set<ConstraintViolation<ActualizarEmpleadoDTO>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("entre 3 y 100")));
    }

    @Test
    @DisplayName("ActualizarEmpleadoDTO con email inválido debe tener violación")
    void actualizarEmpleadoDTO_EmailInvalido() {
        // Given
        ActualizarEmpleadoDTO dto = new ActualizarEmpleadoDTO("Emp", "invalido", Rol.EMPLEADO, null);

        // When
        Set<ConstraintViolation<ActualizarEmpleadoDTO>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Formato de correo")));
    }

    @Test
    @DisplayName("ActualizarEmpleadoDTO con rol nulo debe tener violación")
    void actualizarEmpleadoDTO_RolNulo() {
        // Given
        ActualizarEmpleadoDTO dto = new ActualizarEmpleadoDTO("Emp", "emp@test.com", null, null);

        // When
        Set<ConstraintViolation<ActualizarEmpleadoDTO>> violations = validator.validate(dto);

        // Then
        assertEquals(1, violations.size());
        assertEquals("El rol es obligatorio", violations.iterator().next().getMessage());
    }
}
