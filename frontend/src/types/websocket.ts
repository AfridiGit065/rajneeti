/** WebSocket + STOMP message contracts (Module 04 backend integration shape). */

export type GameServerMessageType =
  | "GAME_STATE"
  | "STATE_SYNC"
  | "TURN_CHANGED"
  | "ACTION_PERFORMED"
  | "ACTION_BLOCKED"
  | "CHALLENGE_ISSUED"
  | "CHALLENGE_RESOLVED"
  | "CARD_REVEALED"
  | "INFLUENCE_LOST"
  | "PLAYER_ELIMINATED"
  | "GAME_ENDED";

export interface GameServerMessage<T = unknown> {
  type: GameServerMessageType;
  payload: T;
  timestamp: string;
}

export interface ClientCommand<T = unknown> {
  type: string;
  payload: T;
  requestId?: string;
}