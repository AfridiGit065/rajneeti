package com.rajneeti.dto.profile;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response payload representing a player's user profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileResponse {

    private UUID id;
    private String username;
    private String email;
    private String avatarUrl;
    private Integer rating;
    private Integer totalMatches;
    private Integer wins;
    private Integer losses;
    private Double winRate;
}