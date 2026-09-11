import { apiClient } from "@/lib/api-client";
import type { BackendGameState, BackendGamePlayer, BackendPendingAction } from "@/types/backend";
import type { GameRepository } from "../game-repository";
import { MatchStatus, type ActionIntent, type ChallengeResolution, type GameActionId, type GameResult, type GameState, type GamePhase, type InfluenceCard, type GameLogEntry } from "@/types/game";
import { err, ok, type Result } from "@/types/api";

const CHARACTER_IDS = ["minister", "ghatok", "dalal", "amla", "goyenda"] as const;

type CharacterId = (typeof CHARACTER_IDS)[number];

function toCharacterId(value: string): CharacterId {
  return CHARACTER_IDS.includes(value as CharacterId) ? (value as CharacterId) : "minister";
}

function mapStatus(status: BackendGameState["status"]): MatchStatus {
  switch (status) {
    case "FINISHED":
      return MatchStatus.FINISHED;
    case "CANCELLED":
      return MatchStatus.ABANDONED;
    case "CREATED":
    case "IN_PROGRESS":
    default:
      return MatchStatus.IN_PROGRESS;
  }
}

function mapPhase(phase: BackendGameState["phase"]): GamePhase {
  switch (phase) {
    case "setup":
      return "setup";
    case "game_over":
      return "game_over";
    case "in_progress":
    default:
      return "action_selection";
  }
}

/**
 * Module 18 — derives the frontend phase (and the challenge state) from the
 * server response. A pending action that claims a character is in its
 * block/challenge window; a resolved challenge is shown until the next action.
 */
function mapChallengeResolution(backend: BackendGameState): ChallengeResolution | null {
  const challenge = backend.lastChallenge;
  if (!challenge) return null;

  const claimedCharacter = challenge.claimedCharacter
    ? toCharacterId(challenge.claimedCharacter)
    : toCharacterId(
        backend.pendingAction?.claimedCharacter ?? "minister",
      );

  return {
    challengerId: challenge.challengerId,
    claimantId: challenge.claimantId,
    claimedCharacter,
    result: challenge.result === "CLAIM_TRUE" ? "failed" : "success",
    revealedCardId: challenge.revealedCardId,
    revealedCharacterId: challenge.revealedCharacterId
      ? toCharacterId(challenge.revealedCharacterId)
      : undefined,
    influenceLostById: challenge.influenceLostById,
    actionContinues: challenge.actionContinues,
    effectApplied: challenge.actionContinues,
  };
}

const LOG_KINDS: GameLogEntry["kind"][] = ["info", "action", "challenge", "block", "reveal", "elimination"];

function toInfluenceCards(player: BackendGamePlayer): InfluenceCard[] {
  const hidden: InfluenceCard[] = Array.from(
    { length: Math.max(0, player.influenceCount) },
    (_, i) => ({
      id: `hidden-${player.userId}-${i}`,
      characterId: "minister",
      revealed: false,
    }),
  );
  const own: InfluenceCard[] =
    (player.cards ?? []).map((card) => ({
      id: card.cardId,
      characterId: toCharacterId(card.characterId),
      revealed: false,
    })) ?? [];

  // The backend only ever sends real cards for the requesting player.
  return own.length > 0 ? own : hidden;
}

function toGameState(backend: BackendGameState): GameState {
  const currentTurnPlayerId = backend.currentTurnPlayerId ?? null;

  const players = backend.players.map((player) => ({
    id: player.userId,
    userId: player.userId,
    username: player.username,
    displayName: player.username,
    isHost: player.host,
    isAlive: player.alive,
    isTurn: player.turn,
    coins: player.coins,
    influenceCards: toInfluenceCards(player),
    seatIndex: player.seatIndex,
  }));

  const exchangePool: InfluenceCard[] | undefined =
    backend.pendingAction?.type === "EXCHANGE" && backend.pendingAction.exchangePool
      ? backend.pendingAction.exchangePool.map((card) => ({
          id: card.cardId,
          characterId: toCharacterId(card.characterId),
          revealed: false,
        }))
      : undefined;

  return {
    matchId: backend.matchId,
    roomId: backend.roomId,
    status: mapStatus(backend.status),
    phase: toPhase(backend),
    players,
    currentTurnPlayerId,
    turnOrder: backend.turnOrder ?? players.map((p) => p.id),
    turnNumber: backend.turnNumber,
    deckCount: backend.deckCount,
    revealedCardsCount: backend.revealedCardsCount,
    winnerPlayerId: backend.winnerUserId ?? null,
    activeAction: mapPendingAction(backend.pendingAction),
    pendingChallenge: mapChallengeResolution(backend),
    pendingBlock: null,
    exchangePool,
    log: backend.log.map((entry) => ({
      id: entry.id,
      timestamp: entry.timestamp,
      text: entry.text,
      kind: LOG_KINDS.includes(entry.kind as GameLogEntry["kind"]) ? (entry.kind as GameLogEntry["kind"]) : "info",
    })),
    startedAt: backend.startedAt,
    endedAt: backend.endedAt,
  };
}

/** A resolved challenge is surfaced until the next action begins. */
function toPhase(backend: BackendGameState): GamePhase {
  if (backend.lastChallenge) return "challenge_resolution";
  if (backend.pendingAction?.claimedCharacter) return "action_resolution";
  return mapPhase(backend.phase);
}

