import { apiClient } from "@/lib/api-client";
import type { BackendBlockRequest, BackendGameState } from "@/types/backend";
import { toGameState } from "@/lib/game/backend-game-state";
import type { GameRepository } from "../game-repository";
import { type ActionIntent, type GameResult, type GameState } from "@/types/game";
import { err, ok, type Result } from "@/types/api";

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
 * tax, steal, exchange, assassination) resolve against the backend. Action
 * claims are resolved server-side by the Challenge Manager (Module 18) and
 * block claims by the Block Manager (Module 19); a block itself can be
 * challenged through the same <code>/challenge</code> seam.
 *
 * <p>Module 23 — responses carry the same {@code stateVersion} the realtime
 * snapshots use, so REST and WebSocket views stay interchangeable.
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
    if (intent.action === "coup") {
      const result = await apiClient.post<BackendGameState>(
        `/api/matches/${matchId}/coup`,
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

  async block(matchId: string, claimedCharacter: string): Promise<Result<GameState>> {
    const payload: BackendBlockRequest = { claimedCharacter };
    const result = await apiClient.post<BackendGameState>(
      `/api/matches/${matchId}/block`,
      payload,
    );
    return result.ok ? { ok: true, data: toGameState(result.data) } : result;
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

  async resolveSteal(matchId: string, granted: boolean): Promise<Result<GameState>> {
    const result = await apiClient.post<BackendGameState>(
      `/api/matches/${matchId}/steal/resolve?granted=${granted}`,
    );
    return result.ok ? { ok: true, data: toGameState(result.data) } : result;
  }

  async resolve(matchId: string): Promise<Result<GameState>> {
    const result = await apiClient.post<BackendGameState>(
      `/api/matches/${matchId}/resolve`,
    );
    return result.ok ? { ok: true, data: toGameState(result.data) } : result;
  }
}