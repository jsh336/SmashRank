package com.smashrank.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de seguridad Spring Security.
 * Implementa seguridad basada en JWT sin estado (stateless).
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Permite acceso libre a endpoints de login y de consulta de rankings
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/ranking").permitAll()
                        .requestMatchers("/api/v1/ranking/players/**").permitAll()
                        
                        // Solo los administradores pueden disparar el recálculo
                        .requestMatchers("/api/v1/ranking/recalculate").hasRole("ADMIN")
                        
                        // Cualquier otro recurso requiere autenticación
                        .anyRequest().authenticated()
                )
                // Inyecta el filtro JWT antes del filtro de usuario y contraseña estándar
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
