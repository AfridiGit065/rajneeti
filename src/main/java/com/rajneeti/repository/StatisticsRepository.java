package com.rajneeti.repository;

import com.rajneeti.entity.Statistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for {@link Statistics}.
 */
@Repository
public interface StatisticsRepository extends JpaRepository<Statistics, UUID> {

    Optional<Statistics> findByUserId(UUID userId);

    Optional<Statistics> findByUserUsername(String username);
}