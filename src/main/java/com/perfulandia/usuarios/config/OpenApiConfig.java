package com.perfulandia.usuarios.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración básica de OpenAPI/Swagger.
 * La UI estará disponible en {@code /swagger-ui.html} o {@code /swagger-ui/index.html}.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MS Usuarios — Perfulandia SPA")
                        .description("Microservicio de gestión de usuarios y autenticación para Perfulandia SPA")
                        .version("1.0.0"));
    }
}
