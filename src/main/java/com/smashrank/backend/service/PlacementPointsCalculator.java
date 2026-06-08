package com.smashrank.backend.service;

import org.springframework.stereotype.Component;

/**
 * Calcula los puntos obtenidos y los puntos máximos posibles
 * para un placement dado en un torneo con N entrants.
 *
 * Sistema de puntos estilo ranking competitivo de Smash Bros:
 *  - Los puntos base se escalan con el placement
 *  - Un multiplicador por número de participantes pondera la dificultad
 */
@Component
public class PlacementPointsCalculator {

    // ---------------------------------------------------------------
    // Puntos base por placement (sobre 100 para 1º)
    // ---------------------------------------------------------------
    private static final int[] PLACEMENT_BASE = {
            100,  // 1º
            70,   // 2º
            50,   // 3º
            35,   // 4º
            20,   // 5º-6º
            20,
            12,   // 7º-8º
            12,
            6,    // 9º-12º
            6,
            6,
            6,
            3,    // 13º-16º
            3,
            3,
            3,
            1,    // 17º-24º
            1, 1, 1, 1, 1, 1, 1
    };

    // ---------------------------------------------------------------
    // Multiplicador según número de entrants
    // ---------------------------------------------------------------
    public double getEntrantMultiplier(int totalEntrants) {
        if (totalEntrants < 16)   return 0.5;
        if (totalEntrants < 32)   return 1.0;
        if (totalEntrants < 64)   return 1.5;
        if (totalEntrants < 128)  return 2.0;
        if (totalEntrants < 256)  return 3.0;
        if (totalEntrants < 512)  return 4.0;
        return 5.0;
    }

    /**
     * Puntos ganados para un placement y número de entrants dados.
     *
     * @param placement     Posición final del jugador (1 = primero)
     * @param totalEntrants Número total de participantes en el evento
     * @return Puntos enteros obtenidos
     */
    public int calculatePoints(int placement, int totalEntrants) {
        if (placement <= 0 || totalEntrants <= 0) return 0;

        int base = getBasePts(placement);
        double multiplier = getEntrantMultiplier(totalEntrants);
        return (int) Math.round(base * multiplier);
    }

    /**
     * Puntos máximos posibles en un torneo (equivale a ganar el 1º puesto).
     *
     * @param totalEntrants Número de participantes
     * @return Puntos que habría obtenido el campeón
     */
    public int calculateMaxPoints(int totalEntrants) {
        if (totalEntrants <= 0) return 0;
        return (int) Math.round(100.0 * getEntrantMultiplier(totalEntrants));
    }

    // ---- Helper privado ----

    private int getBasePts(int placement) {
        if (placement <= PLACEMENT_BASE.length) {
            return PLACEMENT_BASE[placement - 1];
        }
        // Por debajo del 24º → 0 puntos
        return 0;
    }
}
