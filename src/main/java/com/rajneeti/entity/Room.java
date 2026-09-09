package com.rajneeti.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rajneeti.entity.enums.RoomStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Room Entity representing a multiplayer lobby room.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"host", "players"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@Table(name = "rooms", indexes = {
        @Index(name = "idx_rooms_room_code", columnList = "room_code"),
        @Index(name = "idx_rooms_status", columnList = "status"),
        @Index(name = "idx_rooms_host_id", columnList = "host_id")
})
public class Room extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "Room code is required")
    @Size(min = 4, max = 10, message = "Room code must be between 4 and 10 characters")
    @Column(name = "room_code", nullable = false, unique = true, length = 10)
    private String roomCode;

    @NotNull(message = "Host is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @NotNull(message = "Room status is required")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private RoomStatus status = RoomStatus.WAITING;

    @Min(value = 2, message = "Minimum players in a room is 2")
    @Max(value = 8, message = "Maximum players in a room is 8")
    @Builder.Default
    @Column(name = "max_players", nullable = false)
    private Integer maxPlayers = 6;

    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomPlayer> players = new ArrayList<>();
}