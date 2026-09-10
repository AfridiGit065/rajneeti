import { apiClient } from "@/lib/api-client";
import type { BackendGameState, BackendGamePlayer } from "@/types/backend";
import type { GameRepository } from "../game-repository";
import { MatchStatus, type ActionIntent, type GameResult, type GameState, type GamePhase, type InfluenceCard, type GameLogEntry } from "@/types/game";
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

  return {
    matchId: backend.matchId,
    roomId: backend.roomId,
    status: mapStatus(backend.status),
    phase: mapPhase(backend.phase),
    players,
    currentTurnPlayerId,
    turnOrder: backend.turnOrder ?? players.map((p) => p.id),
    turnNumber: backend.turnNumber,
    deckCount: backend.deckCount,
    revealedCardsCount: backend.revealedCardsCount,
    winnerPlayerId: backend.winnerUserId ?? null,
    activeAction: null,
    pendingChallenge: null,
    pendingBlock: null,
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

const notImplemented = (): Result<GameState> =>
  err<GameState>({
    status: 501,
    error: "NOT_IMPLEMENTED",
    message: "Gameplay actions are not implemented in this build yet.",
  });

/**
 * REST-backed game repository.
 *
 * Reads the authoritative, player-safe game state from the backend game
 * engine. Instant gameplay actions (income) resolve against the backend.
 * Actions that open a block/challenge window (foreign aid, steal, ...) belong
 * to a later module and currently resolve to NOT_IMPLEMENTED placeholders.
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
    return notImplemented();
  }

  async challenge(_matchId: string, _targetPlayerId: string): Promise<Result<GameState>> {
    return notImplemented();
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
}