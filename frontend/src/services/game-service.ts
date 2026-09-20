import { repositories } from "@/repositories";
import type { ActionIntent, GameResult, GameState } from "@/types/game";
import type { Result } from "@/types/api";

export const GameService = {
  async getGameState(matchId: string): Promise<Result<GameState>> {
    return repositories.game.getGameState(matchId);
  },
  async performAction(
    matchId: string,
    intent: ActionIntent,
  ): Promise<Result<GameState>> {
    return repositories.game.performAction(matchId, intent);
  },
  async challenge(matchId: string): Promise<Result<GameState>> {
    return repositories.game.challenge(matchId);
  },
  async block(matchId: string, claimedCharacter: string): Promise<Result<GameState>> {
    return repositories.game.block(matchId, claimedCharacter);
  },
  async getGameResult(matchId: string): Promise<Result<GameResult>> {
    return repositories.game.getGameResult(matchId);
  },
  async confirmExchange(matchId: string, keepCardIds: string[]): Promise<Result<GameState>> {
    return repositories.game.confirmExchange(matchId, keepCardIds);
  },
  /** Module 20 — resolves the pending action authoritatively (no boolean). */
  async resolve(matchId: string): Promise<Result<GameState>> {
    return repositories.game.resolve(matchId);
  },
};