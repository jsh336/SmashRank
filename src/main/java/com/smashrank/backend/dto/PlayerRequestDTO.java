package com.smashrank.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO para la creación y actualización de un jugador.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerRequestDTO {

    @NotBlank(message = "El gamertag es obligatorio")
    @Size(min = 2, max = 100, message = "El gamertag debe tener entre 2 y 100 caracteres")
    private String gamertag;

    private String startGgUserId;

    private String startGgSlug;

    @Size(max = 100)
    private String country;
}
