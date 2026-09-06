package com.rajneeti.repository;

import com.rajneeti.entity.Room;
import com.rajneeti.entity.enums.RoomStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for {@link Room}.
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

    Optional<Room> findByRoomCode(String roomCode);

    boolean existsByRoomCode(String roomCode);

    List<Room> findByStatus(RoomStatus status);

    Page<Room> findByStatus(RoomStatus status, Pageable pageable);

    List<Room> findByHostId(UUID hostId);
}