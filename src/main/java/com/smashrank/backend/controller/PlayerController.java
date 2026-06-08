package com.smashrank.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.smashrank.backend.dto.PlayerRequestDTO;
import com.smashrank.backend.dto.PlayerResponseDTO;
import com.smashrank.backend.service.PlayerService;

import java.util.List;

/**
 * Controlador REST para la gestión de jugadores.
 * Base URL: /api/v1/players
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    /**
     * POST /api/v1/players
     * Crea un nuevo jugador.
     */
    @PostMapping
    public ResponseEntity<PlayerResponseDTO> createPlayer(@Valid @RequestBody PlayerRequestDTO dto) {
        log.info("POST /api/v1/players - gamertag: {}", dto.getGamertag());
        PlayerResponseDTO created = playerService.createPlayer(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * GET /api/v1/players/{id}
     * Obtiene un jugador por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PlayerResponseDTO> getPlayerById(@PathVariable Long id) {
        return ResponseEntity.ok(playerService.getPlayerById(id));
    }

    /**
     * GET /api/v1/players/gamertag/{gamertag}
     * Obtiene un jugador por su gamertag.
     */
    @GetMapping("/gamertag/{gamertag}")
    public ResponseEntity<PlayerResponseDTO> getPlayerByGamertag(@PathVariable String gamertag) {
        return ResponseEntity.ok(playerService.getPlayerByGamertag(gamertag));
    }

    /**
     * GET /api/v1/players
     * Obtiene todos los jugadores ordenados por ranking.
     * Parámetro opcional: ?search=query para buscar por gamertag.
     */
    @GetMapping
    public ResponseEntity<List<PlayerResponseDTO>> getAllPlayers(
            @RequestParam(required = false) String search) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(playerService.searchPlayers(search));
        }
        return ResponseEntity.ok(playerService.getAllPlayersByRanking());
    }

    /**
     * GET /api/v1/players/top?limit=10
     * Obtiene el top N jugadores del ranking.
     */
    @GetMapping("/top")
    public ResponseEntity<List<PlayerResponseDTO>> getTopPlayers(
            @RequestParam(defaultValue = "15") int limit) {
        return ResponseEntity.ok(playerService.getTopPlayers(limit));
    }

    /**
     * PUT /api/v1/players/{id}
     * Actualiza los datos de un jugador.
     */
    @PutMapping("/{id}")
    public ResponseEntity<PlayerResponseDTO> updatePlayer(
            @PathVariable Long id,
            @Valid @RequestBody PlayerRequestDTO dto) {
        return ResponseEntity.ok(playerService.updatePlayer(id, dto));
    }

    /**
     * PATCH /api/v1/players/{id}/rank-points?delta=50
     * Añade/resta puntos de ranking a un jugador.
     */
    @PatchMapping("/{id}/rank-points")
    public ResponseEntity<PlayerResponseDTO> updateRankPoints(
            @PathVariable Long id,
            @RequestParam int delta) {
        return ResponseEntity.ok(playerService.updateRankPoints(id, delta));
    }

    /**
     * POST /api/v1/players/recalculate-ranking
     * Recalcula las posiciones del ranking para todos los jugadores.
     */
    @PostMapping("/recalculate-ranking")
    public ResponseEntity<Void> recalculateRanking() {
        playerService.recalculateRankPositions();
        return ResponseEntity.ok().build();
    }

    /**
     * DELETE /api/v1/players/{id}
     * Elimina un jugador.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlayer(@PathVariable Long id) {
        playerService.deletePlayer(id);
        return ResponseEntity.noContent().build();
    }
}
