package com.smashrank.backend.dto;

import lombok.*;

/**
 * DTO de respuesta para el ranking completo de un jugador.
 * Incluye puntuación total, sub-puntuación de eficiencia y posición.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerRankingDTO {

    /** Posición en el ranking (1 = primero) */
    private int position;

    /** ID interno del jugador */
    private Long playerId;

    /** Gamertag del jugador */
    private String gamertag;

    /** País del jugador */
    private String country;

    /** Slug de Start.gg */
    private String startGgSlug;

    // ---- Métricas de ranking ----

    /** Suma total de puntos en todos los torneos */
    private int totalPoints;

    /**
     * Número de torneos asistidos ponderados por puntos posibles.
     * = Σ(maxPossiblePoints por torneo)
     * Representa la "dificultad acumulada" a la que se ha enfrentado el jugador.
     */
    private int totalPossiblePoints;

    /**
     * Sub-puntuación: totalPoints / totalPossiblePoints * 100
     * Representa la eficiencia del jugador (0-100%).
     * Se usa como criterio de desempate cuando dos jugadores tienen los mismos totalPoints.
     */
    private double efficiencyScore;

    /** Número de torneos en los que tiene resultado registrado */
    private int tournamentsPlayed;

    /** Mejor placement logrado */
    private int bestPlacement;
}
