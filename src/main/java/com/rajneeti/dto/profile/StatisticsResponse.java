package com.rajneeti.dto.profile;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response payload providing detailed player game statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StatisticsResponse {

    private UUID userId;
    private String username;
    private Integer rating;
    private Integer totalMatches;
    private Integer wins;
    private Integer losses;
    private Double winRate;
    private Long totalCoinsEarned;
    private Long totalCoinsSpent;
}