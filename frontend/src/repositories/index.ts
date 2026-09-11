import type { AuthRepository } from "./auth-repository";
import type { RoomRepository } from "./room-repository";
import type { GameRepository } from "./game-repository";
import type { MetaRepository } from "./meta-repository";
import { RestAuthRepository } from "./rest/rest-auth-repository";
import { RestRoomRepository } from "./rest/rest-room-repository";
import { RestGameRepository } from "./rest/rest-game-repository";
import { MockMetaRepository } from "./mock/mock-meta-repository";

/**
 * Repository factory. Auth, room and match routes talk to the REST backend.
 * The game repository reads the real backend game state; gameplay actions are
 * still placeholders until their module lands.
 */
export const repositories = {
  auth: new RestAuthRepository() as AuthRepository,
  room: new RestRoomRepository() as RoomRepository,
  game: new RestGameRepository() as GameRepository,
  meta: new MockMetaRepository() as MetaRepository,
};

export type { AuthRepository, RoomRepository, GameRepository, MetaRepository };
export type { LoginInput, RegisterInput, AuthSession, UserPublic } from "@/types/user";
export type { CreateRoomInput, JoinRoomInput, RoomSummary } from "@/types/room";
export type { ActionIntent, GameState, GameResult } from "@/types/game";
export type { LeaderboardEntry, MatchHistoryEntry, ProfileStats } from "@/types/user";
