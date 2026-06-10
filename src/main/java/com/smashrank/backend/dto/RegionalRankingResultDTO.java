package com.smashrank.backend.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegionalRankingResultDTO {
    private String region;                         // Province name (e.g., "Sevilla")
    private LocalDateTime calculatedAt;
    private String dateFrom;                       // ISO date string
    private String dateTo;                         // ISO date string
    private int totalTournaments;                  // How many tournaments were included
    private int totalPlayers;                      // How many unique players appeared
    private List<RegionalRankingEntryDTO> ranking; // Ordered ranking list
}
