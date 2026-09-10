import type { RoomRepository } from "../room-repository";
import type { CreateRoomInput, JoinRoomInput, RoomSummary } from "@/types/room";
import { err, ok, type Result } from "@/types/api";
import { MOCK_ROOMS } from "@/mocks/rooms";
import { MOCK_CURRENT_USER } from "@/mocks/users";

const delay = (ms = 400) => new Promise((r) => setTimeout(r, ms));

const CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

function generateRoomCode(): string {
  let code = "";
  for (let i = 0; i < 5; i += 1) {
    code += CODE_CHARS[Math.floor(Math.random() * CODE_CHARS.length)];
  }
  return code;
}

export class MockRoomRepository implements RoomRepository {
  private rooms: RoomSummary[] = [...MOCK_ROOMS];

  async listRooms(): Promise<Result<RoomSummary[]>> {
    await delay(300);
    return ok([...this.rooms]);
  }

  async getRoomById(roomId: string): Promise<Result<RoomSummary>> {
    await delay(250);
    const room = this.rooms.find((r) => r.roomId === roomId);
    return room
      ? ok(room)
      : err({ status: 404, error: "ROOM_NOT_FOUND", message: "রুম পাওয়া যায়নি।" });
  }

  async findRoomByCode(roomCode: string): Promise<Result<RoomSummary>> {
    await delay(350);
    const room = this.rooms.find(
      (r) => r.roomCode.toUpperCase() === roomCode.trim().toUpperCase(),
    );
    return room
      ? ok(room)
      : err({
          status: 404,
          error: "ROOM_NOT_FOUND",
          message: "এই কোডের রুম পাওয়া যায়নি।",
        });
  }

  async createRoom(input: CreateRoomInput): Promise<Result<RoomSummary>> {
    await delay(600);
    const room: RoomSummary = {
      roomId: `room-${Date.now()}`,
      roomCode: generateRoomCode(),
      name: input.name,
      host: MOCK_CURRENT_USER,
      status: "WAITING",
      maxPlayers: input.maxPlayers,
      players: [
        {
          playerId: `rp-${Date.now()}`,
          user: MOCK_CURRENT_USER,
          seatIndex: 0,
          isHost: true,
          isReady: false,
          joinedAt: new Date().toISOString(),
        },
      ],
      createdAt: new Date().toISOString(),
    };
    this.rooms = [room, ...this.rooms];
    return ok(room);
  }

  async joinRoom(input: JoinRoomInput): Promise<Result<RoomSummary>> {
    await delay(500);
    const room = this.rooms.find(
      (r) => r.roomCode.toUpperCase() === input.roomCode.toUpperCase(),
    );
    if (!room) {
      return err({
        status: 404,
        error: "ROOM_NOT_FOUND",
        message: "এই কোডের রুম পাওয়া যায়নি।",
      });
    }
    if (room.players.length >= room.maxPlayers) {
      return err({
        status: 409,
        error: "ROOM_FULL",
        message: "রুমটি ইতিমধ্যে পূর্ণ।",
      });
    }
    const alreadyJoined = room.players.some((p) => p.user.id === MOCK_CURRENT_USER.id);
    if (!alreadyJoined) {
      const updated = {
        ...room,
        players: [
          ...room.players,
          {
            playerId: `rp-${Date.now()}`,
            user: MOCK_CURRENT_USER,
            seatIndex: room.players.length,
            isHost: false,
            isReady: false,
            joinedAt: new Date().toISOString(),
          },
        ],
      };
      this.rooms = this.rooms.map((r) => (r.roomId === updated.roomId ? updated : r));
      return ok(updated);
    }
    return ok(room);
  }

  async leaveRoom(roomId: string): Promise<Result<void>> {
    await delay(200);
    this.rooms = this.rooms.filter((r) => r.roomId !== roomId);
    return ok(undefined);
  }

  async readyUp(roomId: string, ready: boolean): Promise<Result<RoomSummary>> {
    await delay(300);
    let found: RoomSummary | null = null;
    this.rooms = this.rooms.map((r) => {
      if (r.roomId !== roomId) return r;
      found = {
        ...r,
        players: r.players.map((p) =>
          p.user.id === MOCK_CURRENT_USER.id ? { ...p, isReady: ready } : p,
        ),
      };
      return found;
    });
    return found
      ? ok(found)
      : err({ status: 404, error: "ROOM_NOT_FOUND", message: "রুম পাওয়া যায়নি।" });
  }

  async startGame(_roomId: string): Promise<Result<{ matchId: string }>> {
    await delay(700);
    const matchId = `match-${Date.now()}`;
    return ok({ matchId });
  }
}