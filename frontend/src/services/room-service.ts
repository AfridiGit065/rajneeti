import { repositories } from "@/repositories";
import type { CreateRoomInput, JoinRoomInput, RoomSummary } from "@/types/room";
import type { Result } from "@/types/api";

export const RoomService = {
  async listRooms(): Promise<Result<RoomSummary[]>> {
    return repositories.room.listRooms();
  },
  async createRoom(input: CreateRoomInput): Promise<Result<RoomSummary>> {
    return repositories.room.createRoom(input);
  },
  async joinRoom(input: JoinRoomInput): Promise<Result<RoomSummary>> {
    return repositories.room.joinRoom(input);
  },
  async leaveRoom(roomId: string): Promise<Result<void>> {
    return repositories.room.leaveRoom(roomId);
  },
  async setReady(roomId: string, ready: boolean): Promise<Result<RoomSummary>> {
    return repositories.room.readyUp(roomId, ready);
  },
  async startGame(roomId: string): Promise<Result<{ matchId: string }>> {
    return repositories.room.startGame(roomId);
  },
};