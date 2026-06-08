package com.smashrank.backend.service;

import com.smashrank.backend.model.Player;
import com.smashrank.backend.model.User;
import com.smashrank.backend.repository.PlayerRepository;
import com.smashrank.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Servicio encargado del flujo de autenticación OAuth2 con Start.gg,
 * el mapeo al modelo de base de datos local y la emisión del JWT.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PlayerRepository playerRepository;
    private final JwtService jwtService;

    @Value("${startgg.client-id:placeholder_client_id}")
    private String clientId;

    @Value("${startgg.client-secret:placeholder_client_secret}")
    private String clientSecret;

    @Value("${startgg.redirect-uri:http://localhost:4200/auth/callback}")
    private String redirectUri;

    @Value("${startgg.api.url:https://api.start.gg/gql/alpha}")
    private String startGgApiUrl;

    private final WebClient.Builder webClientBuilder = WebClient.builder();

    /**
     * Construye la URL de redirección a Start.gg para que el usuario inicie sesión.
     */
    public String getAuthorizationUrl() {
        if ("placeholder_client_id".equals(clientId) || clientId == null || clientId.isBlank()) {
            log.info("Usando client-id de desarrollo. Redirigiendo directamente al callback mock local.");
            return UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("code", "mock_auth_code_for_offline_testing")
                    .build()
                    .toUriString();
        }
        return UriComponentsBuilder.fromHttpUrl("https://start.gg/oauth/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("scope", "user.identity user.email")
                .queryParam("redirect_uri", redirectUri)
                .build()
                .toUriString();
    }

    /**
     * Completa el login: intercambia el código de autorización por el Access Token,
     * consulta GraphQL para obtener los datos del usuario actual, guarda/actualiza
     * el usuario local en BBDD y genera el JWT de SmashRank.
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> loginWithStartGg(String code) {
        if ("mock_auth_code_for_offline_testing".equals(code) || "placeholder_client_id".equals(clientId)) {
            log.info("Entorno local sin credenciales de Start.gg. Iniciando sesión de desarrollo mock.");
            
            User user = userRepository.findByStartGgUserId("mock_user_123456")
                    .orElse(User.builder().startGgUserId("mock_user_123456").build());
            
            user.setName("Jose Developer");
            user.setGamerTag("JoseDev");
            user.setEmail("jose@smashrank.gg");
            user.setAvatarUrl("");
            user.setAccessToken("mock_access_token");
            user.setRefreshToken("mock_refresh_token");
            
            if (user.getId() == null && userRepository.count() == 0) {
                user.setRole("ADMIN");
            } else if (user.getId() == null) {
                user.setRole("USER");
            }
            
            userRepository.save(user);
            linkUserToPlayer(user);
            
            String localJwt = jwtService.generateToken(user);
            
            return Mono.just(Map.of(
                    "token", localJwt,
                    "user", Map.of(
                            "id", user.getId(),
                            "startGgUserId", user.getStartGgUserId(),
                            "name", user.getName() != null ? user.getName() : "",
                            "gamerTag", user.getGamerTag() != null ? user.getGamerTag() : "",
                            "email", user.getEmail() != null ? user.getEmail() : "",
                            "avatarUrl", "",
                            "role", user.getRole()
                    )
            ));
        }

        log.info("Intercambiando código de autorización con Start.gg");

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("code", code);
        formData.add("redirect_uri", redirectUri);

        return webClientBuilder.build().post()
                .uri("https://api.start.gg/oauth/access_token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(tokenResponse -> {
                    String accessToken = (String) tokenResponse.get("access_token");
                    String refreshToken = (String) tokenResponse.get("refresh_token");

                    if (accessToken == null) {
                        return Mono.error(new RuntimeException("No se recibió el access_token de Start.gg"));
                    }

                    // Query GraphQL para obtener los datos de la cuenta logueada
                    String query = """
                            query getCurrentUser {
                              currentUser {
                                id
                                name
                                slug
                                player {
                                  gamerTag
                                }
                                email
                                images {
                                  url
                                  type
                                }
                              }
                            }
                            """;

                    Map<String, Object> gqlRequest = Map.of("query", query);

                    log.info("Obteniendo datos de currentUser de la API GraphQL de Start.gg");

                    return webClientBuilder.baseUrl(startGgApiUrl).build().post()
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(gqlRequest)
                            .retrieve()
                            .bodyToMono(Map.class)
                            .map(gqlResponse -> {
                                Map<String, Object> data = (Map<String, Object>) gqlResponse.get("data");
                                Map<String, Object> currentUser = (Map<String, Object>) data.get("currentUser");

                                if (currentUser == null) {
                                    throw new RuntimeException("No se pudieron obtener los detalles del usuario actual de Start.gg");
                                }

                                String startGgUserId = currentUser.get("id").toString();
                                String name = (String) currentUser.get("name");
                                String slug = (String) currentUser.get("slug");
                                String email = (String) currentUser.get("email");

                                String gamerTag = name;
                                Map<String, Object> playerMap = (Map<String, Object>) currentUser.get("player");
                                if (playerMap != null && playerMap.get("gamerTag") != null) {
                                    gamerTag = (String) playerMap.get("gamerTag");
                                }

                                String avatarUrl = null;
                                List<Map<String, Object>> images = (List<Map<String, Object>>) currentUser.get("images");
                                if (images != null) {
                                    avatarUrl = images.stream()
                                            .filter(img -> "profile".equals(img.get("type")))
                                            .map(img -> (String) img.get("url"))
                                            .findFirst()
                                            .orElse(null);
                                    if (avatarUrl == null && !images.isEmpty()) {
                                        avatarUrl = (String) images.get(0).get("url");
                                    }
                                }

                                // Guardar o actualizar registro de usuario
                                User user = userRepository.findByStartGgUserId(startGgUserId)
                                        .orElse(User.builder().startGgUserId(startGgUserId).build());

                                user.setName(name);
                                user.setGamerTag(gamerTag);
                                user.setEmail(email);
                                user.setAvatarUrl(avatarUrl);
                                user.setAccessToken(accessToken);
                                user.setRefreshToken(refreshToken);

                                // Si es el primer usuario en registrarse, le otorgamos ADMIN, si no USER
                                if (user.getId() == null && userRepository.count() == 0) {
                                    user.setRole("ADMIN");
                                } else if (user.getId() == null) {
                                    user.setRole("USER");
                                }

                                userRepository.save(user);

                                // Intentar vincular usuario a un Player competitivo del ranking por tag o por ID
                                linkUserToPlayer(user);

                                // Generar token JWT local
                                String localJwt = jwtService.generateToken(user);

                                return Map.of(
                                        "token", localJwt,
                                        "user", Map.of(
                                                "id", user.getId(),
                                                "startGgUserId", user.getStartGgUserId(),
                                                "name", user.getName() != null ? user.getName() : "",
                                                "gamerTag", user.getGamerTag() != null ? user.getGamerTag() : "",
                                                "email", user.getEmail() != null ? user.getEmail() : "",
                                                "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                                                "role", user.getRole()
                                        )
                                );
                            });
                });
    }

    /**
     * Refresca el token de acceso de un usuario mediante el refresh_token de Start.gg.
     */
    @SuppressWarnings("unchecked")
    public Mono<String> refreshAccessToken(String startGgUserId) {
        return Mono.justOrEmpty(userRepository.findByStartGgUserId(startGgUserId))
                .switchIfEmpty(Mono.error(new RuntimeException("Usuario no encontrado para refrescar token: " + startGgUserId)))
                .flatMap(user -> {
                    if (user.getRefreshToken() == null || user.getRefreshToken().isBlank()) {
                        return Mono.error(new RuntimeException("No hay refresh_token disponible para el usuario Start.gg " + startGgUserId));
                    }

                    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
                    formData.add("grant_type", "refresh_token");
                    formData.add("client_id", clientId);
                    formData.add("client_secret", clientSecret);
                    formData.add("refresh_token", user.getRefreshToken());

                    return webClientBuilder.build().post()
                            .uri("https://api.start.gg/oauth/access_token")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .body(BodyInserters.fromFormData(formData))
                            .retrieve()
                            .bodyToMono(Map.class)
                            .flatMap(tokenResponse -> {
                                String refreshedAccessToken = (String) tokenResponse.get("access_token");
                                String refreshedRefreshToken = (String) tokenResponse.get("refresh_token");
                                if (refreshedAccessToken == null) {
                                    return Mono.error(new RuntimeException("No se recibió access_token al refrescar token de Start.gg"));
                                }

                                user.setAccessToken(refreshedAccessToken);
                                if (refreshedRefreshToken != null && !refreshedRefreshToken.isBlank()) {
                                    user.setRefreshToken(refreshedRefreshToken);
                                }
                                userRepository.save(user);
                                return Mono.just(refreshedAccessToken);
                            })
                            .onErrorResume(WebClientResponseException.class, ex -> {
                                log.error("Error al refrescar token de Start.gg para usuario {}: {}", startGgUserId, ex.getMessage());
                                return Mono.error(new RuntimeException("No se pudo refrescar el token de Start.gg", ex));
                            });
                });
    }

    private void linkUserToPlayer(User user) {
        // Buscar si existe un Player local con el mismo startGgUserId
        Optional<Player> playerById = playerRepository.findByStartGgUserId(user.getStartGgUserId());
        if (playerById.isPresent()) {
            log.info("Vínculo establecido entre usuario y jugador por ID start.gg: {}", user.getGamerTag());
            return;
        }

        // Buscar por gamertag (case-insensitive) y vincularlo
        List<Player> playersByTag = playerRepository.findAll().stream()
                .filter(p -> p.getGamertag().equalsIgnoreCase(user.getGamerTag()))
                .toList();

        if (!playersByTag.isEmpty()) {
            Player p = playersByTag.get(0);
            p.setStartGgUserId(user.getStartGgUserId());
            playerRepository.save(p);
            log.info("Vínculo establecido entre usuario y jugador por coincidencia de Gamertag: {}", user.getGamerTag());
        }
    }
}
