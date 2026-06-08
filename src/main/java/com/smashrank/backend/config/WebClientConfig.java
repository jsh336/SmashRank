package com.smashrank.backend.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuración del cliente WebClient para consumir la API GraphQL de Start.gg.
 * Start.gg usa GraphQL sobre HTTP, por lo que WebClient de WebFlux es ideal.
 */
@Configuration
public class WebClientConfig {

    @Value("${startgg.api.url}")
    private String startGgApiUrl;

    @Value("${startgg.api.token}")
    private String startGgApiToken;

    /**
     * Bean WebClient preconfigurado con la URL base y el token de autorización
     * de la API GraphQL de Start.gg.
     */
    @Bean
    public WebClient startGgWebClient() {
        return WebClient.builder()
                .baseUrl(startGgApiUrl)
                .defaultHeader("Authorization", "Bearer " + startGgApiToken)
                .defaultHeader("Content-Type", "application/json")
                .codecs(configurer ->
                        configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024) // 16 MB
                )
                .build();
    }
}
