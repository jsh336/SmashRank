package com.smashrank.backend.controller;

import com.smashrank.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Controlador REST para gestionar la autenticación con Start.gg.
 * Base URL: /api/v1/auth
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * GET /api/v1/auth/login-url
     * Devuelve la URL de redirección a Start.gg para que el frontend
     * pueda redirigir al usuario al flujo de inicio de sesión.
     */
    @GetMapping("/login-url")
    public ResponseEntity<Map<String, String>> getLoginUrl() {
        log.info("GET /api/v1/auth/login-url - Solicitando URL de autorización");
        String loginUrl = authService.getAuthorizationUrl();
        return ResponseEntity.ok(Map.of("url", loginUrl));
    }

    /**
     * POST /api/v1/auth/callback
     * Recibe el código de autorización desde el frontend, realiza el flujo de OAuth2
     * en Start.gg y devuelve el JWT y datos del perfil de usuario.
     */
    @PostMapping("/callback")
    public Mono<ResponseEntity<Map<String, Object>>> handleCallback(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        log.info("POST /api/v1/auth/callback - Procesando código OAuth");

        if (code == null || code.isBlank()) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of("error", "Falta el código de autorización 'code'")));
        }

        return authService.loginWithStartGg(code)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Error en callback de autenticación: ", e);
                    return Mono.just(ResponseEntity.status(500).body(Map.of("error", e.getMessage())));
                });
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<Map<String, String>>> refreshOAuthToken(@RequestBody Map<String, String> request) {
        String startGgUserId = request.get("startGgUserId");
        if (startGgUserId == null || startGgUserId.isBlank()) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of("error", "Falta startGgUserId")));
        }

        return authService.refreshAccessToken(startGgUserId)
                .map(accessToken -> ResponseEntity.ok(Map.of("accessToken", accessToken)))
                .onErrorResume(e -> {
                    log.error("Error al refrescar token OAuth: ", e);
                    return Mono.just(ResponseEntity.status(500).body(Map.of("error", e.getMessage())));
                });
    }
}
