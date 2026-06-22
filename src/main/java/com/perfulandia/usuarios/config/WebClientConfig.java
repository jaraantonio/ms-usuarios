package com.perfulandia.usuarios.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuración que provee un bean {@link WebClient.Builder} incluso en
 * entornos Servlet (no reactivos). En Spring Boot 4.x,
 * {@code WebFluxAutoConfiguration} solo se activa con aplicaciones REACTIVE,
 * por lo que este bean debe definirse manualmente para que
 * {@code NotificacionesWebClient} pueda inyectarlo.
 */
@Configuration
public class WebClientConfig {

    @Bean
    WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
