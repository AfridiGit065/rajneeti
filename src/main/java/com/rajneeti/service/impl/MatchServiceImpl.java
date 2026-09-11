package com.rajneeti.service.impl;

import com.rajneeti.dto.action.ActionRequest;
import com.rajneeti.dto.action.MatchActionResponse;
import com.rajneeti.dto.match.MatchResponse;
import com.rajneeti.dto.turn.TurnInfo;
import com.rajneeti.entity.Match;
import com.rajneeti.entity.MatchPlayer;
import com.rajneeti.entity.PendingAction;
import com.rajneeti.entity.Room;
import com.rajneeti.entity.RoomPlayer;
import com.rajneeti.entity.enums.CharacterType;
import com.rajneeti.entity.enums.MatchActionType;
import com.rajneeti.entity.enums.MatchStatus;
import com.rajneeti.entity.enums.PendingActionStatus;
import com.rajneeti.entity.enums.PlayerStatus;
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
import com.rajneeti.service.TurnManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final TurnManager            turnManager;

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

        // 9. Assign the first turn (deterministic: lowest seat number) and set startedAt
        savedMatch.setStartedAt(LocalDateTime.now());
        turnManager.assignFirstTurn(savedMatch, matchPlayers);

        // 10. Transition room status to IN_GAME
        room.setStatus(RoomStatus.IN_GAME);
        roomRepository.save(room);

        log.info("Match {} started for room '{}' ({} players) by host '{}'",
                savedMatch.getId(), room.getRoomCode(), matchPlayers.size(),
                room.getHost().getUsername());

        // 11. Return match response
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

    @Override
    @Transactional(readOnly = true)
    public TurnInfo getCurrentTurn(UUID matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException("Match with ID '" + matchId + "' not found."));

        List<MatchPlayer> players = matchPlayerRepository.findByMatchId(matchId);
        List<UUID> turnOrder = players.stream()
                .sorted(Comparator.comparing(MatchPlayer::getSeatNumber))
                .map(mp -> mp.getUser().getId())
                .toList();

        return TurnInfo.builder()
                .matchId(match.getId())
                .currentTurnPlayerId(match.getCurrentTurnPlayerId())
                .turnNumber(match.getTurnNumber())
                .turnOrder(turnOrder)
                .activePlayerCount(players.stream()
                        .filter(p -> p.getPlayerStatus() == PlayerStatus.ACTIVE)
                        .count())
                .build();
    }

    @Override
    @Transactional
    public MatchActionResponse performAction(UUID matchId, UUID userId, ActionRequest request) {
        // 1. Validate match exists
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException("Match with ID '" + matchId + "' not found."));

        // 2. Validate match is active
        if (match.getStatus() != MatchStatus.CREATED && match.getStatus() != MatchStatus.IN_PROGRESS) {
            throw new BusinessException("MATCH_NOT_ACTIVE",
                    "Action cannot be performed. Match status is " + match.getStatus() + ".");
        }

        // 3. Validate caller belongs to the match
        MatchPlayer player = matchPlayerRepository.findByMatchIdAndUserId(matchId, userId)
                .orElseThrow(() -> new BusinessException("NOT_IN_MATCH",
                        "You are not a player in this match."));

        // 4. Validate caller is not eliminated
        if (player.getPlayerStatus() == PlayerStatus.ELIMINATED || Boolean.TRUE.equals(player.getEliminated())) {
            throw new BusinessException("PLAYER_ELIMINATED",
                    "Eliminated players cannot perform actions.");
        }

        // 5. Validate it is the caller's turn
        if (!turnManager.isPlayerTurn(matchId, userId)) {
            throw new BusinessException("NOT_YOUR_TURN",
                    "It is not your turn to perform an action.");
        }

        // 6. Validate no action is already pending (one action per turn)
        if (match.getPendingAction() != null) {
            throw new BusinessException("ACTION_PENDING",
                    "An action is already awaiting resolution. Complete it before performing another.");
        }

        MatchActionType actionType = request.getAction();

        // 7. Validate the action type is supported
        if (actionType != MatchActionType.TAX && actionType != MatchActionType.STEAL) {
            throw new BusinessException("ACTION_NOT_SUPPORTED",
                    "Action '" + actionType + "' is not implemented yet.");
        }

        // 8. Validate the claimed character for the action
        CharacterType claimedCharacter = resolveClaimedCharacter(request);

        // 9. Validate the target player when the action targets another player
        UUID targetUserId = request.getTargetPlayerId();
        if (actionType == MatchActionType.STEAL) {
            targetUserId = validateStealTarget(match, player, targetUserId);
        }

        // 10. Persist the pending action (challenge window). No coins awarded yet.
        PendingAction pendingAction = PendingAction.builder()
                .actionType(actionType)
                .claimedCharacter(claimedCharacter)
                .actorUserId(userId)
                .targetUserId(targetUserId)
                .status(PendingActionStatus.AWAITING_CHALLENGE)
                .coinsToAward(actionType.getGainCoins())
                .createdAt(LocalDateTime.now())
                .build();

        match.setPendingAction(pendingAction);
        matchRepository.save(match);

        log.info("Player '{}' claimed {} in match {}, pending challenge",
                player.getUser().getUsername(), actionType, matchId);

        return matchMapper.toMatchActionResponse(match, pendingAction,
                player.getUser().getUsername());
    }

    private CharacterType resolveClaimedCharacter(ActionRequest request) {
        CharacterType claimed = request.getClaimedCharacter();

        if (request.getAction() == MatchActionType.STEAL) {
            // Steal claims the Dalal. Possession is never checked — this is a bluff game.
            if (claimed == null) {
                return CharacterType.DALAL;
            }
            if (claimed != CharacterType.DALAL) {
                throw new BusinessException("INVALID_CLAIM",
                        "Steal can only claim the " + CharacterType.DALAL + ".");
            }
            return claimed;
        }

        // Tax claims the Minister; default the claim if not provided.
        if (claimed == null) {
            return CharacterType.MINISTER;
        }
        if (claimed != CharacterType.MINISTER) {
            throw new BusinessException("INVALID_CLAIM",
                    "Tax can only claim the " + CharacterType.MINISTER + ".");
        }
        return claimed;
    }

    private UUID validateStealTarget(Match match, MatchPlayer actor, UUID targetUserId) {
        if (targetUserId == null) {
            throw new BusinessException("TARGET_REQUIRED",
                    "Steal requires a target player.");
        }
        if (actor.getUser().getId().equals(targetUserId)) {
            throw new BusinessException("CANNOT_TARGET_SELF",
                    "You cannot steal from yourself.");
        }

        MatchPlayer target = matchPlayerRepository.findByMatchIdAndUserId(match.getId(), targetUserId)
                .orElseThrow(() -> new BusinessException("INVALID_TARGET",
                        "Target player is not in this match."));

        if (target.getPlayerStatus() == PlayerStatus.ELIMINATED || Boolean.TRUE.equals(target.getEliminated())) {
            throw new BusinessException("TARGET_ELIMINATED",
                    "The target player is eliminated.");
        }

        // Established rule: a target with no coins cannot be stolen from.
        Integer targetCoins = target.getCoins();
        if (targetCoins == null || targetCoins <= 0) {
            throw new BusinessException("TARGET_NO_COINS",
                    "The target player has no coins to steal.");
        }

        return targetUserId;
    }
}
