import type { CharacterId } from "./character";

export type GameActionId =
  | "income"
  | "foreign_aid"
  | "tax"
  | "steal"
  | "exchange"
  | "assassinate"
  | "coup";

export interface GameAction {
  id: GameActionId;
  nameBn: string;
  nameEn: string;
  description: string;
  requiresCharacter?: CharacterId;
  cost?: number;
  gain?: number;
  /** Character ids that may block this action */
  blockableBy?: readonly CharacterId[];
  challengeable: boolean;
  blockable: boolean;
}

export type ActionIntent = {
  action: GameActionId;
  /** Character claimed while performing the action, if any */
  claimedCharacter?: CharacterId;
  /** Target player id, when the action targets another player */
  targetPlayerId?: string;
};

export type ChallengeResult = "success" | "failed";

export interface ChallengeResolution {
  challengerId: string;
  claimantId: string;
  claimedCharacter: CharacterId;
  result: ChallengeResult;
  revealedCardId?: string;
  influenceLostById?: string;
  effectApplied: boolean;
}

export interface BlockResolution {
  blockerId: string;
  actorId: string;
  blockedAction: GameActionId;
  claimedCharacter: CharacterId;
  challenged: boolean;
}

export enum MatchStatus {
  WAITING = "WAITING",
  IN_PROGRESS = "IN_PROGRESS",
  FINISHED = "FINISHED",
  ABANDONED = "ABANDONED",
}

export type GamePhase =
  | "setup"
  | "action_selection"
  | "action_resolution"
  | "challenge_resolution"
  | "block_resolution"
  | "card_reveal"
  | "game_over";

export interface InfluenceCard {
  id: string;
  characterId: CharacterId;
  revealed: boolean;
  revealedAt?: string;
}

export interface CoinBalance {
  coins: number;
}

export interface GamePlayer {
  id: string;
  userId: string;
  username: string;
  displayName?: string;
  isHost: boolean;
  isAlive: boolean;
  isTurn: boolean;
  coins: number;
  /** Only the local player sees their own cards fully */
  influenceCards: InfluenceCard[];
  seatIndex: number;
}

export interface GameLogEntry {
  id: string;
  timestamp: string;
  text: string;
  textBn?: string;
  kind: "info" | "action" | "challenge" | "block" | "reveal" | "elimination";
}

export interface PlayerStatus {
  playerId: string;
  alive: boolean;
  influenceCount: number;
}

export interface GameState {
  matchId: string;
  roomId: string;
  status: MatchStatus;
  phase: GamePhase;
  players: GamePlayer[];
  currentTurnPlayerId: string | null;
  turnOrder: string[];
  turnNumber: number;
  deckCount: number;
  /** Character cards that have been revealed and returned to the deck */
  revealedCardsCount: number;
  winnerPlayerId: string | null;
  activeAction: ActionIntent | null;
  pendingChallenge: ChallengeResolution | null;
  pendingBlock: BlockResolution | null;
  log: GameLogEntry[];
  startedAt: string;
  endedAt?: string;
}

export interface GameResult {
  matchId: string;
  winnerId: string;
  winnerName: string;
  finishedAt: string;
  turnCount: number;
}