import type { AuthRepository } from "./auth-repository";
import type { RoomRepository } from "./room-repository";
import type { GameRepository } from "./game-repository";
import type { MetaRepository } from "./meta-repository";
import { RestAuthRepository } from "./rest/rest-auth-repository";
import { RestRoomRepository } from "./rest/rest-room-repository";
import { RestGameRepository } from "./rest/rest-game-repository";
import { RestMetaRepository } from "./rest/rest-meta-repository";

/**
 * Repository factory. All repositories talk to the real REST/WebSocket backend;
 * no mock repositories are wired for the multiplayer flow.
 */
export const repositories = {
  auth: new RestAuthRepository() as AuthRepository,
  room: new RestRoomRepository() as RoomRepository,
  game: new RestGameRepository() as GameRepository,
  meta: new RestMetaRepository() as MetaRepository,
};

export type { AuthRepository, RoomRepository, GameRepository, MetaRepository };
export type { LoginInput, RegisterInput, AuthSession, UserPublic } from "@/types/user";
export type { CreateRoomInput, JoinRoomInput, RoomSummary } from "@/types/room";
export type { ActionIntent, GameState, GameResult } from "@/types/game";
export type { LeaderboardEntry, MatchHistoryEntry, ProfileStats } from "@/types/user";
