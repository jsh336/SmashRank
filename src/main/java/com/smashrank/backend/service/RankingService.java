package com.smashrank.backend.service;

import com.smashrank.backend.dto.PlayerRankingDTO;
import com.smashrank.backend.model.Player;
import com.smashrank.backend.model.TournamentResult;
import com.smashrank.backend.repository.PlayerRepository;
import com.smashrank.backend.repository.TournamentResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Servicio que implementa el algoritmo de ranking de SmashRank.
 *
 * ALGORITMO:
 *   Para cada jugador:
 *   1) totalPoints     = SUM(pointsEarned de todos sus TournamentResults)
 *   2) totalPossible   = SUM(maxPointsFor(entrants) de cada torneo asistido)
 *                        → "número de puntos posibles" por los que se asistió
 *   3) efficiency      = (totalPoints / totalPossible) * 100   [0..100 %]
 *
 *   Orden de clasificación (DESC):
 *     1º totalPoints       (criterio principal)
 *     2º efficiencyScore   (desempate: quien rinde más con lo que jugó)
 *     3º tournamentsPlayed ASC (menos torneos necesarios = mejor)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RankingService {

    public static final int TOP_RANKING_SIZE = 15;

    private final PlayerRepository playerRepository;
    private final TournamentResultRepository tournamentResultRepository;
    private final PlacementPointsCalculator pointsCalculator;

    // ---------------------------------------------------------------
    // API pública
    // ---------------------------------------------------------------

    /**
     * Calcula el ranking completo de todos los jugadores en tiempo real.
     *
     * @return Lista ordenada, máximo {@value TOP_RANKING_SIZE} jugadores.
     */
    public List<PlayerRankingDTO> calculateRanking() {
        log.info("Calculando ranking SmashRank para top {}", TOP_RANKING_SIZE);

        List<Player> allPlayers = playerRepository.findAll();
        List<PlayerRankingDTO> rankingList = new ArrayList<>();

        for (Player player : allPlayers) {
            List<TournamentResult> results =
                    tournamentResultRepository.findByPlayerIdOrderByDateDesc(player.getId());

            if (results.isEmpty()) continue; // Jugadores sin torneos no aparecen en el ranking

            PlayerRankingDTO dto = computePlayerScore(player, results);
            rankingList.add(dto);
        }

        // ---- Ordenar: totalPoints DESC, efficiency DESC, torneosPlayed ASC ----
        rankingList.sort(
                Comparator.comparingInt(PlayerRankingDTO::getTotalPoints).reversed()
                        .thenComparingDouble(PlayerRankingDTO::getEfficiencyScore).reversed()
                        .thenComparingInt(PlayerRankingDTO::getTournamentsPlayed)
        );

        // ---- Asignar posiciones 1..N ----
        List<PlayerRankingDTO> top = rankingList.stream()
                .limit(TOP_RANKING_SIZE)
                .toList();

        for (int i = 0; i < top.size(); i++) {
            top.get(i).setPosition(i + 1);
        }

        log.info("Ranking calculado: {} jugadores clasificados", top.size());
        return top;
    }

    /**
     * Recalcula el ranking y persiste las posiciones y puntos en la entidad Player.
     * Llama a este método después de sincronizar nuevos resultados con Start.gg.
     */
    @Transactional
    public List<PlayerRankingDTO> recalculateAndPersist() {
        List<PlayerRankingDTO> ranking = calculateRanking();

        for (PlayerRankingDTO dto : ranking) {
            playerRepository.findById(dto.getPlayerId()).ifPresent(player -> {
                player.setRankPoints(dto.getTotalPoints());
                player.setRankPosition(dto.getPosition());
                playerRepository.save(player);
            });
        }

        log.info("Ranking persistido: {} jugadores actualizados", ranking.size());
        return ranking;
    }

    /**
     * Calcula la puntuacion de un jugador concreto sin afectar al ranking global.
     *
     * @param playerId ID del jugador
     * @return DTO con sus métricas de ranking actuales
     */
    public PlayerRankingDTO getPlayerScore(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Jugador no encontrado: " + playerId));

        List<TournamentResult> results =
                tournamentResultRepository.findByPlayerIdOrderByDateDesc(playerId);

        if (results.isEmpty()) {
            return PlayerRankingDTO.builder()
                    .playerId(playerId)
                    .gamertag(player.getGamertag())
                    .country(player.getCountry())
                    .startGgSlug(player.getStartGgSlug())
                    .totalPoints(0)
                    .totalPossiblePoints(0)
                    .efficiencyScore(0.0)
                    .tournamentsPlayed(0)
                    .bestPlacement(0)
                    .position(0)
                    .build();
        }

        PlayerRankingDTO dto = computePlayerScore(player, results);
        dto.setPosition(player.getRankPosition() != null ? player.getRankPosition() : 0);
        return dto;
    }

    // ---------------------------------------------------------------
    // Lógica de cálculo interna
    // ---------------------------------------------------------------

    /**
     * Calcula las métricas de ranking para un jugador a partir de sus resultados.
     *
     * Fórmula de la sub-puntuación:
     *   totalPossible = SUM { maxPoints(entrants_i) }  para cada torneo i
     *
     *   Esto pesa cada torneo asistido por su "dificultad máxima", es decir,
     *   los puntos que habría obtenido si hubiera ganado. Un torneo con 512
     *   participantes vale mucho más que uno con 16.
     *
     *   efficiency = totalPoints / totalPossible * 100
     *   → Si un jugador gana todos sus torneos: efficiency = 100%
     *   → Si siempre queda último: efficiency ~ 0%
     */
    private PlayerRankingDTO computePlayerScore(Player player, List<TournamentResult> results) {
        int totalPoints = 0;
        int totalPossiblePoints = 0;
        int bestPlacement = Integer.MAX_VALUE;

        for (TournamentResult tr : results) {
            int earned = tr.getPointsEarned();
            int entrants = tr.getTotalEntrants() != null ? tr.getTotalEntrants() : 0;
            int maxPossible = pointsCalculator.calculateMaxPoints(entrants);

            totalPoints += earned;
            totalPossiblePoints += maxPossible;

            if (tr.getPlacement() != null && tr.getPlacement() < bestPlacement) {
                bestPlacement = tr.getPlacement();
            }
        }

        double efficiency = totalPossiblePoints > 0
                ? (double) totalPoints / totalPossiblePoints * 100.0
                : 0.0;

        // Redondear a 2 decimales
        efficiency = Math.round(efficiency * 100.0) / 100.0;

        return PlayerRankingDTO.builder()
                .playerId(player.getId())
                .gamertag(player.getGamertag())
                .country(player.getCountry())
                .startGgSlug(player.getStartGgSlug())
                .totalPoints(totalPoints)
                .totalPossiblePoints(totalPossiblePoints)
                .efficiencyScore(efficiency)
                .tournamentsPlayed(results.size())
                .bestPlacement(bestPlacement == Integer.MAX_VALUE ? 0 : bestPlacement)
                .build();
    }
}
