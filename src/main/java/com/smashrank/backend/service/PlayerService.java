package com.smashrank.backend.service;

import com.smashrank.backend.dto.PlayerRequestDTO;
import com.smashrank.backend.dto.PlayerResponseDTO;
import com.smashrank.backend.exception.DuplicateResourceException;
import com.smashrank.backend.exception.ResourceNotFoundException;
import com.smashrank.backend.model.Player;
import com.smashrank.backend.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio para la gestión de jugadores.
 * Contiene la lógica de negocio relacionada con Players.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PlayerService {

    private final PlayerRepository playerRepository;

    /**
     * Crea un nuevo jugador.
     */
    public PlayerResponseDTO createPlayer(PlayerRequestDTO dto) {
        log.info("Creando jugador con gamertag: {}", dto.getGamertag());

        if (playerRepository.existsByGamertag(dto.getGamertag())) {
            throw new DuplicateResourceException(
                    "Ya existe un jugador con el gamertag: " + dto.getGamertag()
            );
        }

        Player player = Player.builder()
                .gamertag(dto.getGamertag())
                .startGgUserId(dto.getStartGgUserId())
                .startGgSlug(dto.getStartGgSlug())
                .country(dto.getCountry())
                .rankPoints(0)
                .build();

        Player saved = playerRepository.save(player);
        log.info("Jugador creado con ID: {}", saved.getId());
        return toResponseDTO(saved);
    }

    /**
     * Obtiene un jugador por su ID.
     */
    @Transactional(readOnly = true)
    public PlayerResponseDTO getPlayerById(Long id) {
        Player player = findPlayerOrThrow(id);
        return toResponseDTO(player);
    }

    /**
     * Obtiene un jugador por su gamertag.
     */
    @Transactional(readOnly = true)
    public PlayerResponseDTO getPlayerByGamertag(String gamertag) {
        Player player = playerRepository.findByGamertag(gamertag)
                .orElseThrow(() -> new ResourceNotFoundException("Player", "gamertag", gamertag));
        return toResponseDTO(player);
    }

    /**
     * Obtiene todos los jugadores ordenados por ranking.
     */
    @Transactional(readOnly = true)
    public List<PlayerResponseDTO> getAllPlayersByRanking() {
        return playerRepository.findAllOrderByRankPointsDesc()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /**
     * Obtiene el top N jugadores del ranking.
     */
    @Transactional(readOnly = true)
    public List<PlayerResponseDTO> getTopPlayers(int limit) {
        return playerRepository.findTopPlayers(PageRequest.of(0, Math.max(1, limit)))
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /**
     * Busca jugadores por gamertag (búsqueda parcial).
     */
    @Transactional(readOnly = true)
    public List<PlayerResponseDTO> searchPlayers(String query) {
        return playerRepository.searchByGamertag(query)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /**
     * Actualiza los datos de un jugador.
     */
    public PlayerResponseDTO updatePlayer(Long id, PlayerRequestDTO dto) {
        Player player = findPlayerOrThrow(id);

        // Verificar que el nuevo gamertag no esté en uso por otro jugador
        if (!player.getGamertag().equals(dto.getGamertag())
                && playerRepository.existsByGamertag(dto.getGamertag())) {
            throw new DuplicateResourceException(
                    "Ya existe un jugador con el gamertag: " + dto.getGamertag()
            );
        }

        player.setGamertag(dto.getGamertag());
        player.setStartGgUserId(dto.getStartGgUserId());
        player.setStartGgSlug(dto.getStartGgSlug());
        player.setCountry(dto.getCountry());

        Player updated = playerRepository.save(player);
        log.info("Jugador actualizado: {}", updated.getId());
        return toResponseDTO(updated);
    }

    /**
     * Actualiza los puntos de ranking de un jugador.
     */
    public PlayerResponseDTO updateRankPoints(Long id, int points) {
        Player player = findPlayerOrThrow(id);
        player.setRankPoints(player.getRankPoints() + points);
        Player updated = playerRepository.save(player);
        log.info("Puntos de {} actualizados a {}", updated.getGamertag(), updated.getRankPoints());
        return toResponseDTO(updated);
    }

    /**
     * Recalcula y actualiza las posiciones del ranking para todos los jugadores.
     */
    public void recalculateRankPositions() {
        log.info("Recalculando posiciones del ranking...");
        List<Player> players = playerRepository.findAllOrderByRankPointsDesc();
        for (int i = 0; i < players.size(); i++) {
            players.get(i).setRankPosition(i + 1);
        }
        playerRepository.saveAll(players);
        log.info("Ranking recalculado para {} jugadores", players.size());
    }

    /**
     * Elimina un jugador por ID.
     */
    public void deletePlayer(Long id) {
        Player player = findPlayerOrThrow(id);
        playerRepository.delete(player);
        log.info("Jugador eliminado: {}", id);
    }

    // ---- Helpers ----

    private Player findPlayerOrThrow(Long id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Player", "id", id));
    }

    private PlayerResponseDTO toResponseDTO(Player player) {
        return PlayerResponseDTO.builder()
                .id(player.getId())
                .gamertag(player.getGamertag())
                .startGgUserId(player.getStartGgUserId())
                .startGgSlug(player.getStartGgSlug())
                .country(player.getCountry())
                .rankPoints(player.getRankPoints())
                .rankPosition(player.getRankPosition())
                .createdAt(player.getCreatedAt())
                .updatedAt(player.getUpdatedAt())
                .build();
    }
}
