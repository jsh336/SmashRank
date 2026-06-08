package com.smashrank.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * DTO genérico para encapsular una query/mutation GraphQL hacia Start.gg.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GraphQLRequestDTO {

    /** La query GraphQL en formato string */
    private String query;

    /** Variables opcionales para la query (puede ser null) */
    private Object variables;
}
