package com.rajneeti.dto.meta;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Historical match result for a player, with the final per-participant snapshot.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MatchHistoryEntryDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ParticipantDto {

        private UUID userId;
        private String username;
        private String avatarUrl;
        private Integer seatNumber;
        private Integer finalRank;
        private Boolean isCurrentUser;
    }

    private UUID matchId;
    private String playedAt;
    private Integer durationMinutes;
    private Integer position;
    private Integer playerCount;
    private String result;
    private Boolean eliminated;
    private List<ParticipantDto> participants;
}