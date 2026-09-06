package com.rajneeti.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * MatchPlayer Entity representing player participation and outcome in a specific match.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"match", "user"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(
        name = "match_players",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_match_players_match_user", columnNames = {"match_id", "user_id"}),
                @UniqueConstraint(name = "uk_match_players_match_seat", columnNames = {"match_id", "seat_number"})
        },
        indexes = {
                @Index(name = "idx_match_players_match_id", columnList = "match_id"),
                @Index(name = "idx_match_players_user_id", columnList = "user_id")
        }
)
public class MatchPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull(message = "Match is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull(message = "Seat number is required")
    @Min(value = 1, message = "Seat number must be at least 1")
    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    @Column(name = "final_rank")
    private Integer finalRank;

    @Builder.Default
    @Column(name = "coins_at_end", nullable = false)
    private Integer coinsAtEnd = 0;

    @Builder.Default
    @Column(name = "eliminated", nullable = false)
    private Boolean eliminated = false;

    @Column(name = "eliminated_at")
    private LocalDateTime eliminatedAt;
}