import type { UserPublic } from "./user";

export type RoomStatus = "WAITING" | "IN_PROGRESS" | "FINISHED" | "CANCELLED";

export type BotDifficulty = "EASY" | "MEDIUM" | "HARD";

export interface RoomPlayer {
  playerId: string;
  user: UserPublic;
  seatIndex: number;
  isHost: boolean;
  isReady: boolean;
  isBot: boolean;
  botDifficulty?: BotDifficulty;
  joinedAt: string;
}

export interface RoomSummary {
  roomId: string;
  roomCode: string;
  name: string;
  host: UserPublic;
  status: RoomStatus;
  maxPlayers: number;
  players: RoomPlayer[];
  createdAt: string;
}

export interface CreateRoomInput {
  name: string;
  maxPlayers: number;
  botCount: number;
  botDifficulty: BotDifficulty;
}

export interface JoinRoomInput {
  roomCode: string;
}

export type RoomEventType =
  | "ROOM_CREATED"
  | "PLAYER_JOINED"
  | "PLAYER_LEFT"
  | "PLAYER_READY"
  | "GAME_STARTED"
  | "ROOM_CLOSED";

export interface RoomEvent {
  type: RoomEventType;
  room: RoomSummary;
  actor?: UserPublic;
  timestamp: string;
}