function mapPendingAction(pending: BackendPendingAction | undefined): ActionIntent | null {
  if (!pending) return null;
  const action: GameActionId =
    pending.type === "EXCHANGE"
      ? "exchange"
      : pending.type === "FOREIGN_AID"
      ? "foreign_aid"
      : pending.type === "ASSASSINATE"
      ? "assassinate"
      : pending.type === "TAX"
      ? "tax"
      : pending.type === "STEAL"
      ? "steal"
      : "income";
  const intent: ActionIntent = {
    action,
    claimedCharacter: pending.claimedCharacter
      ? toCharacterId(pending.claimedCharacter)
      : action === "exchange"
      ? "amla"
      : action === "assassinate"
      ? "ghatok"
      : undefined,
  };
  if (pending.targetPlayerId) {
    intent.targetPlayerId = pending.targetPlayerId;
  }
  return intent;
}

const notImplemented = (): Result<GameState> =>
  err<GameState>({
    status: 501,
    error: "NOT_IMPLEMENTED",
    message: "Gameplay actions are not implemented in this build yet.",
  });

/**
 * REST-backed game repository.
 *
 * <p>Reads the authoritative, player-safe game state from the backend game
 * engine. Instant and block-window gameplay actions (income, foreign aid,
 * tax, steal, exchange, assassination) resolve against the backend, and
 * challenges are resolved server-side by the Challenge Manager (Module 18).
 * Blocking is not implemented in this build yet.
 */
export class RestGameRepository implements GameRepository {
  async getGameState(matchId: string): Promise<Result<GameState>> {
    const result = await apiClient.get<BackendGameState>(`/api/matches/${matchId}/game`);
    return result.ok ? { ok: true, data: toGameState(result.data) } : result;
  }

  async performAction(matchId: string, intent: ActionIntent): Promise<Result<GameState>> {
    if (intent.action === "income") {
      const result = await apiClient.post<BackendGameState>(`/api/matches/${matchId}/income`);
      return result.ok ? { ok: true, data: toGameState(result.data) } : result;
    }
    if (intent.action === "foreign_aid") {
      const result = await apiClient.post<BackendGameState>(`/api/matches/${matchId}/foreign-aid`);
      return result.ok ? { ok: true, data: toGameState(result.data) } : result;
    }
    if (intent.action === "exchange") {
      const result = await apiClient.post<BackendGameState>(`/api/matches/${matchId}/exchange`);
      return result.ok ? { ok: true, data: toGameState(result.data) } : result;
    }
    if (intent.action === "assassinate") {
      const result = await apiClient.post<BackendGameState>(
        `/api/matches/${matchId}/assassinate`,
        { targetPlayerId: intent.targetPlayerId },
      );
      return result.ok ? { ok: true, data: toGameState(result.data) } : result;
    }
    if (intent.action === "tax") {
      const result = await apiClient.post<BackendGameState>(`/api/matches/${matchId}/tax`);
      return result.ok ? { ok: true, data: toGameState(result.data) } : result;
    }
    if (intent.action === "steal") {
      const result = await apiClient.post<BackendGameState>(
        `/api/matches/${matchId}/steal`,
        { targetPlayerId: intent.targetPlayerId },
      );
      return result.ok ? { ok: true, data: toGameState(result.data) } : result;
    }
    return notImplemented();
  }

  async challenge(matchId: string): Promise<Result<GameState>> {
    const result = await apiClient.post<BackendGameState>(
      `/api/matches/${matchId}/challenge`,
    );
    return result.ok ? { ok: true, data: toGameState(result.data) } : result;
  }

  async block(_matchId: string, _claimedCharacter: string): Promise<Result<GameState>> {
    return notImplemented();
  }

  async endTurn(_matchId: string, _playerId: string): Promise<Result<GameState>> {
    return notImplemented();
  }

  async getGameResult(matchId: string): Promise<Result<GameResult>> {
    const result = await this.getGameState(matchId);
    if (!result.ok) return result;

    const game = result.data;
    if (!game.winnerPlayerId) {
      return err<GameResult>({
        status: 409,
        error: "GAME_NOT_FINISHED",
        message: "This match has not finished yet.",
      });
    }

    const winner = game.players.find((player) => player.id === game.winnerPlayerId);
    return ok<GameResult>({
      matchId: game.matchId,
      winnerId: game.winnerPlayerId,
      winnerName: winner?.displayName ?? winner?.username ?? "",
      finishedAt: game.endedAt ?? new Date().toISOString(),
      turnCount: game.turnNumber,
    });
  }

  async resolveForeignAid(matchId: string, blocked: boolean): Promise<Result<GameState>> {
    const result = await apiClient.post<BackendGameState>(
      `/api/matches/${matchId}/foreign-aid/resolve?blocked=${blocked}`,
    );
    return result.ok ? { ok: true, data: toGameState(result.data) } : result;
  }

  async confirmExchange(matchId: string, keepCardIds: string[]): Promise<Result<GameState>> {
    const result = await apiClient.post<BackendGameState>(
      `/api/matches/${matchId}/exchange/confirm`,
      { keepCardIds },
    );
    return result.ok ? { ok: true, data: toGameState(result.data) } : result;
  }

  async resolveAssassinate(matchId: string, succeeded: boolean): Promise<Result<GameState>> {
    const result = await apiClient.post<BackendGameState>(
      `/api/matches/${matchId}/assassinate/resolve?succeeded=${succeeded}`,
    );
    return result.ok ? { ok: true, data: toGameState(result.data) } : result;
  }
}