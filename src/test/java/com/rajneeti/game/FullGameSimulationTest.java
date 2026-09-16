package com.rajneeti.game;

import com.rajneeti.dto.game.GameStateResponse;
import com.rajneeti.dto.match.MatchResponse;
import com.rajneeti.dto.room.CreateRoomRequest;
import com.rajneeti.dto.room.JoinRoomRequest;
import com.rajneeti.dto.room.RoomResponse;
import com.rajneeti.entity.Match;
import com.rajneeti.entity.MatchPlayer;
import com.rajneeti.entity.User;
import com.rajneeti.entity.enums.MatchStatus;
import com.rajneeti.entity.enums.PlayerStatus;
import com.rajneeti.repository.MatchPlayerRepository;
import com.rajneeti.repository.MatchRepository;
import com.rajneeti.repository.UserRepository;
import com.rajneeti.service.MatchService;
import com.rajneeti.service.RoomService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Module 21 — Phase 4 end-to-end simulation.
 *
 * <p>Unlike the manager-level unit tests, this drives the real Spring beans
 * (room service -> match service -> game engine -> challenge/block managers ->
 * action resolver -> winner manager) against an in-memory H2 database. It plays
 * a complete two-player match from an empty lobby all the way to game over and
 * asserts the persisted outcome, proving the whole Phase 4 pipeline fits
 * together without any Mockito stubs.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "jwt.secret=dGVzdC1zZWNyZXQtdGhhdC1pcy1sb25nLWVub3VnaC1mb3ItaG1hYy1zaGEyNTY=",
        "logging.level.root=WARN"
})
class FullGameSimulationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomService roomService;

    @Autowired
    private MatchService matchService;

    @Autowired
    private GameEngine gameEngine;

    @Autowired
    private ChallengeManager challengeManager;

    @Autowired
    private BlockManager blockManager;

    @Autowired
    private ActionResolver actionResolver;

    @Autowired
    private GameStore gameStore;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private MatchPlayerRepository matchPlayerRepository;

    private UUID matchId;

    private UUID hostId;
    private UUID guestId;

    @AfterEach
    void cleanup() {
        if (matchId != null) {
            gameStore.remove(matchId);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Scenario 1 — room -> match -> block -> assassinate -> game over    */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Full game: a blocked action and two assassinations end the match with one survivor")
    void fullGame_reachesGameOverDeterministically() {
        startTwoPlayerMatch();

        // Setup: the engine lazily initialised the live game on the first read.
        GameState initial = gameEngine.getGameState(matchId);
        assertThat(initial.getPlayers()).hasSize(2);
        assertThat(initial.getPhase()).isEqualTo(GameEngine.PHASE_SETUP);
        assertThat(coinsOf(hostId)).isEqualTo(GameEngine.STARTING_COINS);
        assertThat(cardsOf(hostId)).isEqualTo(GameEngine.STARTING_INFLUENCE);
        assertThat(initial.getCurrentTurnPlayerId()).isEqualTo(hostId);

        // Turn 1 — host income (+1).
        gameEngine.performIncome(matchId, hostId);
        assertTurn(guestId);
        assertThat(coinsOf(hostId)).isEqualTo(3);

        // Turn 2 — guest declares Foreign Aid; host blocks as Minister; the
        // Action Resolver confirms the block and the turn advances.
        gameEngine.performForeignAid(matchId, guestId);
        assertThat(gameEngine.getGameState(matchId).getPendingAction()).isNotNull();

        blockManager.block(matchId, hostId, GameEngine.CHARACTER_MINISTER);
        assertThat(gameEngine.getGameState(matchId).getPendingAction().getBlockerUserId())
                .isEqualTo(hostId);

        actionResolver.resolve(matchId, guestId);
        assertThat(coinsOf(guestId)).isEqualTo(GameEngine.STARTING_COINS);
        assertThat(gameEngine.getGameState(matchId).getPendingAction()).isNull();
        assertTurn(hostId);

        // Turns 3-5 — cycle income until the guest can afford an assassination.
        gameEngine.performIncome(matchId, hostId);   // host 4
        assertTurn(guestId);
        gameEngine.performIncome(matchId, guestId);  // guest 3
        assertTurn(hostId);
        gameEngine.performIncome(matchId, hostId);   // host 5
        assertTurn(guestId);

        // Turn 6 — guest assassinates the host (first influence lost).
        gameEngine.performAssassinate(matchId, guestId, hostId);
        actionResolver.resolve(matchId, guestId);
        assertThat(cardsOf(hostId)).isEqualTo(1);
        assertThat(coinsOf(guestId)).isZero();
        assertThat(gameEngine.getGameState(matchId).getStatus()).isNotEqualTo(MatchStatus.FINISHED);
        assertTurn(hostId);

        // Turns 7-13 — cycle income until the guest can afford the final blow.
        gameEngine.performIncome(matchId, hostId);   // host 6
        assertTurn(guestId);
        gameEngine.performIncome(matchId, guestId);  // guest 1
        assertTurn(hostId);
        gameEngine.performIncome(matchId, hostId);   // host 7
        assertTurn(guestId);
        gameEngine.performIncome(matchId, guestId);  // guest 2
        assertTurn(hostId);
        gameEngine.performIncome(matchId, hostId);   // host 8
        assertTurn(guestId);
        gameEngine.performIncome(matchId, guestId);  // guest 3
        assertTurn(hostId);
        gameEngine.performIncome(matchId, hostId);   // host 9
        assertTurn(guestId);

        // Turn 14 — the guest lands the second assassination: the host loses
        // their last influence card and the game is over.
        gameEngine.performAssassinate(matchId, guestId, hostId);
        GameStateResponse finalResponse = actionResolver.resolve(matchId, guestId);

        assertThat(statusOf(hostId)).isEqualTo(PlayerStatus.ELIMINATED);
        assertThat(cardsOf(hostId)).isZero();
        assertGameOver(finalResponse, guestId);

        assertPersistedOutcome(winnerId -> winnerId.equals(guestId));
    }

    /* ------------------------------------------------------------------ */
    /*  Scenario 2 — bluff challenge exposes the claimant and ends the game */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Full game: a bluff claim challenged by the last opponent ends the match")
    void fullGame_bluffChallengeEndsTheGame() {
        startTwoPlayerMatch();

        // Turns 1-3 — bank enough coins for the guest to assassinate once.
        gameEngine.performIncome(matchId, hostId);   // host 3
        assertTurn(guestId);
        gameEngine.performIncome(matchId, guestId);  // guest 3
        assertTurn(hostId);
        gameEngine.performIncome(matchId, hostId);   // host 4
        assertTurn(guestId);

        // Turn 4 — guest assassinates, leaving the host with a single card.
        gameEngine.performAssassinate(matchId, guestId, hostId);
        actionResolver.resolve(matchId, guestId);
        assertThat(cardsOf(hostId)).isEqualTo(1);
        assertTurn(hostId);

        // Turns 5-6 — reach the host's turn with exactly one influence card.
        gameEngine.performIncome(matchId, hostId);   // host 5
        assertTurn(guestId);
        gameEngine.performIncome(matchId, guestId);  // guest 1
        assertTurn(hostId);

        // The host now bluffs a character they do not hold. Claiming a
        // character that is guaranteed to be absent keeps the challenge outcome
        // deterministic without peeking at the opponent's (public-safe) state.
        CharacterType lastCard = gameEngine.getGameState(matchId)
                .getPlayers().stream()
                .filter(p -> p.getUserId().equals(hostId))
                .findFirst().orElseThrow()
                .getCards().get(0).getCharacter();

        if (lastCard == CharacterType.MINISTER) {
            // Holds Minister -> claim Dalal (a guaranteed bluff).
            gameEngine.performSteal(matchId, hostId, guestId);
        } else {
            // Does not hold Minister -> claim Minister (a guaranteed bluff).
            gameEngine.performTax(matchId, hostId);
        }

        // Turn 7 — the guest challenges the bluff. The host loses their last
        // influence card and the guest is the last player standing.
        GameStateResponse finalResponse = challengeManager.challenge(matchId, guestId, null);

        assertThat(statusOf(hostId)).isEqualTo(PlayerStatus.ELIMINATED);
        assertThat(cardsOf(hostId)).isZero();
        assertGameOver(finalResponse, guestId);

        assertPersistedOutcome(winnerId -> winnerId.equals(guestId));
    }

    /* ------------------------------------------------------------------ */
    /*  Setup + assertions                                                */
    /* ------------------------------------------------------------------ */

    private void startTwoPlayerMatch() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        User host = userRepository.save(User.builder()
                .username("sim-host-" + suffix)
                .email("sim-host-" + suffix + "@rajneeti.test")
                .password("secret")
                .build());
        User guest = userRepository.save(User.builder()
                .username("sim-guest-" + suffix)
                .email("sim-guest-" + suffix + "@rajneeti.test")
                .password("secret")
                .build());
        hostId = host.getId();
        guestId = guest.getId();

        RoomResponse room = roomService.createRoom(hostId,
                CreateRoomRequest.builder().maxPlayers(2).build());
        roomService.joinRoom(guestId, JoinRoomRequest.builder()
                .roomCode(room.getRoomCode())
                .build());
        roomService.setReadyStatus(room.getId(), hostId, true);
        roomService.setReadyStatus(room.getId(), guestId, true);

        assertThat(roomService.canStartMatch(room.getId())).isTrue();

        MatchResponse match = matchService.startMatch(room.getId(), hostId);
        matchId = match.getId();

        assertThat(match.getStatus()).isEqualTo(MatchStatus.CREATED);
        assertThat(match.getCurrentTurnPlayerId()).isEqualTo(hostId);
        assertThat(match.getTurnNumber()).isEqualTo(1);

        // Initialize the live game explicitly so the first assertions are stable.
        gameEngine.getOrInitialize(matchId);
    }

    private GamePlayerState runtime(UUID userId) {
        return gameEngine.getGameState(matchId).getPlayers().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow();
    }

    private int coinsOf(UUID userId) {
        return runtime(userId).getCoins();
    }

    private int cardsOf(UUID userId) {
        return runtime(userId).getCards().size();
    }

    private PlayerStatus statusOf(UUID userId) {
        return runtime(userId).getStatus();
    }

    private void assertTurn(UUID expected) {
        assertThat(gameEngine.getGameState(matchId).getCurrentTurnPlayerId()).isEqualTo(expected);
    }

    private void assertGameOver(GameStateResponse response, UUID expectedWinner) {
        GameState state = gameEngine.getGameState(matchId);
        assertThat(state.getStatus()).isEqualTo(MatchStatus.FINISHED);
        assertThat(state.getPhase()).isEqualTo(GameEngine.PHASE_GAME_OVER);
        assertThat(state.getWinnerUserId()).isEqualTo(expectedWinner);
        assertThat(state.getEndedAt()).isNotNull();
        assertThat(state.getPendingAction()).isNull();

        assertThat(response.getStatus()).isEqualTo(MatchStatus.FINISHED);
        assertThat(response.getPhase()).isEqualTo(GameEngine.PHASE_GAME_OVER);
        assertThat(response.getWinnerUserId()).isEqualTo(expectedWinner);
        assertThat(response.getEndedAt()).isNotNull();
    }

    private void assertPersistedOutcome(java.util.function.Predicate<UUID> winnerPredicate) {
        Match match = matchRepository.findById(matchId).orElseThrow();
        assertThat(match.getStatus()).isEqualTo(MatchStatus.FINISHED);
        assertThat(match.getEndedAt()).isNotNull();
        assertThat(match.getWinner()).isNotNull();
        assertThat(winnerPredicate.test(match.getWinner().getId())).isTrue();

        List<MatchPlayer> rows = matchPlayerRepository.findByMatchId(matchId);
        MatchPlayer winnerRow = rows.stream()
                .filter(r -> r.getUser().getId().equals(match.getWinner().getId()))
                .findFirst().orElseThrow();
        MatchPlayer loserRow = rows.stream()
                .filter(r -> !r.getUser().getId().equals(match.getWinner().getId()))
                .findFirst().orElseThrow();

        assertThat(winnerRow.getPlayerStatus()).isEqualTo(PlayerStatus.ACTIVE);
        assertThat(winnerRow.getFinalRank()).isEqualTo(1);
        assertThat(winnerRow.getCoinsAtEnd()).isEqualTo(winnerRow.getCoins());

        assertThat(loserRow.getPlayerStatus()).isEqualTo(PlayerStatus.ELIMINATED);
        assertThat(loserRow.getEliminated()).isTrue();
        assertThat(loserRow.getEliminatedAt()).isNotNull();
        assertThat(loserRow.getFinalRank()).isEqualTo(2);
        assertThat(loserRow.getCoinsAtEnd()).isNotNull();

        // Player-safe projection: the winner is exposed, the loser's hand is
        // never leaked to the opponent.
        GameStateResponse winnerView = gameEngine.getSafeGameState(matchId, winnerRow.getUser().getId());
        assertThat(winnerView.getWinnerUserId()).isEqualTo(match.getWinner().getId());
        assertThat(winnerView.getPlayers().stream()
                .filter(p -> p.getUserId().equals(loserRow.getUser().getId()))
                .findFirst().orElseThrow()
                .getCards()).isNull();
    }
}
