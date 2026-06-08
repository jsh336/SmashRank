package com.smashrank.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa a un jugador/competidor de Smash Bros.
 * Almacena tanto los datos locales como la referencia al perfil de Start.gg.
 */
@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Gamertag / alias del jugador */
    @NotBlank(message = "El gamertag no puede estar vacío")
    @Column(name = "gamertag", nullable = false, unique = true, length = 100)
    private String gamertag;

    /** ID de usuario en la plataforma Start.gg */
    @Column(name = "startgg_user_id", unique = true)
    private String startGgUserId;

    /** Slug del jugador en Start.gg (ej: user/abc123) */
    @Column(name = "startgg_slug", length = 200)
    private String startGgSlug;

    /** País o región del jugador */
    @Column(name = "country", length = 100)
    private String country;

    /** Puntuación/ranking actual */
    @Builder.Default
    @Column(name = "rank_points", nullable = false)
    private Integer rankPoints = 0;

    /** Posición actual en el ranking */
    @Column(name = "rank_position")
    private Integer rankPosition;

    /** Fecha de creación del registro */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Última actualización del registro */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Resultados del jugador en torneos */
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TournamentResult> tournamentResults = new ArrayList<>();

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
