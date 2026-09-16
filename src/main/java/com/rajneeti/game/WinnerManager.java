package com.rajneeti.game;

import com.rajneeti.dto.websocket.GameOverPayload;
import com.rajneeti.dto.websocket.WebSocketEventType;
import com.rajneeti.entity.Match;
import com.rajneeti.entity.MatchPlayer;
import com.rajneeti.entity.User;
import com.rajneeti.entity.enums.MatchStatus;
import com.rajneeti.entity.enums.PlayerStatus;
import com.rajneeti.exception.BusinessException;
import com.rajneeti.repository.MatchPlayerRepository;
import com.rajneeti.repository.MatchRepository;
import com.rajneeti.websocket.WebSocketEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Module 21 — Winner Manager.
 *
 * <p>Owns the end-of-game decision. It never mutates coins or influence cards:
 * the {@link GameEngine}, {@link ChallengeManager} and the Module 20
 * {@link ActionResolver} remain the only code that removes an influence card
 * and marks a {@link GamePlayerState} as {@link PlayerStatus#ELIMINATED}. This
 * manager is the single authority that:
 *
 * <ol>
 *   <li><b>synchronises eliminations</b> into the persisted {@code match_players}
 *       rows, so {@link com.rajneeti.service.TurnManager} — which reads the
 *       persisted {@code player_status} — automatically skips every player who
 *       already lost their last influence card;</li>
 *   <li><b>detects game over</b>: exactly one active player left means that
 *       player wins; zero active players is an impossible/corrupt state and is
 *       rejected instead of silently picking a winner;</li>
 *   <li><b>finishes the match</b>: the {@link Match} becomes
 *       {@link MatchStatus#FINISHED} with the winner, an end timestamp and a
 *       final per-player snapshot (status, coins, coins-at-end, final rank),
 *       while the in-memory {@link GameState} becomes {@code game_over} with the
 *       pending action cleared.</li>
 * </ol>
 *
 * <p>Both entry points are idempotent: once the match is finished a repeated
 * call is a no-op, so the check may be invoked from every resolution seam
 * (generic resolver, Coup, challenge/bluff) without ever double-finishing.
 *
 * <p>The in-memory {@link GameStore} stays the authoritative owner of the live
 * game. Persistence here is the final outcome report; the guards tolerate a
 * missing row (already-removed match, unit contexts) rather than corrupting the
 * live state.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WinnerManager {

    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final WebSocketEventPublisher webSocketEventPublisher;

    /**
     * Persists the elimination of every player whose in-memory status is now
     * {@link PlayerStatus#ELIMINATED} but whose {@code match_players} row is
     * still active, assigning a deterministic final rank (elimination order:
     * the first player out gets the highest rank, the winner ends at 1).
     *
     * <p>Must be called <b>before</b> the turn advances so
     * {@link com.rajneeti.service.TurnManager#advanceTurn(UUID)} reads the
     * updated statuses and skips eliminated players.
     *
     * @param state the live game state
     * @return true if at least one elimination was newly recorded
     */
    @Transactional
    public boolean syncPlayerStates(GameState state) {
        if (state == null) {
            return false;
        }

        List<MatchPlayer> persisted = matchPlayerRepository.findByMatchId(state.getMatchId());
        if (persisted == null || persisted.isEmpty()) {
            return false;
        }

        long activeAfter = state.getPlayers().stream()
                .filter(player -> player.getStatus() == PlayerStatus.ACTIVE)
                .count();

        List<MatchPlayer> toSave = new ArrayList<>();
        for (GamePlayerState player : state.getPlayers()) {
            if (player.getStatus() != PlayerStatus.ELIMINATED) {
                continue;
            }
            MatchPlayer row = findRow(persisted, player.getUserId());
            if (row == null || row.getPlayerStatus() == PlayerStatus.ELIMINATED) {
                continue;
            }

            row.setPlayerStatus(PlayerStatus.ELIMINATED);
            row.setEliminated(true);
            row.setEliminatedAt(LocalDateTime.now());
            row.setCoinsAtEnd(player.getCoins());
            row.setFinalRank((int) activeAfter + 1);
            toSave.add(row);

            log.info("Player '{}' recorded as ELIMINATED (rank {}) in match {}",
                    player.getUsername(), row.getFinalRank(), state.getMatchId());
        }

        if (toSave.isEmpty()) {
            return false;
        }
        matchPlayerRepository.saveAll(toSave);
        return true;
    }

    /**
     * Decision point after a resolution that may have removed a player's last
     * influence card. Finishes the match when only one active player remains.
     *
     * <ul>
     *   <li>two or more active players — the game continues (returns false);</li>
     *   <li>exactly one active player — that player wins and the match is
     *       finished (returns true);</li>
     *   <li>no active players — rejected with {@code INVALID_GAME_STATE}.</li>
     * </ul>
     *
     * @param state the live game state
     * @return true if this call finished the game
     * @throws BusinessException {@code INVALID_GAME_STATE} if no player is active
     */
    @Transactional
    public boolean checkAndFinish(GameState state) {
        if (state == null || state.getStatus() == MatchStatus.FINISHED) {
            return false;
        }

        // Keep the persisted statuses current before counting (idempotent).
        syncPlayerStates(state);

        List<GamePlayerState> active = state.getPlayers().stream()
                .filter(player -> player.getStatus() == PlayerStatus.ACTIVE)
                .toList();

        if (active.size() > 1) {
            return false;
        }

        if (active.isEmpty()) {
            throw new BusinessException("INVALID_GAME_STATE",
                    "Cannot determine a winner for match '" + state.getMatchId()
                            + "': no active players remain.");
        }

        finishGame(state, active.get(0));
        return true;
    }

    /**
     * Transitions the match and the live state to their finished form. Callers
     * are expected to have verified exactly one active player.
     */
    private void finishGame(GameState state, GamePlayerState winner) {
        UUID matchId = state.getMatchId();
        LocalDateTime endedAt = LocalDateTime.now();

        Match match = matchRepository.findById(matchId).orElse(null);
        List<MatchPlayer> rows = matchPlayerRepository.findByMatchId(matchId);
        if (rows == null) {
            rows = List.of();
        }

        User winnerUser = rows.stream()
                .filter(row -> row.getUser() != null
                        && winner.getUserId().equals(row.getUser().getId()))
                .map(MatchPlayer::getUser)
                .findFirst()
                .orElse(null);

        // Final per-player snapshot: status, coins, coins-at-end, final rank.
        if (!rows.isEmpty()) {
            for (MatchPlayer row : rows) {
                if (row.getUser() == null) {
                    continue;
                }
                GamePlayerState runtime = state.getPlayers().stream()
                        .filter(player -> player.getUserId().equals(row.getUser().getId()))
                        .findFirst()
                        .orElse(null);
                if (runtime == null) {
                    continue;
                }

                row.setPlayerStatus(runtime.getStatus());
                row.setEliminated(runtime.getStatus() == PlayerStatus.ELIMINATED);
                row.setCoins(runtime.getCoins());
                row.setCoinsAtEnd(runtime.getCoins());
                if (runtime.getStatus() == PlayerStatus.ELIMINATED && row.getEliminatedAt() == null) {
                    row.setEliminatedAt(endedAt);
                }
                if (winner.getUserId().equals(runtime.getUserId())) {
                    row.setFinalRank(1);
                }
            }
            matchPlayerRepository.saveAll(rows);
        }

        if (match != null) {
            match.setStatus(MatchStatus.FINISHED);
            match.setWinner(winnerUser);
            match.setEndedAt(endedAt);
            match.setPendingAction(null);
            matchRepository.save(match);
        } else {
            log.warn("Match row '{}' not found while finishing; live state finished without persistence.",
                    matchId);
        }

        state.setStatus(MatchStatus.FINISHED);
        state.setPhase(GameEngine.PHASE_GAME_OVER);
        state.setWinnerUserId(winner.getUserId());
        state.setEndedAt(endedAt);
        state.setPendingAction(null);
        state.setActionExecuted(false);
        state.getLog().add(GameLogEntry.info(
                winner.getUsername() + " is the last player standing and wins the game!"));

        webSocketEventPublisher.publishToMatch(matchId, WebSocketEventType.GAME_OVER,
                winner.getUserId(), GameOverPayload.builder()
                        .winnerId(winner.getUserId())
                        .winnerUsername(winner.getUsername())
                        .endedAt(endedAt)
                        .build());

        log.info("Game over in match {}: winner '{}' ({} coins, {} influence card(s))",
                matchId, winner.getUsername(), winner.getCoins(),
                winner.getCards() != null ? winner.getCards().size() : 0);
    }

    private MatchPlayer findRow(List<MatchPlayer> rows, UUID userId) {
        return rows.stream()
                .filter(row -> row.getUser() != null && userId.equals(row.getUser().getId()))
                .findFirst()
                .orElse(null);
    }
}
