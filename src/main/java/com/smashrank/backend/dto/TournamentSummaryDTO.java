package com.smashrank.backend.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentSummaryDTO {
    private String id;                // Start.gg tournament ID
    private String name;
    private LocalDateTime startAt;
    private int numAttendees;         // Total attendees across all events
    private int ssbUltimateEntrants;  // Entrants specifically in the SSB Ultimate event
    private String city;              // City/location
}
