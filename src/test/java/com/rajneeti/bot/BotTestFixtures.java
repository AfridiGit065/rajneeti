package com.rajneeti.bot;

import com.rajneeti.entity.User;
import com.rajneeti.entity.enums.MatchStatus;
import com.rajneeti.entity.enums.PlayerStatus;
import com.rajneeti.game.CharacterType;
import com.rajneeti.game.GameCard;
import com.rajneeti.game.GamePlayerState;
import com.rajneeti.game.GameState;
import com.rajneeti.game.PendingAction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Reusable fixture builders for bot decision tests: a small in-memory
 * {@link GameState} with deterministic players/cards/pending actions.
 */
final class BotTestFixtures {

    private BotTestFixtures() {
    }

    static User botUser(String name) {
        return User.builder()
                .username(name)
                .email(name.toLowerCase() + "@rajneeti.bot")
                .password("x")
                .isBot(true)
                .botDifficulty("MEDIUM")
                .botPersonality("BALANCED")
                .build();
    }

    static GameCard card(CharacterType character) {
        return GameCard.builder().id(UUID.randomUUID()).character(character).build();
    }

    /** A bare game-state builder with players and a deck, ready to customize. */
    static GameState.GameStateBuilder baseState(GamePlayerState... players) {
        return GameState.builder()
                .matchId(UUID.randomUUID())
                .roomId(UUID.randomUUID())
                .roomCode("RAJTEST")
                .status(MatchStatus.IN_PROGRESS)
                .phase("in_progress")
                .currentTurnPlayerId(players.length > 0 ? players[0].getUserId() : null)
                .turnNumber(3)
                .players(new ArrayList<>(List.of(players)))
                .turnOrder(new ArrayList<>(java.util.Arrays.stream(players)
                        .map(GamePlayerState::getUserId).toList()))
                .deck(new ArrayList<>())
                .startedAt(LocalDateTime.now())
                .actionExecuted(false);
    }

    static GamePlayerState player(UUID userId, String username, int coins, List<GameCard> cards) {
        return GamePlayerState.builder()
                .userId(userId)
                .username(username)
                .seatNumber(1)
                .status(PlayerStatus.ACTIVE)
                .coins(coins)
                .cards(cards)
                .build();
    }

    static GamePlayerState botPlayer(UUID userId, String username, int coins, List<GameCard> cards) {
        return GamePlayerState.builder()
                .userId(userId)
                .username(username)
                .seatNumber(1)
                .status(PlayerStatus.ACTIVE)
                .coins(coins)
                .cards(cards)
                .isBot(true)
                .botDifficulty("MEDIUM")
                .botPersonality("BALANCED")
                .build();
    }

    static PendingAction pending(String type, UUID actorUserId, String claimedCharacter,
                                 UUID targetPlayerId) {
        return PendingAction.builder()
                .id(UUID.randomUUID())
                .type(type)
                .actorUserId(actorUserId)
                .startedAt(LocalDateTime.now())
                .claimedCharacter(claimedCharacter)
                .targetPlayerId(targetPlayerId)
                .build();
    }
}