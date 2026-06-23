package com.perfulandia.usuarios.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtService - Pruebas unitarias")
class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "clave-secreta-para-test-jwt-12345678901234567890",
            3600000L);

    @Nested
    @DisplayName("generarToken")
    class GenerarTokenTests {

        @Test
        @DisplayName("Debe generar un token JWT no nulo con los claims correctos")
        void generarToken_Exito() {
            String token = jwtService.generarToken(1L, "test@test.com", "CLIENTE");

            assertNotNull(token);
            assertFalse(token.isBlank());
        }

        @Test
        @DisplayName("Debe generar tokens distintos para diferentes usuarios")
        void generarToken_TokensDistintos() {
            String token1 = jwtService.generarToken(1L, "a@test.com", "CLIENTE");
            String token2 = jwtService.generarToken(2L, "b@test.com", "EMPLEADO");

            assertNotEquals(token1, token2);
        }
    }

    @Nested
    @DisplayName("validarToken")
    class ValidarTokenTests {

        @Test
        @DisplayName("Debe validar un token recién generado")
        void validarToken_Valido() {
            String token = jwtService.generarToken(1L, "test@test.com", "CLIENTE");

            assertTrue(jwtService.validarToken(token));
        }

        @Test
        @DisplayName("Debe rechazar un token inválido o manipulado")
        void validarToken_Invalido() {
            assertFalse(jwtService.validarToken("token-falso"));
            assertFalse(jwtService.validarToken(""));
            assertFalse(jwtService.validarToken("eyJhbGciOiJIUzI1NiJ9.manipulado.firma"));
        }
    }

    @Nested
    @DisplayName("extraerClaims")
    class ExtraerClaimsTests {

        @Test
        @DisplayName("Debe extraer email, userId y rol de un token válido")
        void extraerClaims_Exito() {
            String token = jwtService.generarToken(42L, "user@test.com", "GERENTE");

            assertEquals("user@test.com", jwtService.extraerEmail(token));
            assertEquals(42L, jwtService.extraerUserId(token));
            assertEquals("GERENTE", jwtService.extraerRol(token));
        }
    }
}
