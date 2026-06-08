package com.smashrank.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO de respuesta con los datos de un resultado de torneo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentResultResponseDTO {

    private Long id;
    private Long playerId;
    private String playerGamertag;
    private String startGgTournamentId;
    private String tournamentName;
    private String eventName;
    private Integer placement;
    private Integer totalEntrants;
    private Integer pointsEarned;
    private LocalDateTime tournamentDate;
    private LocalDateTime syncedAt;
}
