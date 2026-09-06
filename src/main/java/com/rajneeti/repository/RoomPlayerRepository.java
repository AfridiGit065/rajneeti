package com.rajneeti.repository;

import com.rajneeti.entity.RoomPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for {@link RoomPlayer}.
 */
@Repository
public interface RoomPlayerRepository extends JpaRepository<RoomPlayer, UUID> {

    List<RoomPlayer> findByRoomId(UUID roomId);

    Optional<RoomPlayer> findByRoomIdAndUserId(UUID roomId, UUID userId);

    Optional<RoomPlayer> findByRoomIdAndSeatNumber(UUID roomId, Integer seatNumber);

    boolean existsByRoomIdAndUserId(UUID roomId, UUID userId);

    boolean existsByRoomIdAndSeatNumber(UUID roomId, Integer seatNumber);

    long countByRoomId(UUID roomId);

    void deleteByRoomIdAndUserId(UUID roomId, UUID userId);
}