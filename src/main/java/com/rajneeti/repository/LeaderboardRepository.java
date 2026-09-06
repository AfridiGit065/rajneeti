package com.rajneeti.repository;

import com.rajneeti.entity.Leaderboard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for {@link Leaderboard}.
 */
@Repository
public interface LeaderboardRepository extends JpaRepository<Leaderboard, UUID> {

    Optional<Leaderboard> findByUserId(UUID userId);

    List<Leaderboard> findTop100ByOrderByRatingDesc();

    Page<Leaderboard> findAllByOrderByRatingDesc(Pageable pageable);

    Page<Leaderboard> findAllByOrderByRankAsc(Pageable pageable);
}