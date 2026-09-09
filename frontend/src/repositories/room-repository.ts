import type { CreateRoomInput, JoinRoomInput, RoomSummary } from "@/types/room";
import type { Result } from "@/types/api";

export interface RoomRepository {
  listRooms(): Promise<Result<RoomSummary[]>>;
  createRoom(input: CreateRoomInput): Promise<Result<RoomSummary>>;
  joinRoom(input: JoinRoomInput): Promise<Result<RoomSummary>>;
  leaveRoom(roomId: string): Promise<Result<void>>;
  readyUp(roomId: string, ready: boolean): Promise<Result<RoomSummary>>;
  startGame(roomId: string): Promise<Result<{ matchId: string }>>;
}