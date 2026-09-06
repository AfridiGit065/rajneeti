package com.rajneeti.repository;

import com.rajneeti.entity.MatchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository for {@link MatchHistory}.
 */
@Repository
public interface MatchHistoryRepository extends JpaRepository<MatchHistory, UUID> {

    List<MatchHistory> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<MatchHistory> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<MatchHistory> findByMatchId(UUID matchId);
}