package com.smashrank.backend.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.smashrank.backend.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Servicio encargado de la creación, validación y extracción de información de tokens JWT.
 */
@Service
public class JwtService {

    @Value("${app.jwt.secret:SmashRankSecretKeySuperGlowSecreatKeyForESportsRankProEdition2026}")
    private String secretKey;

    @Value("${app.jwt.expiration-ms:86400000}") // 24 horas de validez por defecto
    private long expirationMs;

    /**
     * Genera un token JWT para un usuario logueado en la aplicación.
     */
    public String generateToken(User user) {
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        return JWT.create()
                .withSubject(user.getStartGgUserId())
                .withClaim("name", user.getName())
                .withClaim("gamerTag", user.getGamerTag())
                .withClaim("role", user.getRole())
                .withClaim("avatarUrl", user.getAvatarUrl())
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + expirationMs))
                .sign(algorithm);
    }

    /**
     * Extrae el ID de usuario de Start.gg del token JWT.
     */
    public String getStartGgUserIdFromToken(String token) {
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        JWTVerifier verifier = JWT.require(algorithm).build();
        DecodedJWT decodedJWT = verifier.verify(token);
        return decodedJWT.getSubject();
    }

    /**
     * Valida si la firma del token es correcta y no ha expirado.
     */
    public boolean validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secretKey);
            JWTVerifier verifier = JWT.require(algorithm).build();
            verifier.verify(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
