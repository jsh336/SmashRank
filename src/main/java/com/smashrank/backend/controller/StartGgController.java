package com.smashrank.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.smashrank.backend.dto.TournamentResultResponseDTO;
import com.smashrank.backend.dto.TournamentSummaryDTO;
import com.smashrank.backend.dto.RegionalRankingResultDTO;
import com.smashrank.backend.service.StartGgService;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la integración con la API GraphQL de Start.gg.
 * Base URL: /api/v1/startgg
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/startgg")
@RequiredArgsConstructor
public class StartGgController {

    private final StartGgService startGgService;

    /**
     * POST /api/v1/startgg/players/{playerId}/sync
     * Sincroniza los datos de un jugador desde Start.gg usando su slug.
     * Ejemplo body: { "slug": "user/abc123" }
     */
    @PostMapping("/players/{playerId}/sync")
    public ResponseEntity<Mono<Void>> syncPlayer(
            @PathVariable Long playerId,
            @RequestBody Map<String, String> body) {

        String slug = body.get("slug");
        if (slug == null || slug.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("Sincronizando jugador {} con slug: {}", playerId, slug);
        Mono<Void> result = startGgService.syncPlayerFromStartGg(playerId, slug);
        return ResponseEntity.accepted().body(result);
    }

    /**
     * POST /api/v1/startgg/players/{playerId}/tournaments/sync
     * Sincroniza los resultados de torneos de un jugador desde Start.gg.
     */
    @PostMapping("/players/{playerId}/tournaments/sync")
    public Mono<ResponseEntity<List<TournamentResultResponseDTO>>> syncTournaments(
            @PathVariable Long playerId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int perPage) {

        log.info("Sincronizando torneos para jugador {} (página {}, {} por página)", playerId, page, perPage);
        return startGgService.syncTournamentResults(playerId, page, perPage)
                .map(ResponseEntity::ok);
    }

    /**
     * GET /api/v1/startgg/players/{playerId}/tournaments
     * Obtiene los resultados de torneos guardados localmente para un jugador.
     */
    @GetMapping("/players/{playerId}/tournaments")
    public ResponseEntity<List<TournamentResultResponseDTO>> getTournamentResults(
            @PathVariable Long playerId) {
        return ResponseEntity.ok(startGgService.getTournamentResultsByPlayer(playerId));
    }

    /**
     * POST /api/v1/startgg/graphql
     * Ejecuta una query GraphQL personalizada contra Start.gg.
     * Body: { "query": "...", "variables": { ... } }
     */
    @PostMapping("/graphql")
    public Mono<ResponseEntity<Map>> executeCustomQuery(@RequestBody Map<String, Object> body) {
        String query = (String) body.get("query");
        Object variables = body.get("variables");

        if (query == null || query.isBlank()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return startGgService.executeGraphQLQuery(query, variables)
                .map(ResponseEntity::ok);
    }

    /**
     * GET /api/v1/startgg/tournaments/region
     * Busca torneos de SSB Ultimate en una provincia española.
     * Params: region (nombre provincia en inglés), afterDate (epoch seconds), page, perPage
     */
    @GetMapping("/tournaments/region")
    public Mono<ResponseEntity<List<TournamentSummaryDTO>>> getTournamentsByRegion(
            @RequestParam String region,
            @RequestParam long afterDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "25") int perPage) {
        log.info("GET /api/v1/startgg/tournaments/region - región: {}, afterDate: {}", region, afterDate);
        return startGgService.searchTournamentsByRegion(region, afterDate, page, perPage)
                .map(ResponseEntity::ok);
    }

    /**
     * POST /api/v1/startgg/ranking/regional
     * Calcula el ranking regional con los torneos seleccionados.
     * Body: { "region": "Sevilla", "tournamentIds": ["123", "456"], "dateFrom": "2024-01-01", "dateTo": "2025-01-01" }
     */
    @PostMapping("/ranking/regional")
    public Mono<ResponseEntity<RegionalRankingResultDTO>> calculateRegionalRanking(
            @RequestBody Map<String, Object> body) {
        String region = (String) body.get("region");
        @SuppressWarnings("unchecked")
        List<String> tournamentIds = (List<String>) body.get("tournamentIds");
        String dateFrom = (String) body.getOrDefault("dateFrom", "");
        String dateTo = (String) body.getOrDefault("dateTo", "");

        if (region == null || tournamentIds == null || tournamentIds.isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        log.info("POST /api/v1/startgg/ranking/regional - región: {}, {} torneos", region, tournamentIds.size());
        return startGgService.calculateRegionalRanking(region, tournamentIds, dateFrom, dateTo)
                .map(ResponseEntity::ok);
    }
}
