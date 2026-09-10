package com.rajneeti.service;

import com.rajneeti.entity.Match;
import com.rajneeti.entity.MatchPlayer;
import com.rajneeti.entity.User;
import com.rajneeti.entity.enums.MatchStatus;
import com.rajneeti.entity.enums.PlayerStatus;
import com.rajneeti.exception.BusinessException;
import com.rajneeti.exception.MatchNotFoundException;
import com.rajneeti.repository.MatchPlayerRepository;
import com.rajneeti.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Manages turn lifecycle within a match.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Assign the first turn when a match starts</li>
 *   <li>Track the current player</li>
 *   <li>Validate that a player is allowed to act</li>
 *   <li>Determine the next eligible player</li>
 *   <li>Skip eliminated players</li>
 *   <li>Maintain deterministic turn order based on seat numbers</li>
 * </ul>
 *
 * <p>This service does NOT handle gameplay actions (income, tax, steal, etc.).
 * It only manages whose turn it is and turn transitions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TurnManager {

    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;

    /**
     * Assigns the first turn to the player with the lowest seat number.
     * Called when a match is initialized.
     *
     * @param match the match to initialize turns for
     * @param players the match players (must be non-empty)
     */
    @Transactional
    public void assignFirstTurn(Match match, List<MatchPlayer> players) {
        if (players == null || players.isEmpty()) {
            throw new BusinessException("NO_PLAYERS", "Cannot assign first turn: no players in match.");
        }

        MatchPlayer firstPlayer = players.stream()
                .filter(p -> p.getPlayerStatus() == PlayerStatus.ACTIVE)
                .min(Comparator.comparing(MatchPlayer::getSeatNumber))
                .orElseThrow(() -> new BusinessException(
                        "NO_ACTIVE_PLAYERS", "Cannot assign first turn: no active players in match."));

        match.setCurrentTurnPlayerId(firstPlayer.getUser().getId());
        match.setTurnNumber(1);

        matchRepository.save(match);

        log.info("First turn assigned to player '{}' (seat {}) in match {}",
                firstPlayer.getUser().getUsername(), firstPlayer.getSeatNumber(), match.getId());
    }

    /**
     * Returns the player ID of the current turn holder.
     *
     * @param matchId the match ID
     * @return the current turn player's user ID, or null if no turn is active
     */
    @Transactional(readOnly = true)
    public UUID getCurrentTurnPlayerId(UUID matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException("Match not found: " + matchId));
        return match.getCurrentTurnPlayerId();
    }

    /**
     * Validates whether the given player is allowed to act on their turn.
     *
     * @param matchId the match ID
     * @param userId the user attempting to act
     * @return true if the player is the current turn holder and is active
     */
    @Transactional(readOnly = true)
    public boolean isPlayerTurn(UUID matchId, UUID userId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException("Match not found: " + matchId));

        if (!isMatchActive(match)) {
            return false;
        }

        UUID currentTurnPlayerId = match.getCurrentTurnPlayerId();
        if (currentTurnPlayerId == null) {
            return false;
        }

        if (!currentTurnPlayerId.equals(userId)) {
            return false;
        }

        MatchPlayer player = matchPlayerRepository.findByMatchIdAndUserId(matchId, userId)
                .orElse(null);

        return player != null && player.getPlayerStatus() == PlayerStatus.ACTIVE;
    }

    /**
     * Advances the turn to the next eligible player.
     * Skips eliminated players. If only one active player remains, does not create a new turn.
     *
     * @param matchId the match to advance turns in
     * @return the match with updated turn state
     */
    @Transactional
    public Match advanceTurn(UUID matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException("Match not found: " + matchId));

        if (match.getStatus() != MatchStatus.IN_PROGRESS && match.getStatus() != MatchStatus.CREATED) {
            throw new BusinessException("MATCH_NOT_ACTIVE",
                    "Cannot advance turn: match is not active.");
        }

        List<MatchPlayer> players = matchPlayerRepository.findByMatchId(matchId);
        List<MatchPlayer> activePlayers = players.stream()
                .filter(p -> p.getPlayerStatus() == PlayerStatus.ACTIVE)
                .sorted(Comparator.comparing(MatchPlayer::getSeatNumber))
                .toList();

        if (activePlayers.size() <= 1) {
            log.info("Only {} active player(s) remain in match {}. Turn not advanced.",
                    activePlayers.size(), matchId);
            return match;
        }

        UUID currentTurnUserId = match.getCurrentTurnPlayerId();
        int currentTurnNumber = match.getTurnNumber() != null ? match.getTurnNumber() : 1;

        int currentIndex = -1;
        for (int i = 0; i < activePlayers.size(); i++) {
            if (activePlayers.get(i).getUser().getId().equals(currentTurnUserId)) {
                currentIndex = i;
                break;
            }
        }

        int nextIndex;
        if (currentIndex == -1) {
            nextIndex = 0;
        } else {
            nextIndex = (currentIndex + 1) % activePlayers.size();
        }

        MatchPlayer nextPlayer = activePlayers.get(nextIndex);
        match.setCurrentTurnPlayerId(nextPlayer.getUser().getId());
        match.setTurnNumber(currentTurnNumber + 1);

        matchRepository.save(match);

        log.info("Turn advanced to player '{}' (seat {}) in match {}, turn #{}",
                nextPlayer.getUser().getUsername(), nextPlayer.getSeatNumber(),
                matchId, match.getTurnNumber());

        return match;
    }

    /**
     * Returns all match players sorted by seat number (deterministic turn order).
     *
     * @param matchId the match ID
     * @return sorted list of match players
     */
    @Transactional(readOnly = true)
    public List<MatchPlayer> getPlayersInTurnOrder(UUID matchId) {
        return matchPlayerRepository.findByMatchId(matchId).stream()
                .sorted(Comparator.comparing(MatchPlayer::getSeatNumber))
                .toList();
    }

    /**
     * Returns the list of player user IDs in deterministic turn order.
     *
     * @param matchId the match ID
     * @return ordered list of user IDs
     */
    @Transactional(readOnly = true)
    public List<UUID> getTurnOrder(UUID matchId) {
        return getPlayersInTurnOrder(matchId).stream()
                .map(mp -> mp.getUser().getId())
                .toList();
    }

    /**
     * Counts the number of active (non-eliminated) players in a match.
     *
     * @param matchId the match ID
     * @return count of active players
     */
    @Transactional(readOnly = true)
    public long countActivePlayers(UUID matchId) {
        return matchPlayerRepository.findByMatchId(matchId).stream()
                .filter(p -> p.getPlayerStatus() == PlayerStatus.ACTIVE)
                .count();
    }

    /**
     * Checks if the match has only one active player remaining.
     *
     * @param matchId the match ID
     * @return true if exactly one player remains active
     */
    @Transactional(readOnly = true)
    public boolean isSinglePlayerRemaining(UUID matchId) {
        return countActivePlayers(matchId) == 1;
    }

    private boolean isMatchActive(Match match) {
        return match.getStatus() == MatchStatus.IN_PROGRESS || match.getStatus() == MatchStatus.CREATED;
    }
}
