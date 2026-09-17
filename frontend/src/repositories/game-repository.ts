import type { ActionIntent, GameResult, GameState } from "@/types/game";
import type { Result } from "@/types/api";

export interface GameRepository {
  getGameState(matchId: string): Promise<Result<GameState>>;
  performAction(
    matchId: string,
    intent: ActionIntent,
  ): Promise<Result<GameState>>;
  challenge(matchId: string): Promise<Result<GameState>>;
  block(
    matchId: string,
    claimedCharacter: string,
  ): Promise<Result<GameState>>;
  getGameResult(matchId: string): Promise<Result<GameResult>>;
  confirmExchange(
    matchId: string,
    keepCardIds: string[],
  ): Promise<Result<GameState>>;
  /** Module 20 — resolves the pending action authoritatively (no boolean). */
  resolve(matchId: string): Promise<Result<GameState>>;
}