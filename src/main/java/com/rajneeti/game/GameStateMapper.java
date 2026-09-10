package com.rajneeti.game;

import com.rajneeti.dto.game.GameCardDto;
import com.rajneeti.dto.game.GameLogEntryDto;
import com.rajneeti.dto.game.GamePlayerDto;
import com.rajneeti.dto.game.GameStateResponse;
import com.rajneeti.dto.game.PendingActionDto;
import com.rajneeti.entity.enums.PlayerStatus;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Projects an internal {@link GameState} into its player-safe public form.
 *
 * <p>The projection is perspective-dependent: the requesting player sees their
 * own hand (cards), while opponents' cards are never included — only public
 * information such as influence count and coin balance.
 */
@Component
public class GameStateMapper {

    /**
     * Builds the public game-state response for a given viewer.
     *
     * @param state    the authoritative in-memory game state
     * @param viewerId the requesting player's user ID (projection perspective)
     */
    public GameStateResponse toResponse(GameState state, UUID viewerId) {
        List<GamePlayerDto> players = (state.getPlayers() == null)
                ? Collections.emptyList()
                : state.getPlayers().stream()
                        .map(player -> toPlayerDto(state, player, viewerId))
                        .toList();

        List<GameLogEntryDto> log = (state.getLog() == null)
                ? Collections.emptyList()
                : state.getLog().stream()
                        .map(entry -> GameLogEntryDto.builder()
                                .id(entry.getId())
                                .timestamp(entry.getTimestamp())
                                .text(entry.getText())
                                .kind(entry.getKind())
                                .build())
                        .toList();

        PendingActionDto pendingDto = null;
        if (state.getPendingAction() != null) {
            pendingDto = PendingActionDto.builder()
                    .type(state.getPendingAction().getType())
                    .actorUserId(state.getPendingAction().getActorUserId())
                    .startedAt(state.getPendingAction().getStartedAt())
                    .build();
        }

        return GameStateResponse.builder()
                .matchId(state.getMatchId())
                .roomId(state.getRoomId())
                .roomCode(state.getRoomCode())
                .status(state.getStatus())
                .phase(state.getPhase())
                .hostUserId(state.getHostUserId())
                .players(players)
                .currentTurnPlayerId(state.getCurrentTurnPlayerId())
                .turnNumber(state.getTurnNumber())
                .turnOrder(state.getTurnOrder())
                .deckCount(state.getDeck() != null ? state.getDeck().size() : 0)
                .revealedCardsCount(state.getRevealedCardsCount())
                .winnerUserId(state.getWinnerUserId())
                .log(log)
                .pendingAction(pendingDto)
                .startedAt(state.getStartedAt())
                .endedAt(state.getEndedAt())
                .build();
    }

    private GamePlayerDto toPlayerDto(GameState state, GamePlayerState player, UUID viewerId) {
        boolean isViewer = viewerId != null && viewerId.equals(player.getUserId());

        List<GameCardDto> cards = isViewer
                ? player.getCards().stream()
                        .map(card -> GameCardDto.builder()
                                .cardId(card.getId())
                                .characterId(card.getCharacter().name().toLowerCase())
                                .build())
                        .toList()
                : null;

        return GamePlayerDto.builder()
                .userId(player.getUserId())
                .username(player.getUsername())
                .avatarUrl(player.getAvatarUrl())
                .seatIndex(player.getSeatNumber() != null ? player.getSeatNumber() - 1 : null)
                .status(player.getStatus())
                .host(player.isHost())
                .alive(player.getStatus() == PlayerStatus.ACTIVE)
                .turn(state.getCurrentTurnPlayerId() != null
                        && state.getCurrentTurnPlayerId().equals(player.getUserId()))
                .coins(player.getCoins())
                .influenceCount(player.getCards() != null ? player.getCards().size() : 0)
                .cards(cards)
                .build();
    }
}