package com.smashrank.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad que representa a un usuario autenticado en SmashRank
 * que ha iniciado sesión utilizando su cuenta de Start.gg.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID único de usuario de la plataforma Start.gg */
    @NotBlank
    @Column(name = "startgg_user_id", nullable = false, unique = true, length = 100)
    private String startGgUserId;

    /** Nombre del perfil del usuario */
    @Column(name = "name", length = 150)
    private String name;

    /** Gamertag competitivo obtenido del jugador vinculado */
    @Column(name = "gamer_tag", length = 100)
    private String gamerTag;

    /** Email principal de la cuenta */
    @Column(name = "email", length = 150)
    private String email;

    /** Enlace a la imagen de avatar del usuario */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /** Rol de acceso en la aplicación (ej: USER, ADMIN) */
    @Builder.Default
    @Column(name = "role", nullable = false, length = 50)
    private String role = "USER";

    /** Token de acceso OAuth de Start.gg (para consultas en su nombre) */
    @Column(name = "access_token", length = 500)
    private String accessToken;

    /** Token de refresco OAuth de Start.gg */
    @Column(name = "refresh_token", length = 500)
    private String refreshToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
