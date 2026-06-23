package com.perfulandia.usuarios;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MsUsuariosApplicationTests {

	@Test
	void contextLoads() {
		// Verifica que el contexto de Spring Boot se levanta correctamente
		// usando el perfil de prueba con H2
	}

}