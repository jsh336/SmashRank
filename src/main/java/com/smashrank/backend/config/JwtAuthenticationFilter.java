package com.smashrank.backend.config;

import com.smashrank.backend.model.User;
import com.smashrank.backend.repository.UserRepository;
import com.smashrank.backend.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro HTTP que intercepta todas las peticiones para validar y extraer el token JWT,
 * autenticando al usuario en el contexto de Spring Security.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);
        if (jwtService.validateToken(token)) {
            String startGgUserId = jwtService.getStartGgUserIdFromToken(token);

            if (startGgUserId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                userRepository.findByStartGgUserId(startGgUserId).ifPresent(user -> {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                });
            }
        }

        filterChain.doFilter(request, response);
    }
}
