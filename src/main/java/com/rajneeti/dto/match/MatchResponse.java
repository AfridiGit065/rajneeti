package com.rajneeti.dto.match;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rajneeti.entity.enums.MatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for match details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MatchResponse {

    private UUID id;
    private UUID roomId;
    private String roomCode;
    private MatchStatus status;
    private Integer playerCount;
    private List<MatchPlayerResponse> players;
    private UUID winnerId;
    private String winnerUsername;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime createdAt;
}
