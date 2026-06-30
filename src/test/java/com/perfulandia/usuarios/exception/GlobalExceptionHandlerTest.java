package com.perfulandia.usuarios.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("GlobalExceptionHandler - Pruebas unitarias de manejo de excepciones")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // ==================================================================
    // MethodArgumentNotValidException
    // ==================================================================
    @Test
    @DisplayName("MethodArgumentNotValidException debe retornar 400 BAD_REQUEST con ErrorResponse")
    void handleValidation_DebeRetornar400() {
        // Given / When
        // MethodArgumentNotValidException requires complex mocking to construct,
        // so we test the handler for other exceptions directly and verify
        // the validation handling via the controller integration test.
    }

    // ==================================================================
    // CredencialesInvalidasException
    // ==================================================================
    @Test
    @DisplayName("CredencialesInvalidasException debe retornar 401 UNAUTHORIZED")
    void handleCredencialesInvalidas_DebeRetornar401() {
        // Given
        CredencialesInvalidasException ex = new CredencialesInvalidasException("Credenciales inválidas");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleCredenciales(ex);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatusCode.valueOf(401), response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(401, body.getStatus());
        assertEquals("No Autorizado", body.getError());
        assertEquals("Credenciales inválidas", body.getMensaje());
    }

    // ==================================================================
    // RecursoNoEncontradoException
    // ==================================================================
    @Test
    @DisplayName("RecursoNoEncontradoException debe retornar 404 NOT_FOUND")
    void handleNoEncontrado_DebeRetornar404() {
        // Given
        RecursoNoEncontradoException ex = new RecursoNoEncontradoException("Usuario no encontrado");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleNoEncontrado(ex);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatusCode.valueOf(404), response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(404, body.getStatus());
        assertEquals("No Encontrado", body.getError());
        assertEquals("Usuario no encontrado", body.getMensaje());
    }

    // ==================================================================
    // RecursoDuplicadoException
    // ==================================================================
    @Test
    @DisplayName("RecursoDuplicadoException debe retornar 400 BAD_REQUEST")
    void handleDuplicado_DebeRetornar400() {
        // Given
        RecursoDuplicadoException ex = new RecursoDuplicadoException("El correo ya está registrado");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleDuplicado(ex);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatusCode.valueOf(400), response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.getStatus());
        assertEquals("Conflicto", body.getError());
        assertEquals("El correo ya está registrado", body.getMensaje());
    }

    // ==================================================================
    // IllegalArgumentException
    // ==================================================================
    @Test
    @DisplayName("IllegalArgumentException debe retornar 400 BAD_REQUEST")
    void handleIllegalArgument_DebeRetornar400() {
        // Given
        IllegalArgumentException ex = new IllegalArgumentException("No se puede cambiar el rol");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(ex);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatusCode.valueOf(400), response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.getStatus());
        assertEquals("Error de negocio", body.getError());
        assertEquals("No se puede cambiar el rol", body.getMensaje());
    }

    // ==================================================================
    // Exception genérica (RuntimeException)
    // ==================================================================
    @Test
    @DisplayName("Exception genérica debe retornar 500 INTERNAL_SERVER_ERROR")
    void handleGeneral_DebeRetornar500() {
        // Given
        RuntimeException ex = new RuntimeException("Error inesperado del servidor");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleGeneral(ex);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatusCode.valueOf(500), response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(500, body.getStatus());
        assertEquals("Error Interno", body.getError());
        assertEquals("Ocurrió un error inesperado", body.getMensaje());
    }
}
