package com.smashrank.backend.service;

import com.smashrank.backend.dto.GraphQLRequestDTO;
import com.smashrank.backend.dto.TournamentResultResponseDTO;
import com.smashrank.backend.exception.ResourceNotFoundException;
import com.smashrank.backend.model.Player;
import com.smashrank.backend.model.TournamentResult;
import com.smashrank.backend.repository.PlayerRepository;
import com.smashrank.backend.repository.TournamentResultRepository;
import com.smashrank.backend.repository.UserRepository;
import com.smashrank.backend.service.AuthService;
import com.smashrank.backend.service.PlacementPointsCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Servicio para sincronizar datos de torneos desde la API GraphQL de Start.gg
 * usando WebClient (reactivo) y persistirlos en PostgreSQL.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StartGgService {

    private final WebClient startGgWebClient;
    private final PlayerRepository playerRepository;
    private final TournamentResultRepository tournamentResultRepository;
    private final PlacementPointsCalculator pointsCalculator;
    private final UserRepository userRepository;
    private final AuthService authService;

    @Value("${startgg.api.url:https://api.start.gg/gql/alpha}")
    private String startGgApiUrl;

    @Value("${startgg.country.filter:Spain}")
    private String startGgCountryFilter;

    @Value("${startgg.region.filter:Andalusia}")
    private String startGgRegionFilter;

    // -----------------------------------------------------------------------
    // Queries GraphQL predefinidas para Start.gg
    // -----------------------------------------------------------------------

    private static final String QUERY_USER_BY_SLUG = """
            query getUserBySlug($slug: String!) {
              user(slug: $slug) {
                id
                name
                gamerTag
                location {
                  country
                }
                images {
                  url
                  type
                }
              }
            }
            """;

    private static final String QUERY_PLAYER_TOURNAMENTS = """
            query getPlayerTournaments($userId: ID!, $page: Int!, $perPage: Int!, $gamerTag: String!) {
              user(id: $userId) {
                tournaments(query: {
                  page: $page
                  perPage: $perPage
                  sortBy: "startAt desc"
                }) {
                  nodes {
                    id
                    name
                    startAt
                    location {
                      country
                      region
                    }
                    events {
                      id
                      name
                      entrants(query: { page: 1 perPage: 1 }) {
                        pageInfo {
                          total
                        }
                      }
                      standings(query: { page: 1 perPage: 20 filter: { search: { fieldsToSearch: ["gamerTag"], value: $gamerTag } } }) {
                        nodes {
                          placement
                          entrant {
                            participants {
                              user {
                                id
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            """;

    // -----------------------------------------------------------------------
    // Métodos públicos
    // -----------------------------------------------------------------------

    /**
     * Realiza una query GraphQL genérica a Start.gg y devuelve la respuesta como Map.
     * Útil para queries personalizadas.
     */
    public Mono<Map> executeGraphQLQuery(String query, Object variables) {
        return executeGraphQLQuery(query, variables, null, null);
    }

    public Mono<Map> executeGraphQLQuery(String query, Object variables, String bearerToken, String startGgUserId) {
        GraphQLRequestDTO request = GraphQLRequestDTO.builder()
                .query(query)
                .variables(variables)
                .build();

        log.debug("Ejecutando query GraphQL a Start.gg");

        WebClient client = startGgWebClient;
        if (bearerToken != null && !bearerToken.isBlank()) {
            client = WebClient.builder()
                    .baseUrl(startGgApiUrl)
                    .defaultHeader("Authorization", "Bearer " + bearerToken)
                    .defaultHeader("Content-Type", "application/json")
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                    .build();
        }

        return client.post()
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorResume(WebClientResponseException.Unauthorized.class, ex -> {
                    if (bearerToken != null && startGgUserId != null && !startGgUserId.isBlank()) {
                        log.warn("Token de Start.gg expirado para usuario {}. Intentando refrescar.", startGgUserId);
                        return authService.refreshAccessToken(startGgUserId)
                                .flatMap(newAccessToken -> executeGraphQLQuery(query, variables, newAccessToken, startGgUserId));
                    }
                    return Mono.error(ex);
                })
                .doOnError(e -> log.error("Error al llamar a Start.gg GraphQL API: {}", e.getMessage()));
    }

    /**
     * Obtiene los datos de un usuario de Start.gg por su slug y sincroniza con el Player local.
     *
     * @param playerId ID local del jugador
     * @param slug     Slug de Start.gg (ej: "user/abc123")
     */
    @SuppressWarnings("unchecked")
    public Mono<Void> syncPlayerFromStartGg(Long playerId, String slug) {
        log.info("Sincronizando jugador {} desde Start.gg con slug: {}", playerId, slug);

        return executeGraphQLQuery(QUERY_USER_BY_SLUG, Map.of("slug", slug))
                .flatMap(response -> {
                    try {
                        Map<String, Object> data = (Map<String, Object>) response.get("data");
                        Map<String, Object> user = (Map<String, Object>) data.get("user");

                        if (user == null) {
                            log.warn("No se encontró el usuario en Start.gg con slug: {}", slug);
                            return Mono.empty();
                        }

                        String startGgUserId = user.get("id").toString();
                        String gamerTag = (String) user.get("gamerTag");
                        Map<String, Object> location = (Map<String, Object>) user.get("location");
                        String country = location != null ? (String) location.get("country") : null;

                        // Actualizar en base de datos
                        Player player = playerRepository.findById(playerId)
                                .orElseThrow(() -> new ResourceNotFoundException("Player", "id", playerId));

                        player.setStartGgUserId(startGgUserId);
                        player.setStartGgSlug(slug);
                        if (country != null) player.setCountry(country);

                        playerRepository.save(player);
                        log.info("Jugador {} sincronizado con Start.gg ID: {}", playerId, startGgUserId);

                    } catch (Exception e) {
                        log.error("Error procesando respuesta de Start.gg: {}", e.getMessage(), e);
                    }
                    return Mono.empty();
                });
    }

    /**
     * Sincroniza los resultados de torneos de un jugador desde Start.gg.
     *
     * @param playerId ID local del jugador
     * @param page     Página de resultados (1-indexed)
     * @param perPage  Resultados por página (máx. 25 recomendado por Start.gg)
     */
    @SuppressWarnings("unchecked")
    public Mono<List<TournamentResultResponseDTO>> syncTournamentResults(Long playerId, int page, int perPage) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player", "id", playerId));

        if (player.getStartGgUserId() == null) {
            log.warn("El jugador {} no tiene un startGgUserId configurado", playerId);
            return Mono.just(List.of());
        }

        Map<String, Object> variables = Map.of(
                "userId", player.getStartGgUserId(),
                "page", page,
                "perPage", perPage,
                "gamerTag", player.getGamertag()
        );

        String bearerToken = userRepository.findByStartGgUserId(player.getStartGgUserId())
                .map(u -> u.getAccessToken())
                .filter(token -> token != null && !token.isBlank())
                .orElse(null);

        return executeGraphQLQuery(QUERY_PLAYER_TOURNAMENTS, variables, bearerToken, player.getStartGgUserId())
                .flatMap(response -> {
                    List<TournamentResultResponseDTO> savedResults = new ArrayList<>();
                    try {
                        Map<String, Object> data = (Map<String, Object>) response.get("data");
                        Map<String, Object> userData = data != null ? (Map<String, Object>) data.get("user") : null;
                        if (userData == null) {
                            log.warn("No se encontró el usuario Start.gg para playerId {}", playerId);
                            return Mono.just(getTournamentResultsByPlayer(playerId));
                        }

                        Map<String, Object> tournaments = (Map<String, Object>) userData.get("tournaments");
                        List<Map<String, Object>> nodes = tournaments != null ? (List<Map<String, Object>>) tournaments.get("nodes") : List.of();

                        for (Map<String, Object> tournament : nodes) {
                            if (!shouldSyncTournament(tournament)) {
                                continue;
                            }

                            List<Map<String, Object>> events = (List<Map<String, Object>>) tournament.get("events");
                            if (events == null) continue;

                            for (Map<String, Object> event : events) {
                                Optional<TournamentResultResponseDTO> saved = saveTournamentResult(player, tournament, event);
                                saved.ifPresent(savedResults::add);
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error procesando resultados de Start.gg para jugador {}: {}", playerId, e.getMessage(), e);
                    }

                    log.info("Sincronización de torneos completada para jugador {}. Resultados nuevos guardados: {}", playerId, savedResults.size());
                    return Mono.just(getTournamentResultsByPlayer(playerId));
                });
    }

    private boolean shouldSyncTournament(Map<String, Object> tournament) {
        if (tournament == null) {
            return false;
        }
        Map<String, Object> location = (Map<String, Object>) tournament.get("location");
        if (location == null) {
            return false;
        }

        String country = location.get("country") != null ? location.get("country").toString() : null;
        String region = location.get("region") != null ? location.get("region").toString() : null;

        if (country != null && country.equalsIgnoreCase(startGgCountryFilter)) {
            return true;
        }
        return region != null && region.equalsIgnoreCase(startGgRegionFilter);
    }

    @SuppressWarnings("unchecked")
    private Optional<TournamentResultResponseDTO> saveTournamentResult(Player player,
                                                                      Map<String, Object> tournament,
                                                                      Map<String, Object> event) {
        String tournamentId = tournament.get("id") != null ? tournament.get("id").toString() : null;
        String tournamentName = (String) tournament.get("name");
        String eventName = (String) event.get("name");

        if (tournamentId == null || eventName == null) {
            return Optional.empty();
        }

        if (tournamentResultRepository.existsByPlayerIdAndStartGgTournamentId(player.getId(), tournamentId)) {
            return Optional.empty();
        }

        Integer totalEntrants = extractTotalEntrants(event);
        Integer placement = extractPlacement(event, player.getStartGgUserId());

        if (placement == null || placement <= 0) {
            return Optional.empty();
        }

        TournamentResult tr = TournamentResult.builder()
                .player(player)
                .startGgTournamentId(tournamentId)
                .tournamentName(tournamentName)
                .eventName(eventName)
                .placement(placement)
                .totalEntrants(totalEntrants)
                .pointsEarned(pointsCalculator.calculatePoints(placement, totalEntrants != null ? totalEntrants : 0))
                .tournamentDate(parseStartAt(tournament.get("startAt")))
                .build();

        TournamentResult saved = tournamentResultRepository.save(tr);
        return Optional.of(toResponseDTO(saved));
    }

    @SuppressWarnings("unchecked")
    private Integer extractTotalEntrants(Map<String, Object> event) {
        Map<String, Object> entrants = (Map<String, Object>) event.get("entrants");
        if (entrants == null) {
            return 0;
        }
        Map<String, Object> pageInfo = (Map<String, Object>) entrants.get("pageInfo");
        if (pageInfo == null) {
            return 0;
        }
        return pageInfo.get("total") instanceof Number ? ((Number) pageInfo.get("total")).intValue() : 0;
    }

    @SuppressWarnings("unchecked")
    private Integer extractPlacement(Map<String, Object> event, String playerStartGgUserId) {
        Map<String, Object> standings = (Map<String, Object>) event.get("standings");
        if (standings == null) {
            return null;
        }
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) standings.get("nodes");
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }

        for (Map<String, Object> node : nodes) {
            Integer placement = node.get("placement") instanceof Number ? ((Number) node.get("placement")).intValue() : null;
            Map<String, Object> entrant = (Map<String, Object>) node.get("entrant");
            if (entrant == null) {
                continue;
            }
            List<Map<String, Object>> participants = (List<Map<String, Object>>) entrant.get("participants");
            if (participants == null) {
                continue;
            }
            for (Map<String, Object> participant : participants) {
                Map<String, Object> user = (Map<String, Object>) participant.get("user");
                if (user != null && playerStartGgUserId.equals(String.valueOf(user.get("id")))) {
                    return placement;
                }
            }
        }
        return null;
    }

    private LocalDateTime parseStartAt(Object startAt) {
        if (startAt == null) {
            return null;
        }
        try {
            long timestamp = Long.parseLong(startAt.toString());
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp / 1000), ZoneOffset.UTC);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * Obtiene los resultados de torneos de un jugador desde la base de datos local.
     */
    @Transactional(readOnly = true)
    public List<TournamentResultResponseDTO> getTournamentResultsByPlayer(Long playerId) {
        if (!playerRepository.existsById(playerId)) {
            throw new ResourceNotFoundException("Player", "id", playerId);
        }
        return tournamentResultRepository.findByPlayerIdOrderByDateDesc(playerId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    // ---- Helper mapper ----

    private TournamentResultResponseDTO toResponseDTO(TournamentResult tr) {
        return TournamentResultResponseDTO.builder()
                .id(tr.getId())
                .playerId(tr.getPlayer().getId())
                .playerGamertag(tr.getPlayer().getGamertag())
                .startGgTournamentId(tr.getStartGgTournamentId())
                .tournamentName(tr.getTournamentName())
                .eventName(tr.getEventName())
                .placement(tr.getPlacement())
                .totalEntrants(tr.getTotalEntrants())
                .pointsEarned(tr.getPointsEarned())
                .tournamentDate(tr.getTournamentDate())
                .syncedAt(tr.getSyncedAt())
                .build();
    }
}
