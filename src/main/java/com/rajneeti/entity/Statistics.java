package com.rajneeti.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Statistics Entity representing aggregated performance metrics for a user.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "statistics", indexes = {
        @Index(name = "idx_statistics_user_id", columnList = "user_id"),
        @Index(name = "idx_statistics_win_rate", columnList = "win_rate")
})
public class Statistics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull(message = "User is required")
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Builder.Default
    @Column(name = "total_matches", nullable = false)
    private Integer totalMatches = 0;

    @Builder.Default
    @Column(name = "wins", nullable = false)
    private Integer wins = 0;

    @Builder.Default
    @Column(name = "losses", nullable = false)
    private Integer losses = 0;

    @Builder.Default
    @Column(name = "win_rate", nullable = false)
    private Double winRate = 0.0;

    @Builder.Default
    @Column(name = "total_coins_earned", nullable = false)
    private Long totalCoinsEarned = 0L;

    @Builder.Default
    @Column(name = "total_coins_spent", nullable = false)
    private Long totalCoinsSpent = 0L;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void calculateWinRate() {
        if (totalMatches != null && totalMatches > 0 && wins != null) {
            this.winRate = Math.round(((double) wins / totalMatches * 100.0) * 100.0) / 100.0;
        } else {
            this.winRate = 0.0;
        }
    }
}