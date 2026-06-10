package com.smashrank.backend.service;

import com.smashrank.backend.dto.GraphQLRequestDTO;
import com.smashrank.backend.dto.NotableWinDTO;
import com.smashrank.backend.dto.RegionalRankingEntryDTO;
import com.smashrank.backend.dto.RegionalRankingResultDTO;
import com.smashrank.backend.dto.TournamentResultResponseDTO;
import com.smashrank.backend.dto.TournamentSummaryDTO;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    private static final String QUERY_TOURNAMENTS_BY_REGION = """
            query getTournamentsByRegion($addrState: String!, $afterDate: Timestamp!, $page: Int!, $perPage: Int!) {
              tournaments(query: {
                page: $page
                perPage: $perPage
                filter: {
                  countryCode: "ES"
                  addrState: $addrState
                  afterDate: $afterDate
                  videogameIds: [1386]
                }
                sortBy: "startAt desc"
              }) {
                nodes {
                  id
                  name
                  startAt
                  numAttendees
                  city
                  events(filter: { videogameId: 1386 }) {
                    id
                    name
                    entrants(query: { page: 1, perPage: 1 }) {
                      pageInfo { total }
                    }
                  }
                }
              }
            }
            """;

    private static final String QUERY_TOURNAMENT_STANDINGS = """
            query getTournamentStandings($tournamentId: ID!, $perPage: Int!) {
              tournament(id: $tournamentId) {
                id
                name
                startAt
                events(filter: { videogameId: 1386 }) {
                  id
                  name
                  entrants(query: { page: 1, perPage: 1 }) {
                    pageInfo { total }
                  }
                  standings(query: { page: 1, perPage: $perPage }) {
                    nodes {
                      placement
                      entrant {
                        name
                        participants {
                          user {
                            id
                            name
                            images { url type }
                            player { gamerTag }
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

    // -----------------------------------------------------------------------
    // Métodos regionales
    // -----------------------------------------------------------------------

    /**
     * Busca torneos de Super Smash Bros. Ultimate en una región/provincia española.
     * La región debe ser el nombre de la provincia en inglés (e.g., "Seville", "Malaga").
     */
    @SuppressWarnings("unchecked")
    public Mono<List<TournamentSummaryDTO>> searchTournamentsByRegion(String region, long afterDateEpoch, int page, int perPage) {
        log.info("Buscando torneos de SSB Ultimate en region/provincia: {} desde epoch: {}", region, afterDateEpoch);

        String stateCode = mapProvinceToStateCode(region);
        
        Map<String, Object> variables = Map.of(
                "addrState", stateCode,
                "afterDate", afterDateEpoch,
                "page", 1, // Start.gg returns all tournaments in page 1 if perPage is high
                "perPage", 150
        );

        return executeGraphQLQuery(QUERY_TOURNAMENTS_BY_REGION, variables)
                .map(response -> {
                    List<TournamentSummaryDTO> result = new ArrayList<>();
                    try {
                        Map<String, Object> data = (Map<String, Object>) response.get("data");
                        if (data == null) return result;
                        Map<String, Object> tournamentsData = (Map<String, Object>) data.get("tournaments");
                        if (tournamentsData == null) return result;
                        List<Map<String, Object>> nodes = (List<Map<String, Object>>) tournamentsData.get("nodes");
                        if (nodes == null) return result;

                        for (Map<String, Object> t : nodes) {
                            String city = t.get("city") != null ? t.get("city").toString() : "";
                            String name = (String) t.get("name");
                            
                            // Filter by province city list
                            if (!isCityInProvince(city, region, name)) {
                                continue;
                            }

                            List<Map<String, Object>> events = (List<Map<String, Object>>) t.get("events");
                            int ssbEntrants = 0;
                            if (events != null && !events.isEmpty()) {
                                for (Map<String, Object> ev : events) {
                                    Map<String, Object> entrants = (Map<String, Object>) ev.get("entrants");
                                    if (entrants != null) {
                                        Map<String, Object> pageInfo = (Map<String, Object>) entrants.get("pageInfo");
                                        if (pageInfo != null && pageInfo.get("total") instanceof Number) {
                                            ssbEntrants += ((Number) pageInfo.get("total")).intValue();
                                        }
                                    }
                                }
                            }

                            String id = t.get("id") != null ? t.get("id").toString() : null;
                            if (id == null) continue;

                            TournamentSummaryDTO dto = TournamentSummaryDTO.builder()
                                    .id(id)
                                    .name(name)
                                    .startAt(parseStartAt(t.get("startAt")))
                                    .numAttendees(t.get("numAttendees") instanceof Number ? ((Number) t.get("numAttendees")).intValue() : 0)
                                    .ssbUltimateEntrants(ssbEntrants)
                                    .city(city)
                                    .build();
                            result.add(dto);
                        }
                    } catch (Exception e) {
                        log.error("Error procesando torneos de region/provincia {}: {}", region, e.getMessage(), e);
                    }
                    return result;
                });
    }

    /**
     * Calcula el ranking regional a partir de los IDs de torneos seleccionados.
     * Detecta upsets y victorias notables (victoria contra top 8 del torneo).
     */
    @SuppressWarnings("unchecked")
    public Mono<RegionalRankingResultDTO> calculateRegionalRanking(String region, List<String> tournamentIds, String dateFrom, String dateTo) {
        log.info("Calculando ranking regional para {} con {} torneos", region, tournamentIds.size());

        return Flux.fromIterable(tournamentIds)
                .flatMap(id -> executeGraphQLQuery(QUERY_TOURNAMENT_STANDINGS, Map.of("tournamentId", id, "perPage", 200)))
                .collectList()
                .map(results -> {
                    Map<String, PlayerAccumulator> playerMap = new HashMap<>();

                    for (Object rawResult : results) {
                        Map<String, Object> response = (Map<String, Object>) rawResult;
                        try {
                            Map<String, Object> data = (Map<String, Object>) response.get("data");
                            if (data == null) continue;
                            Map<String, Object> tournament = (Map<String, Object>) data.get("tournament");
                            if (tournament == null) continue;

                            String tournamentName = (String) tournament.get("name");
                            List<Map<String, Object>> events = (List<Map<String, Object>>) tournament.get("events");
                            if (events == null) continue;

                            for (Map<String, Object> event : events) {
                                String eventName = (String) event.get("name");
                                Map<String, Object> entrantsInfo = (Map<String, Object>) event.get("entrants");
                                int totalEntrants = 0;
                                if (entrantsInfo != null) {
                                    Map<String, Object> pageInfo = (Map<String, Object>) entrantsInfo.get("pageInfo");
                                    if (pageInfo != null && pageInfo.get("total") instanceof Number) {
                                        totalEntrants = ((Number) pageInfo.get("total")).intValue();
                                    }
                                }
                                if (totalEntrants == 0) continue;

                                Map<String, Object> standings = (Map<String, Object>) event.get("standings");
                                if (standings == null) continue;
                                List<Map<String, Object>> nodes = (List<Map<String, Object>>) standings.get("nodes");
                                if (nodes == null) continue;

                                // Build lookup maps for this event: userId -> placement/name/avatar
                                Map<String, Integer> placementLookup = new HashMap<>();
                                Map<String, String> nameLookup = new HashMap<>();
                                Map<String, String> avatarLookup = new HashMap<>();

                                for (Map<String, Object> node : nodes) {
                                    Integer placement = node.get("placement") instanceof Number ? ((Number) node.get("placement")).intValue() : null;
                                    if (placement == null) continue;
                                    Map<String, Object> entrant = (Map<String, Object>) node.get("entrant");
                                    if (entrant == null) continue;
                                    List<Map<String, Object>> participants = (List<Map<String, Object>>) entrant.get("participants");
                                    if (participants == null) continue;
                                    for (Map<String, Object> participant : participants) {
                                        Map<String, Object> user = (Map<String, Object>) participant.get("user");
                                        if (user == null) continue;
                                        String userId = user.get("id") != null ? user.get("id").toString() : null;
                                        if (userId == null) continue;
                                        String gamerTag = null;
                                        Map<String, Object> player = (Map<String, Object>) user.get("player");
                                        if (player != null) gamerTag = (String) player.get("gamerTag");
                                        if (gamerTag == null) gamerTag = (String) user.get("name");
                                        String avatar = null;
                                        List<Map<String, Object>> images = (List<Map<String, Object>>) user.get("images");
                                        if (images != null) {
                                            for (Map<String, Object> img : images) {
                                                if ("profile".equals(img.get("type"))) { avatar = (String) img.get("url"); break; }
                                            }
                                            if (avatar == null && !images.isEmpty()) avatar = (String) images.get(0).get("url");
                                        }
                                        placementLookup.put(userId, placement);
                                        nameLookup.put(userId, gamerTag);
                                        if (avatar != null) avatarLookup.put(userId, avatar);
                                    }
                                }

                                // Accumulate points and detect notable wins
                                int finalEntrants = totalEntrants;
                                String finalTournamentName = tournamentName;
                                String finalEventName = eventName;

                                for (Map.Entry<String, Integer> entry : placementLookup.entrySet()) {
                                    String userId = entry.getKey();
                                    int placement = entry.getValue();
                                    int points = pointsCalculator.calculatePoints(placement, finalEntrants);
                                    int maxPoints = pointsCalculator.calculateMaxPoints(finalEntrants);

                                    PlayerAccumulator acc = playerMap.computeIfAbsent(userId, k -> new PlayerAccumulator(userId, nameLookup.get(userId), avatarLookup.get(userId)));
                                    acc.totalPoints += points;
                                    acc.totalPossiblePoints += maxPoints;
                                    acc.tournamentsPlayed++;
                                    acc.tournamentsAttended.add(finalTournamentName);
                                    if (placement < acc.bestPlacement) acc.bestPlacement = placement;

                                    // Detect notable wins: beating players in top 8
                                    for (Map.Entry<String, Integer> opponentEntry : placementLookup.entrySet()) {
                                        String opponentId = opponentEntry.getKey();
                                        if (opponentId.equals(userId)) continue;
                                        int opponentPlacement = opponentEntry.getValue();
                                        // A notable win is when this player placed BETTER (lower number) than a top-8 opponent
                                        if (opponentPlacement <= 8 && placement < opponentPlacement) {
                                            boolean isUpset = placement > opponentPlacement * 2;
                                            NotableWinDTO win = NotableWinDTO.builder()
                                                    .opponentName(nameLookup.getOrDefault(opponentId, "Unknown"))
                                                    .startGgUserId(opponentId)
                                                    .opponentPlacement(opponentPlacement)
                                                    .winnerPlacement(placement)
                                                    .tournamentName(finalTournamentName)
                                                    .eventName(finalEventName)
                                                    .isUpset(isUpset)
                                                    .tournamentEntrants(finalEntrants)
                                                    .build();
                                            acc.notableWins.add(win);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.error("Error procesando standings de torneo: {}", e.getMessage(), e);
                        }
                    }

                    // Build sorted ranking list
                    List<RegionalRankingEntryDTO> rankingList = playerMap.values().stream()
                            .map(acc -> {
                                double efficiency = acc.totalPossiblePoints > 0
                                        ? Math.round((double) acc.totalPoints / acc.totalPossiblePoints * 10000.0) / 100.0
                                        : 0.0;
                                return RegionalRankingEntryDTO.builder()
                                        .playerName(acc.playerName != null ? acc.playerName : "Unknown")
                                        .startGgUserId(acc.startGgUserId)
                                        .avatarUrl(acc.avatarUrl)
                                        .totalPoints(acc.totalPoints)
                                        .totalPossiblePoints(acc.totalPossiblePoints)
                                        .efficiencyScore(efficiency)
                                        .tournamentsPlayed(acc.tournamentsPlayed)
                                        .bestPlacement(acc.bestPlacement == Integer.MAX_VALUE ? 0 : acc.bestPlacement)
                                        .tournamentsAttended(new ArrayList<>(new LinkedHashSet<>(acc.tournamentsAttended)))
                                        .notableWins(acc.notableWins)
                                        .build();
                            })
                            .filter(e -> e.getTotalPoints() > 0)
                            .sorted(java.util.Comparator.comparingInt(RegionalRankingEntryDTO::getTotalPoints).reversed()
                                    .thenComparing(java.util.Comparator.comparingDouble(RegionalRankingEntryDTO::getEfficiencyScore).reversed())
                                    .thenComparingInt(RegionalRankingEntryDTO::getTournamentsPlayed))
                            .collect(Collectors.toList());

                    // Assign positions
                    for (int i = 0; i < rankingList.size(); i++) {
                        rankingList.get(i).setPosition(i + 1);
                    }

                    return RegionalRankingResultDTO.builder()
                            .region(region)
                            .calculatedAt(LocalDateTime.now())
                            .dateFrom(dateFrom)
                            .dateTo(dateTo)
                            .totalTournaments(tournamentIds.size())
                            .totalPlayers(rankingList.size())
                            .ranking(rankingList)
                            .build();
                });
    }

    // Helper methods for regional filtering in Spain (Andalusia)
    private String mapProvinceToStateCode(String province) {
        if (province == null) return "AN";
        String norm = normalizeString(province);
        if (List.of("seville", "sevilla", "malaga", "granada", "cadiz", "cordoba", "almeria", "huelva", "jaen").contains(norm)) {
            return "AN";
        }
        if (province.length() == 2) {
            return province.toUpperCase();
        }
        return "AN";
    }

    private static boolean isCityInProvince(String city, String province, String tournamentName) {
        if (province == null) return false;
        String normProv = normalizeString(province);
        
        if (tournamentName != null) {
            String normName = normalizeString(tournamentName);
            if (normName.contains(normProv)) {
                return true;
            }
        }
        
        if (city == null || city.isBlank()) {
            return false;
        }
        
        String normCity = normalizeString(city);
        
        if (normCity.contains(normProv) || normProv.contains(normCity)) {
            return true;
        }
        
        switch (normProv) {
            case "seville":
            case "sevilla":
                return normCity.contains("sevill") || normCity.contains("sevil") || normCity.contains("doshermanas") 
                    || normCity.contains("alcaladeguadaira") || normCity.contains("utrera") || normCity.contains("mairena") 
                    || normCity.contains("carmona") || normCity.contains("tomares") || normCity.contains("bormujos") 
                    || normCity.contains("camas") || normCity.contains("lebrija");
            case "malaga":
                return normCity.contains("malag") || normCity.contains("fuengirola") || normCity.contains("marbella") 
                    || normCity.contains("estepona") || normCity.contains("benalmadena") || normCity.contains("torremolinos") 
                    || normCity.contains("mijas") || normCity.contains("velez") || normCity.contains("ronda") 
                    || normCity.contains("antequera") || normCity.contains("alhaurin") || normCity.contains("cartama");
            case "granada":
                return normCity.contains("granad") || normCity.contains("armilla") || normCity.contains("motril") 
                    || normCity.contains("almunecar") || normCity.contains("baza") || normCity.contains("loja") 
                    || normCity.contains("maracena") || normCity.contains("lasgabias") || normCity.contains("lazubia") 
                    || normCity.contains("guadix");
            case "cadiz":
                return normCity.contains("cadiz") || normCity.contains("jerez") || normCity.contains("algeciras") 
                    || normCity.contains("sanfernando") || normCity.contains("elpuerto") || normCity.contains("chiclana") 
                    || normCity.contains("sanlucar") || normCity.contains("puertoreal") || normCity.contains("rota") 
                    || normCity.contains("sanroque") || normCity.contains("barbate");
            case "cordoba":
                return normCity.contains("cordob") || normCity.contains("lucena") || normCity.contains("puentegenil") 
                    || normCity.contains("montilla") || normCity.contains("cabra") || normCity.contains("priego");
            case "almeria":
                return normCity.contains("almeri") || normCity.contains("roquetas") || normCity.contains("ejido") 
                    || normCity.contains("nijar") || normCity.contains("adra") || normCity.contains("vicar");
            case "huelva":
                return normCity.contains("huelv") || normCity.contains("lepe") || normCity.contains("almonte") 
                    || normCity.contains("moguer") || normCity.contains("islacristina") || normCity.contains("ayamonte") 
                    || normCity.contains("cartaya");
            case "jaen":
                return normCity.contains("jaen") || normCity.contains("linares") || normCity.contains("ubeda") 
                    || normCity.contains("andujar") || normCity.contains("martos") || normCity.contains("bailen") 
                    || normCity.contains("alcalalareal");
            default:
                return false;
        }
    }

    private static String normalizeString(String s) {
        if (s == null) return "";
        return s.toLowerCase()
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")
                .replace("ü", "u")
                .replace("ñ", "n")
                .replaceAll("[^a-z0-9]", "");
    }

    // -----------------------------------------------------------------------
    // Inner helper class for accumulating player stats across tournaments
    // -----------------------------------------------------------------------

    private static class PlayerAccumulator {
        String startGgUserId;
        String playerName;
        String avatarUrl;
        int totalPoints = 0;
        int totalPossiblePoints = 0;
        int tournamentsPlayed = 0;
        int bestPlacement = Integer.MAX_VALUE;
        List<String> tournamentsAttended = new ArrayList<>();
        List<NotableWinDTO> notableWins = new ArrayList<>();

        PlayerAccumulator(String id, String name, String avatar) {
            this.startGgUserId = id;
            this.playerName = name;
            this.avatarUrl = avatar;
        }
    }
}
