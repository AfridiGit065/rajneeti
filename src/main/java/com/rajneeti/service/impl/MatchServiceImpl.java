package com.rajneeti.service.impl;

import com.rajneeti.dto.match.MatchResponse;
import com.rajneeti.entity.Match;
import com.rajneeti.entity.MatchPlayer;
import com.rajneeti.entity.Room;
import com.rajneeti.entity.RoomPlayer;
import com.rajneeti.entity.enums.MatchStatus;
import com.rajneeti.entity.enums.RoomStatus;
import com.rajneeti.exception.BusinessException;
import com.rajneeti.exception.MatchAlreadyExistsException;
import com.rajneeti.exception.MatchNotFoundException;
import com.rajneeti.exception.NotRoomHostException;
import com.rajneeti.exception.RoomNotFoundException;
import com.rajneeti.mapper.MatchMapper;
import com.rajneeti.repository.MatchPlayerRepository;
import com.rajneeti.repository.MatchRepository;
import com.rajneeti.repository.RoomPlayerRepository;
import com.rajneeti.repository.RoomRepository;
import com.rajneeti.service.MatchService;
import com.rajneeti.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link MatchService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {

    private final MatchRepository        matchRepository;
    private final MatchPlayerRepository  matchPlayerRepository;
    private final RoomRepository         roomRepository;
    private final RoomPlayerRepository   roomPlayerRepository;
    private final RoomService            roomService;
    private final MatchMapper            matchMapper;

    @Override
    @Transactional
    public MatchResponse startMatch(UUID roomId, UUID userId) {
        // 1. Validate room exists
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room with ID '" + roomId + "' not found."));

        // 2. Validate requester is the host
        if (!room.getHost().getId().equals(userId)) {
            throw new NotRoomHostException("Only the room host can start a match.");
        }

        // 3. Validate room state is WAITING
        if (room.getStatus() != RoomStatus.WAITING) {
            throw new BusinessException("ROOM_NOT_WAITING",
                    "Cannot start match. Room status is " + room.getStatus() + ".");
        }

        // 4. Load current players
        List<RoomPlayer> roomPlayers = roomPlayerRepository.findByRoomIdOrderBySeatNumberAsc(roomId);

        // 5. Validate eligible players (reuse canStartMatch logic)
        boolean canStart = roomService.canStartMatch(roomId);
        if (!canStart) {
            throw new BusinessException("MATCH_START_CONDITIONS_NOT_MET",
                    "Cannot start match. Ensure at least 2 players are present and all players are ready.");
        }

        // 6. Prevent duplicate match creation
        boolean hasActiveMatch = matchRepository.findByRoomId(roomId).stream()
                .anyMatch(m -> m.getStatus() == MatchStatus.CREATED || m.getStatus() == MatchStatus.IN_PROGRESS);
        if (hasActiveMatch) {
            throw new MatchAlreadyExistsException("A match is already in progress or created for this room.");
        }

        // 7. Create Match
        Match match = Match.builder()
                .room(room)
                .status(MatchStatus.CREATED)
                .players(new ArrayList<>())
                .build();

        Match savedMatch = matchRepository.save(match);

        // 8. Create MatchPlayer records for every eligible RoomPlayer
        List<MatchPlayer> matchPlayers = new ArrayList<>();
        for (RoomPlayer rp : roomPlayers) {
            MatchPlayer matchPlayer = MatchPlayer.builder()
                    .match(savedMatch)
                    .user(rp.getUser())
                    .seatNumber(rp.getSeatNumber())
                    .coinsAtEnd(0)
                    .eliminated(false)
                    .build();

            matchPlayers.add(matchPlayerRepository.save(matchPlayer));
        }

        savedMatch.setPlayers(matchPlayers);

        // 9. Transition room status to IN_GAME
        room.setStatus(RoomStatus.IN_GAME);
        roomRepository.save(room);

        log.info("Match {} started for room '{}' ({} players) by host '{}'",
                savedMatch.getId(), room.getRoomCode(), matchPlayers.size(),
                room.getHost().getUsername());

        // 10. Return match response
        return matchMapper.toMatchResponse(savedMatch, matchPlayers);
    }

    @Override
    @Transactional(readOnly = true)
    public MatchResponse getMatch(UUID matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException("Match with ID '" + matchId + "' not found."));

        List<MatchPlayer> players = matchPlayerRepository.findByMatchId(matchId);
        return matchMapper.toMatchResponse(match, players);
    }

    @Override
    @Transactional(readOnly = true)
    public MatchResponse getActiveMatchByRoom(UUID roomId) {
        // Validate room exists
        roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room with ID '" + roomId + "' not found."));

        Match match = matchRepository.findByRoomId(roomId).stream()
                .filter(m -> m.getStatus() == MatchStatus.CREATED || m.getStatus() == MatchStatus.IN_PROGRESS)
                .findFirst()
                .orElseThrow(() -> new MatchNotFoundException("No active match found for room ID '" + roomId + "'."));

        List<MatchPlayer> players = matchPlayerRepository.findByMatchId(match.getId());
        return matchMapper.toMatchResponse(match, players);
    }
}
