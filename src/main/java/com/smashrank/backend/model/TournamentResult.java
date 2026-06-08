package com.smashrank.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad que almacena el resultado de un jugador en un torneo específico.
 * Los datos se sincronizan desde la API GraphQL de Start.gg.
 */
@Entity
@Table(name = "tournament_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Jugador al que pertenece este resultado */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    /** ID del torneo en Start.gg */
    @NotNull
    @Column(name = "startgg_tournament_id", nullable = false)
    private String startGgTournamentId;

    /** Nombre del torneo */
    @Column(name = "tournament_name", length = 300)
    private String tournamentName;

    /** Nombre del evento dentro del torneo (ej: Singles, Doubles) */
    @Column(name = "event_name", length = 200)
    private String eventName;

    /** Posición final del jugador en el torneo */
    @Min(1)
    @Column(name = "placement")
    private Integer placement;

    /** Número de entrants totales en el evento */
    @Column(name = "total_entrants")
    private Integer totalEntrants;

    /** Puntos ganados/perdidos por este resultado */
    @Builder.Default
    @Column(name = "points_earned", nullable = false)
    private Integer pointsEarned = 0;

    /** Fecha en que se celebró el torneo */
    @Column(name = "tournament_date")
    private LocalDateTime tournamentDate;

    /** Fecha de sincronización con Start.gg */
    @Column(name = "synced_at", nullable = false, updatable = false)
    private LocalDateTime syncedAt;

    @PrePersist
    protected void onCreate() {
        this.syncedAt = LocalDateTime.now();
    }
}
