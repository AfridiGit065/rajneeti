/** WebSocket + STOMP contracts matching the backend WebSocketEvent envelope (Module 22). */

export type WebSocketEventType =
  | "JOIN_ROOM"
  | "LEAVE_ROOM"
  | "READY"
  | "UNREADY"
  | "START_GAME"
  | "PLAYER_ACTION"
  | "TURN_CHANGE"
  | "GAME_OVER"
  | "CHALLENGE"
  | "BLOCK"
  | "CARD_REVEAL"
  | "CHAT_MESSAGE"
  | "WEBSOCKET_ERROR"
  | "ROOM_CREATED"
  | "ROOM_CANCELLED"
  | "HOST_CHANGED"
  | "ROOM_UPDATED";

export interface WebSocketEvent<T = unknown> {
  eventType: WebSocketEventType;
  roomId?: string | null;
  matchId?: string | null;
  senderId?: string | null;
  timestamp: string;
  payload?: T | null;
}

export interface RoomPayload {
  playerId?: string;
  username?: string;
  seatNumber?: number;
  ready?: boolean;
  host?: boolean;
  playerCount?: number;
  newHostId?: string;
  newHostUsername?: string;
}

export interface MatchStartedPayload {
  matchId: string;
  roomId: string;
  hostUserId: string;
  playerCount: number;
}

export interface GameActionPayload {
  actorUserId: string;
  actorUsername: string;
  actionType: string;
  targetUserId?: string | null;
  outcome: string;
}

export interface TurnChangePayload {
  previousTurnPlayerId?: string | null;
  currentTurnPlayerId: string;
  turnNumber: number;
}

export interface GameOverPayload {
  winnerId?: string | null;
  winnerUsername?: string | null;
  endedAt: string;
}

export interface ChallengePayload {
  challengerUserId: string;
  challengerUsername: string;
  claimantUserId: string;
  claimantUsername: string;
  actionType: string;
  claimedCharacter: string;
  claimTrue: boolean;
  actionContinues: boolean;
  blockClaim: boolean;
}

export interface BlockPayload {
  blockerUserId: string;
  blockerUsername: string;
  actorUserId: string;
  actionType: string;
  blockedCharacter: string;
}

export interface CardRevealPayload {
  playerId: string;
  username: string;
  characterId: string;
  reason: "revealed" | "drawn" | string;
}

export interface ChatMessagePayload {
  senderId: string;
  senderUsername: string;
  message: string;
}

export interface WebSocketErrorPayload {
  errorCode: string;
  message: string;
  timestamp: string;
}

export interface ChatMessage {
  id: string;
  senderId: string;
  senderUsername: string;
  message: string;
  timestamp: string;
}

/** Inbound client command over STOMP, e.g. { message } to /app/rooms/{roomId}/chat. */
export type ClientCommand<T = unknown> = T;