package com.smashrank.backend.controller;

import com.smashrank.backend.dto.PlayerRankingDTO;
import com.smashrank.backend.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para el sistema de ranking de SmashRank.
 * Base URL: /api/v1/ranking
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    /**
     * GET /api/v1/ranking
     * Devuelve el ranking calculado en tiempo real (top 15 jugadores).
     * No persiste datos; solo calcula y devuelve.
     */
    @GetMapping
    public ResponseEntity<List<PlayerRankingDTO>> getRanking() {
        log.info("GET /api/v1/ranking - solicitando ranking en tiempo real");
        return ResponseEntity.ok(rankingService.calculateRanking());
    }

    /**
     * POST /api/v1/ranking/recalculate
     * Recalcula el ranking y persiste posiciones y puntos en los jugadores.
     * Útil para llamar después de sincronizar resultados con Start.gg.
     */
    @PostMapping("/recalculate")
    public ResponseEntity<List<PlayerRankingDTO>> recalculate() {
        log.info("POST /api/v1/ranking/recalculate - recalculando y persistiendo ranking");
        return ResponseEntity.ok(rankingService.recalculateAndPersist());
    }

    /**
     * GET /api/v1/ranking/players/{playerId}
     * Devuelve las métricas de ranking individuales de un jugador concreto.
     */
    @GetMapping("/players/{playerId}")
    public ResponseEntity<PlayerRankingDTO> getPlayerScore(@PathVariable Long playerId) {
        log.info("GET /api/v1/ranking/players/{} - obteniendo puntuacion individual", playerId);
        return ResponseEntity.ok(rankingService.getPlayerScore(playerId));
    }
}
