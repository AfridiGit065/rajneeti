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
  async challenge(matchId: string, targetPlayerId: string): Promise<Result<GameState>> {
    return repositories.game.challenge(matchId, targetPlayerId);
  },
  async block(matchId: string, claimedCharacter: string): Promise<Result<GameState>> {
    return repositories.game.block(matchId, claimedCharacter);
  },
  async endTurn(matchId: string, playerId: string): Promise<Result<GameState>> {
    return repositories.game.endTurn(matchId, playerId);
  },
  async getGameResult(matchId: string): Promise<Result<GameResult>> {
    return repositories.game.getGameResult(matchId);
  },
  async resolveForeignAid(matchId: string, blocked: boolean): Promise<Result<GameState>> {
    return repositories.game.resolveForeignAid(matchId, blocked);
  },
  async confirmExchange(matchId: string, keepCardIds: string[]): Promise<Result<GameState>> {
    return repositories.game.confirmExchange(matchId, keepCardIds);
  },
  async resolveAssassinate(matchId: string, succeeded: boolean): Promise<Result<GameState>> {
    return repositories.game.resolveAssassinate(matchId, succeeded);
  },
};