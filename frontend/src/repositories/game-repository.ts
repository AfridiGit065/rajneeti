import type { ActionIntent, GameResult, GameState } from "@/types/game";
import type { Result } from "@/types/api";

export interface GameRepository {
  getGameState(matchId: string): Promise<Result<GameState>>;
  performAction(
    matchId: string,
    intent: ActionIntent,
  ): Promise<Result<GameState>>;
  challenge(
    matchId: string,
    targetPlayerId: string,
  ): Promise<Result<GameState>>;
  block(
    matchId: string,
    claimedCharacter: string,
  ): Promise<Result<GameState>>;
  endTurn(matchId: string, playerId: string): Promise<Result<GameState>>;
  getGameResult(matchId: string): Promise<Result<GameResult>>;
  resolveForeignAid(
    matchId: string,
    blocked: boolean,
  ): Promise<Result<GameState>>;
}