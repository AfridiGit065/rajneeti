package com.rajneeti.repository;

import com.rajneeti.entity.Match;
import com.rajneeti.entity.enums.MatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository for {@link Match}.
 */
@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {

    List<Match> findByStatus(MatchStatus status);

    Page<Match> findByStatus(MatchStatus status, Pageable pageable);

    List<Match> findByWinnerId(UUID winnerId);

    Page<Match> findByWinnerId(UUID winnerId, Pageable pageable);

    List<Match> findByRoomId(UUID roomId);
}