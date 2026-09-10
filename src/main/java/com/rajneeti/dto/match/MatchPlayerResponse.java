package com.rajneeti.dto.match;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rajneeti.entity.enums.PlayerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for a player within a match.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MatchPlayerResponse {

    private UUID id;
    private UUID userId;
    private String username;
    private String avatarUrl;
    private Integer seatNumber;
    private PlayerStatus playerStatus;
    private Integer finalRank;
    private Integer coinsAtEnd;
    private Boolean eliminated;
    private LocalDateTime eliminatedAt;
}
