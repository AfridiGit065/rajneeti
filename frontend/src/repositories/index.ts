import type { AuthRepository } from "./auth-repository";
import type { RoomRepository } from "./room-repository";
import type { GameRepository } from "./game-repository";
import type { MetaRepository } from "./meta-repository";
import { MockAuthRepository } from "./mock/mock-auth-repository";
import { MockRoomRepository } from "./mock/mock-room-repository";
import { MockGameRepository } from "./mock/mock-game-repository";
import { MockMetaRepository } from "./mock/mock-meta-repository";

/**
 * Repository factory. Everything in the UI talks to these interfaces.
 * Swapping mock → REST/WebSocket means changing these constructors only.
 */
export const repositories = {
  auth: new MockAuthRepository() as AuthRepository,
  room: new MockRoomRepository() as RoomRepository,
  game: new MockGameRepository() as GameRepository,
  meta: new MockMetaRepository() as MetaRepository,
};

export type { AuthRepository, RoomRepository, GameRepository, MetaRepository };
export type { LoginInput, RegisterInput, AuthSession, UserPublic } from "@/types/user";
export type { CreateRoomInput, JoinRoomInput, RoomSummary } from "@/types/room";
export type { ActionIntent, GameState, GameResult } from "@/types/game";
export type { LeaderboardEntry, MatchHistoryEntry, ProfileStats } from "@/types/user";