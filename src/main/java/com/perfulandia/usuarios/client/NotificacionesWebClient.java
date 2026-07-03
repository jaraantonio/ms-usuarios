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

    public void enviarNotificacion(CorreoRequestDTO request, String token) {
        Map<String, Object> notificacionRequest = Map.of(
                "tipo", "RECUPERACION_CLAVE",
                "destinatario", request.correo(),
                "asunto", "Recuperación de Contraseña — Perfulandia SPA",
                "variables", Map.of(
                        "nombre", "Usuario",
                        "tokenRecuperacion", token
                )
        );

        try {
            webClient.post()
                    .uri("/api/notificaciones/enviar")
                    .bodyValue(notificacionRequest)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("Notificación de recuperación registrada para {}", request.correo());
        } catch (Exception e) {
            log.error("Error al registrar notificación de recuperación para {}: {}", request.correo(), e.getMessage());
        }
    }
}
