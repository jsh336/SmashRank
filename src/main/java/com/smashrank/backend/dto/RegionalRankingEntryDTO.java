package com.smashrank.backend.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegionalRankingEntryDTO {
    private int position;
    private String playerName;           // GamerTag de Start.gg
    private String startGgUserId;        // ID de Start.gg
    private String avatarUrl;            // Avatar URL de Start.gg
    private int totalPoints;
    private int totalPossiblePoints;
    private double efficiencyScore;
    private int tournamentsPlayed;
    private int bestPlacement;
    private List<String> tournamentsAttended;   // Nombres de los torneos jugados
    private List<NotableWinDTO> notableWins;     // Victorias destacadas
}
