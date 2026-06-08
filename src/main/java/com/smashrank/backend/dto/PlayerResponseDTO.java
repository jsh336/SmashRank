package com.smashrank.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO de respuesta con los datos de un jugador.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerResponseDTO {

    private Long id;
    private String gamertag;
    private String startGgUserId;
    private String startGgSlug;
    private String country;
    private Integer rankPoints;
    private Integer rankPosition;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
