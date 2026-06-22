package com.perfulandia.usuarios.security;

import com.perfulandia.usuarios.repository.TokenInvalidadoRepository;
import com.perfulandia.usuarios.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenInvalidadoRepository tokenInvalidadoRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // Verificar si el token está en la blacklist
        if (tokenInvalidadoRepository.existsById(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (jwtService.validarToken(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            String email = jwtService.extraerEmail(token);
            Long userId = jwtService.extraerUserId(token);
            String rol = jwtService.extraerRol(token);

            var auth = new UsernamePasswordAuthenticationToken(
                    email, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Guardar userId como atributo de request para el controller
            request.setAttribute("userId", userId);
        }

        filterChain.doFilter(request, response);
    }
}