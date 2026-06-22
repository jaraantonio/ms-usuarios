package com.perfulandia.usuarios.client;

import com.perfulandia.usuarios.model.dto.CorreoRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class NotificacionesWebClient {

    private final WebClient webClient;

    public NotificacionesWebClient(@Value("${ms-notificaciones.url}") String msNotificacionesUrl,
                                   WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(msNotificacionesUrl).build();
    }

    public void enviarCorreo(CorreoRequestDTO request) {
        webClient.post()
                .uri("/api/v1/notificaciones/enviar-correo")
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(response -> log.info("Correo enviado exitosamente a {}", request.correo()))
                .doOnError(error -> log.error("Error al enviar correo a {}: {}", request.correo(),
                        error.getMessage()))
                .onErrorResume(error -> {
                    log.warn("No se pudo enviar el correo, continuando con el flujo: {}", error.getMessage());
                    return Mono.empty();
                })
                .block();
    }
}