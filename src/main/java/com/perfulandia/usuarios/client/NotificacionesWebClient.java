package com.perfulandia.usuarios.client;

import com.perfulandia.usuarios.model.dto.CorreoRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@Slf4j
public class NotificacionesWebClient {

    private final WebClient webClient;

    public NotificacionesWebClient(@Value("${ms-notificaciones.url}") String msNotificacionesUrl,
                                   WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(msNotificacionesUrl).build();
    }

    public void enviarCorreo(CorreoRequestDTO request) {
        // Construir el payload que espera el endpoint de notificaciones
        Map<String, Object> notificacionRequest = Map.of(
                "tipo", "RECUPERACION_CLAVE",
                "destinatario", request.correo(),
                "asunto", "Recuperación de Contraseña — Perfulandia SPA",
                "variables", Map.of(
                        "nombre", "Usuario",
                        "enlaceRestablecimiento", "https://perfulandia.cl/restablecer"
                )
        );

        try {
            webClient.post()
                    .uri("/api/notificaciones/enviar")
                    .bodyValue(notificacionRequest)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("Correo de recuperación enviado exitosamente a {}", request.correo());
        } catch (Exception e) {
            log.error("Error al enviar correo de recuperación a {}: {}", request.correo(), e.getMessage());
            throw new RuntimeException("No se pudo enviar la notificación a " + request.correo(), e);
        }
    }
}
