package com.rajneeti.dto.meta;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Leaderboard row exposing a player's competitive standing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LeaderboardEntryDto {

    private Integer rank;
    private UUID userId;
    private String username;
    private String avatarUrl;
    private Integer rating;
    private Integer wins;
    private Integer losses;
    private Integer totalMatches;
    private Double winRate;
}