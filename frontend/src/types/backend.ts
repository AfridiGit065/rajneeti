export interface BackendUser {
  id: string;
  username: string;
  email: string;
  avatarUrl?: string;
  rating: number;
  totalMatches: number;
  wins: number;
  losses: number;
  createdAt?: string;
}

export interface BackendAuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: BackendUser;
}

export interface BackendProfile extends BackendUser {
  winRate: number;
}

export interface BackendRoomPlayer {
  id: string;
  username: string;
  avatarUrl?: string;
  seatNumber: number;
  ready: boolean;
  isHost: boolean;
  joinedAt: string;
}

export interface BackendRoom {
  id: string;
  roomCode: string;
  hostId: string;
  hostUsername: string;
  status: "WAITING" | "IN_GAME" | "FINISHED" | "CANCELLED";
  maxPlayers: number;
  currentPlayers: number;
  canStart: boolean;
  players: BackendRoomPlayer[];
  createdAt: string;
}

export interface BackendMatchPlayer {
  id: string;
  userId: string;
  username: string;
  avatarUrl?: string;
  seatNumber: number;
  playerStatus: "ACTIVE" | "ELIMINATED";
  finalRank?: number;
  coinsAtEnd: number;
  eliminated: boolean;
  eliminatedAt?: string;
}

export interface BackendMatch {
  id: string;
  roomId: string;
  roomCode: string;
  status: "CREATED" | "IN_PROGRESS" | "FINISHED" | "CANCELLED";
  playerCount: number;
  players: BackendMatchPlayer[];
  winnerId?: string;
  winnerUsername?: string;
  currentTurnPlayerId?: string;
  turnNumber?: number;
  turnOrder?: string[];
  startedAt?: string;
  endedAt?: string;
  createdAt: string;
}

export interface BackendTurnInfo {
  matchId: string;
  currentTurnPlayerId?: string;
  turnNumber?: number;
  turnOrder?: string[];
  activePlayerCount?: number;
}

export interface BackendGameCard {
  cardId: string;
  characterId: "minister" | "ghatok" | "dalal" | "amla" | "goyenda";
}

export interface BackendGamePlayer {
  userId: string;
  username: string;
  avatarUrl?: string;
  seatIndex: number;
  status: "ACTIVE" | "ELIMINATED";
  host: boolean;
  alive: boolean;
  turn: boolean;
  coins: number;
  influenceCount: number;
  /** Present only for the requesting player's own hand; null for opponents. */
  cards?: BackendGameCard[];
}

export interface BackendGameLogEntry {
  id: string;
  timestamp: string;
  text: string;
  kind: string;
}

export type BackendGameStatus = "CREATED" | "IN_PROGRESS" | "FINISHED" | "CANCELLED";

export interface BackendPendingAction {
  type: string;
  actorUserId: string;
  startedAt: string;
  /** The claimed character (e.g. "amla" for Exchange, "ghatok" for Assassination). */
  claimedCharacter?: string;
  /** The exchange card pool (only present for the action's own actor). */
  exchangePool?: BackendGameCard[];
  /** The target player for single-target actions (e.g. Assassination). */
  targetPlayerId?: string;
  /** Module 18 — the opponent who challenged a truthful claim (blocks a second challenge). */
  challengerUserId?: string;
}

export interface BackendChallenge {
  challengerId: string;
  claimantId: string;
  actionType: string;
  /** Lower-case character id the claimant asserted, e.g. "minister". */
  claimedCharacter?: string;
  /** "CLAIM_TRUE" when the claimant owned the card, "CLAIM_FALSE" when bluffing. */
  result: "CLAIM_TRUE" | "CLAIM_FALSE";
  /** Physical card id revealed face-up during the challenge. */
  revealedCardId?: string;
  revealedCharacterId?: string;
  influenceLostById: string;
  /** Whether the original action continues after a truthful claim. */
  actionContinues: boolean;
}

export interface BackendGameState {
  matchId: string;
  roomId: string;
  roomCode: string;
  status: BackendGameStatus;
  phase: "setup" | "in_progress" | "game_over";
  hostUserId: string;
  players: BackendGamePlayer[];
  currentTurnPlayerId?: string;
  turnNumber: number;
  turnOrder?: string[];
  deckCount: number;
  revealedCardsCount: number;
  winnerUserId?: string;
  log: BackendGameLogEntry[];
  pendingAction?: BackendPendingAction;
  /** Module 18 — non-null after a challenge resolved for the current action. */
  lastChallenge?: BackendChallenge;
  startedAt: string;
  endedAt?: string;
}
