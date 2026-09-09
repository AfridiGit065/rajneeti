import { apiClient } from "@/lib/api-client";
import type { BackendRoom } from "@/types/backend";
import type { RoomRepository } from "../room-repository";
import type { CreateRoomInput, JoinRoomInput, RoomSummary } from "@/types/room";
import { err, type Result } from "@/types/api";

function user(id: string, username: string, avatarUrl = "") {
  return { id, username, displayName: username, avatarInitial: username.slice(0, 2).toUpperCase(), level: 1, avatarUrl };
}

export function toRoomSummary(room: BackendRoom): RoomSummary {
  return {
    roomId: room.id,
    roomCode: room.roomCode,
    name: "",
    host: user(room.hostId, room.hostUsername),
    status: room.status === "IN_GAME" ? "IN_PROGRESS" : room.status,
    maxPlayers: room.maxPlayers,
    players: room.players.map((player) => ({
      playerId: player.id,
      user: user(player.id, player.username, player.avatarUrl),
      seatIndex: player.seatNumber - 1,
      isHost: player.isHost,
      isReady: player.ready,
      joinedAt: player.joinedAt,
    })),
    createdAt: room.createdAt,
  };
}

function mapRoom(result: Result<BackendRoom>): Result<RoomSummary> {
  return result.ok ? { ok: true, data: toRoomSummary(result.data) } : result;
}

export class RestRoomRepository implements RoomRepository {
  async listRooms(): Promise<Result<RoomSummary[]>> {
    const result = await apiClient.get<BackendRoom[]>("/api/rooms");
    return result.ok ? { ok: true, data: result.data.map(toRoomSummary) } : result;
  }
  async getRoomById(roomId: string): Promise<Result<RoomSummary>> { return mapRoom(await apiClient.get<BackendRoom>(`/api/rooms/${roomId}`)); }
  async findRoomByCode(roomCode: string): Promise<Result<RoomSummary>> {
    const rooms = await this.listRooms();
    if (!rooms.ok) return rooms;
    const room = rooms.data.find((item) => item.roomCode === roomCode.trim().toUpperCase());
    return room ? { ok: true, data: room } : err<RoomSummary>({ status: 404, error: "ROOM_NOT_FOUND", message: "Room not found." });
  }
  async createRoom(input: CreateRoomInput): Promise<Result<RoomSummary>> { return mapRoom(await apiClient.post<BackendRoom>("/api/rooms", { maxPlayers: input.maxPlayers })); }
  async joinRoom(input: JoinRoomInput): Promise<Result<RoomSummary>> { return mapRoom(await apiClient.post<BackendRoom>("/api/rooms/join", { roomCode: input.roomCode })); }
  async leaveRoom(roomId: string): Promise<Result<void>> { return apiClient.post<void>(`/api/rooms/${roomId}/leave`); }
  async readyUp(): Promise<Result<RoomSummary>> { throw new Error("Ready state is not part of the Phase 1 frontend integration."); }
  async startGame(): Promise<Result<{ matchId: string }>> { throw new Error("Starting a match is not part of the Phase 1 frontend integration."); }
}
