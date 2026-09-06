package com.rajneeti.repository;

import com.rajneeti.entity.MatchPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for {@link MatchPlayer}.
 */
@Repository
public interface MatchPlayerRepository extends JpaRepository<MatchPlayer, UUID> {

    List<MatchPlayer> findByMatchId(UUID matchId);

    Optional<MatchPlayer> findByMatchIdAndUserId(UUID matchId, UUID userId);

    Optional<MatchPlayer> findByMatchIdAndSeatNumber(UUID matchId, Integer seatNumber);

    List<MatchPlayer> findByUserId(UUID userId);
}