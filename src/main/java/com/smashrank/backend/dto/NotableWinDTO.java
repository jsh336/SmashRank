package com.smashrank.backend.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotableWinDTO {
    private String opponentName;      // GamerTag del rival vencido
    private String startGgUserId;     // ID de Start.gg del rival
    private int opponentPlacement;    // Posición del rival en ese torneo
    private int winnerPlacement;      // Posición del ganador en ese torneo
    private String tournamentName;    // Nombre del torneo donde ocurrió
    private String eventName;         // Nombre del evento
    private boolean isUpset;          // true si fue un upset (ganó alguien con peor seed)
    private int tournamentEntrants;   // Número de participantes del torneo
}
