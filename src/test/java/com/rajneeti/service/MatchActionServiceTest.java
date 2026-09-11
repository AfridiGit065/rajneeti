package com.rajneeti.service;

import com.rajneeti.dto.action.ActionRequest;
import com.rajneeti.dto.action.MatchActionResponse;
import com.rajneeti.entity.Match;
import com.rajneeti.entity.MatchPlayer;
import com.rajneeti.entity.PendingAction;
import com.rajneeti.entity.Room;
import com.rajneeti.entity.User;
import com.rajneeti.entity.enums.CharacterType;
import com.rajneeti.entity.enums.MatchActionType;
import com.rajneeti.entity.enums.MatchStatus;
import com.rajneeti.entity.enums.PendingActionStatus;
import com.rajneeti.entity.enums.PlayerStatus;
import com.rajneeti.entity.enums.RoomStatus;
import com.rajneeti.exception.BusinessException;
import com.rajneeti.exception.MatchNotFoundException;
import com.rajneeti.mapper.MatchMapper;
import com.rajneeti.repository.MatchPlayerRepository;
import com.rajneeti.repository.MatchRepository;
import com.rajneeti.repository.RoomPlayerRepository;
import com.rajneeti.repository.RoomRepository;
import com.rajneeti.repository.UserRepository;
import com.rajneeti.service.impl.MatchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchActionServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchPlayerRepository matchPlayerRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomPlayerRepository roomPlayerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomService roomService;

    @Mock
    private TurnManager turnManager;

    @Spy
    private MatchMapper matchMapper = new MatchMapper();

    @InjectMocks
    private MatchServiceImpl matchService;

    private User hostUser;
    private UUID hostId;
    private UUID matchId;
    private Room room;
    private Match match;
    private MatchPlayer hostPlayer;
    private User guestUser;
    private UUID guestId;
    private MatchPlayer guestPlayer;

    @BeforeEach
    void setUp() {
        hostId = UUID.randomUUID();
        matchId = UUID.randomUUID();
        guestId = UUID.randomUUID();

        hostUser = User.builder()
                .id(hostId)
                .username("hostPlayer")
                .email("host@rajneeti.com")
                .rating(1000)
                .build();

        guestUser = User.builder()
                .id(guestId)
                .username("guestPlayer")
                .email("guest@rajneeti.com")
                .rating(1000)
                .build();

        room = Room.builder()
                .id(UUID.randomUUID())
                .roomCode("RAJ100")
                .host(hostUser)
                .status(RoomStatus.IN_GAME)
                .maxPlayers(6)
                .build();

        match = Match.builder()
                .id(matchId)
                .room(room)
                .status(MatchStatus.CREATED)
                .currentTurnPlayerId(hostId)
                .turnNumber(1)
                .build();

        hostPlayer = MatchPlayer.builder()
                .id(UUID.randomUUID())
                .match(match)
                .user(hostUser)
                .seatNumber(1)
                .coins(0)
                .coinsAtEnd(0)
                .playerStatus(PlayerStatus.ACTIVE)
                .eliminated(false)
                .build();

        guestPlayer = MatchPlayer.builder()
                .id(UUID.randomUUID())
                .match(match)
                .user(guestUser)
                .seatNumber(2)
                .coins(2)
                .coinsAtEnd(0)
                .playerStatus(PlayerStatus.ACTIVE)
                .eliminated(false)
                .build();
    }

    private ActionRequest taxRequest() {
        return ActionRequest.builder()
                .action(MatchActionType.TAX)
                .claimedCharacter(CharacterType.MINISTER)
                .build();
    }

    private ActionRequest stealRequest() {
        return ActionRequest.builder()
                .action(MatchActionType.STEAL)
                .claimedCharacter(CharacterType.DALAL)
                .targetPlayerId(guestId)
                .build();
    }

    private void stubStealPreconditions(MatchPlayer actor) {
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchIdAndUserId(matchId, hostId)).thenReturn(Optional.of(actor));
        when(matchPlayerRepository.findByMatchIdAndUserId(matchId, guestId)).thenReturn(Optional.of(guestPlayer));
        when(turnManager.isPlayerTurn(matchId, hostId)).thenReturn(true);
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Tax - current player can claim Tax with Minister")
    void performAction_Tax_Success() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchIdAndUserId(matchId, hostId)).thenReturn(Optional.of(hostPlayer));
        when(turnManager.isPlayerTurn(matchId, hostId)).thenReturn(true);
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> inv.getArgument(0));

        MatchActionResponse response = matchService.performAction(matchId, hostId, taxRequest());

        assertThat(response).isNotNull();
        assertThat(response.getAction()).isEqualTo(MatchActionType.TAX);
        assertThat(response.getClaimedCharacter()).isEqualTo(CharacterType.MINISTER);
        assertThat(response.getStatus()).isEqualTo(PendingActionStatus.AWAITING_CHALLENGE);
        assertThat(response.getCoinsToAward()).isEqualTo(3);
        assertThat(response.getActorUserId()).isEqualTo(hostId);
        assertThat(response.getActorUsername()).isEqualTo("hostPlayer");

        assertThat(match.getPendingAction()).isNotNull();
        assertThat(match.getPendingAction().getActionType()).isEqualTo(MatchActionType.TAX);
        assertThat(match.getPendingAction().getStatus()).isEqualTo(PendingActionStatus.AWAITING_CHALLENGE);
        verify(matchRepository).save(match);
    }

    @Test
    @DisplayName("Tax - player does not need to possess Minister (bluff allowed)")
    void performAction_Tax_NoCardRequirement() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchIdAndUserId(matchId, hostId)).thenReturn(Optional.of(hostPlayer));
        when(turnManager.isPlayerTurn(matchId, hostId)).thenReturn(true);
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> inv.getArgument(0));

        // No claimed character provided — the backend defaults the Minister claim
        // and accepts it even though possession is never verified.
        ActionRequest request = ActionRequest.builder().action(MatchActionType.TAX).build();

        MatchActionResponse response = matchService.performAction(matchId, hostId, request);

        assertThat(response).isNotNull();
        assertThat(response.getClaimedCharacter()).isEqualTo(CharacterType.MINISTER);
        assertThat(response.getStatus()).isEqualTo(PendingActionStatus.AWAITING_CHALLENGE);
    }

    @Test
    @DisplayName("Tax - coins are NOT awarded before challenge resolution")
    void performAction_Tax_CoinsNotAwardedBeforeResolution() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchIdAndUserId(matchId, hostId)).thenReturn(Optional.of(hostPlayer));
        when(turnManager.isPlayerTurn(matchId, hostId)).thenReturn(true);
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> inv.getArgument(0));

        matchService.performAction(matchId, hostId, taxRequest());

        assertThat(hostPlayer.getCoins()).isZero();
        assertThat(match.getPendingAction().getStatus()).isEqualTo(PendingActionStatus.AWAITING_CHALLENGE);
        verify(matchPlayerRepository, never()).save(any(MatchPlayer.class));
        verify(turnManager, never()).advanceTurn(matchId);
    }

    @Test
    @DisplayName("Tax - action enters the challenge window")
    void performAction_EntersChallengeWindow() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchIdAndUserId(matchId, hostId)).thenReturn(Optional.of(hostPlayer));
        when(turnManager.isPlayerTurn(matchId, hostId)).thenReturn(true);
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> inv.getArgument(0));

        MatchActionResponse response = matchService.performAction(matchId, hostId, taxRequest());

        assertThat(response.getStatus()).isEqualTo(PendingActionStatus.AWAITING_CHALLENGE);
        assertThat(match.getPendingAction().getCoinsToAward()).isEqualTo(3);
    }

    @Test
    @DisplayName("Tax - non-current player is rejected")
    void performAction_NotYourTurn() {
        UUID otherId = UUID.randomUUID();
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchIdAndUserId(matchId, otherId)).thenReturn(Optional.of(hostPlayer));
        when(turnManager.isPlayerTurn(matchId, otherId)).thenReturn(false);

        assertThatThrownBy(() -> matchService.performAction(matchId, otherId, taxRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo("NOT_YOUR_TURN");
    }

    @Test
    @DisplayName("Tax - player not in the match is rejected")
    void performAction_NotInMatch() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchIdAndUserId(matchId, hostId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.performAction(matchId, hostId, taxRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo("NOT_IN_MATCH");
    }

    @Test
    @DisplayName("Tax - eliminated player is rejected")
    void performAction_EliminatedPlayer() {
        hostPlayer.setPlayerStatus(PlayerStatus.ELIMINATED);
        hostPlayer.setEliminated(true);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchIdAndUserId(matchId, hostId)).thenReturn(Optional.of(hostPlayer));

        assertThatThrownBy(() -> matchService.performAction(matchId, hostId, taxRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo("PLAYER_ELIMINATED");
    }

    @Test
    @DisplayName("Tax - duplicate action rejected while another action is pending")
    void performAction_DuplicateAction() {
        match.setPendingAction(PendingAction.builder()
                .actionType(MatchActionType.TAX)
                .claimedCharacter(CharacterType.MINISTER)
                .actorUserId(hostId)
                .status(PendingActionStatus.AWAITING_CHALLENGE)
                .coinsToAward(3)
                .build());

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchIdAndUserId(matchId, hostId)).thenReturn(Optional.of(hostPlayer));
        when(turnManager.isPlayerTurn(matchId, hostId)).thenReturn(true);

        assertThatThrownBy(() -> matchService.performAction(matchId, hostId, taxRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo("ACTION_PENDING");
    }

    @Test
    @DisplayName("Tax - invalid match rejected")
    void performAction_InvalidMatch() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.performAction(matchId, hostId, taxRequest()))
                .isInstanceOf(MatchNotFoundException.class);
    }

    @Test
    @DisplayName("Tax - match not active rejected")
    void performAction_MatchNotActive() {
        match.setStatus(MatchStatus.FINISHED);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.performAction(matchId, hostId, taxRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo("MATCH_NOT_ACTIVE");
    }

    @Test
    @DisplayName("Steal - current player can steal from a valid target with Dalal")
    void performAction_Steal_Success() {
        stubStealPreconditions(hostPlayer);

        MatchActionResponse response = matchService.performAction(matchId, hostId, stealRequest());

        assertThat(response).isNotNull();
        assertThat(response.getAction()).isEqualTo(MatchActionType.STEAL);
        assertThat(response.getClaimedCharacter()).isEqualTo(CharacterType.DALAL);
        assertThat(response.getActorUserId()).isEqualTo(hostId);
        assertThat(response.getTargetUserId()).isEqualTo(guestId);
        assertThat(response.getStatus()).isEqualTo(PendingActionStatus.AWAITING_CHALLENGE);
        assertThat(response.getCoinsToAward()).isEqualTo(2);

        assertThat(match.getPendingAction()).isNotNull();
        assertThat(match.getPendingAction().getTargetUserId()).isEqualTo(guestId);
        verify(matchRepository).save(match);
    }

    @Test
    @DisplayName("Steal - current player cannot target self")
    void performAction_Steal_TargetSelf() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchIdAndUserId(matchId, hostId)).thenReturn(Optional.of(hostPlayer));
        when(turnManager.isPlayerTurn(matchId, hostId)).thenReturn(true);

        ActionRequest request = ActionRequest.builder()
                .action(MatchActionType.STEAL)
                .claimedCharacter(CharacterType.DALAL)
                .targetPlayerId(hostId)
                .build();

        assertThatThrownBy(() -> matchService.performAction(matchId, hostId, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo("CANNOT_TARGET_SELF");
    }

    @Test
    @DisplayName("Steal - eliminated/inactive target rejected")
    void performAction_Steal_EliminatedTarget() {
        guestPlayer.setPlayerStatus(PlayerStatus.ELIMINATED);
        guestPlayer.setEliminated(true);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchIdAndUserId(matchId, hostId)).thenReturn(Optional.of(hostPlayer));
        when(matchPlayerRepository.findByMatchIdAndUserId(matchId, guestId)).thenReturn(Optional.of(guestPlayer));
        when(turnManager.isPlayerTurn(matchId, hostId)).thenReturn(true);

        assertThatThrownBy(() -> matchService.performAction(matchId, hostId, stealRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo("TARGET_ELIMINATED");
    }

    @Test
    @DisplayName("Steal - invalid target (not in match) rejected")
    void performAction_Steal_InvalidTarget() {
        UUID strangerId = UUID.randomUUID();

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchIdAndUserId(matchId, hostId)).thenReturn(Optional.of(hostPlayer));
        when(matchPlayerRepository.findByMatchIdAndUserId(matchId, strangerId)).thenReturn(Optional.empty());
        when(turnManager.isPlayerTurn(matchId, hostId)).thenReturn(true);

        ActionRequest request = ActionRequest.builder()
                .action(MatchActionType.STEAL)
                .claimedCharacter(CharacterType.DALAL)
                .targetPlayerId(strangerId)
                .build();

        assertThatThrownBy(() -> matchService.performAction(matchId, hostId, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo("INVALID_TARGET");
    }

    @Test
    @DisplayName("Steal - zero-coin target rejected (target has no valid coin state)")
    void performAction_Steal_TargetNoCoins() {
        guestPlayer.setCoins(0);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchIdAndUserId(matchId, hostId)).thenReturn(Optional.of(hostPlayer));
        when(matchPlayerRepository.findByMatchIdAndUserId(matchId, guestId)).thenReturn(Optional.of(guestPlayer));
        when(turnManager.isPlayerTurn(matchId, hostId)).thenReturn(true);

        assertThatThrownBy(() -> matchService.performAction(matchId, hostId, stealRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo("TARGET_NO_COINS");
    }

    @Test
    @DisplayName("Steal - missing target rejected")
    void performAction_Steal_MissingTarget() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchIdAndUserId(matchId, hostId)).thenReturn(Optional.of(hostPlayer));
        when(turnManager.isPlayerTurn(matchId, hostId)).thenReturn(true);

        ActionRequest request = ActionRequest.builder()
                .action(MatchActionType.STEAL)
                .claimedCharacter(CharacterType.DALAL)
                .build();

        assertThatThrownBy(() -> matchService.performAction(matchId, hostId, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo("TARGET_REQUIRED");
    }

    @Test
    @DisplayName("Steal - DALAL ownership is not required (bluff accepted)")
    void performAction_Steal_DalalNotRequired() {
        stubStealPreconditions(hostPlayer);

        ActionRequest request = ActionRequest.builder()
                .action(MatchActionType.STEAL)
                .targetPlayerId(guestId)
                .build();

        MatchActionResponse response = matchService.performAction(matchId, hostId, request);

        assertThat(response).isNotNull();
        assertThat(response.getClaimedCharacter()).isEqualTo(CharacterType.DALAL);
        assertThat(response.getStatus()).isEqualTo(PendingActionStatus.AWAITING_CHALLENGE);
    }

    @Test
    @DisplayName("Steal - no coins transferred before challenge resolution")
    void performAction_Steal_NoTransferBeforeResolution() {
        stubStealPreconditions(hostPlayer);

        matchService.performAction(matchId, hostId, stealRequest());

        assertThat(hostPlayer.getCoins()).isZero();
        assertThat(guestPlayer.getCoins()).isEqualTo(2);
        assertThat(match.getPendingAction().getStatus()).isEqualTo(PendingActionStatus.AWAITING_CHALLENGE);
        verify(matchPlayerRepository, never()).save(any(MatchPlayer.class));
        verify(turnManager, never()).advanceTurn(matchId);
    }
}