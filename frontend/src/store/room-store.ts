"use client";

import { create } from "zustand";
import type { RoomSummary } from "@/types/room";

interface RoomState {
  rooms: RoomSummary[];
  activeRoom: RoomSummary | null;
  loadingRooms: boolean;
  joining: boolean;
  setRooms: (rooms: RoomSummary[]) => void;
  setActiveRoom: (room: RoomSummary | null) => void;
  upsertRoom: (room: RoomSummary) => void;
  patchActiveRoom: (patch: Partial<RoomSummary>) => void;
  setLoadingRooms: (loading: boolean) => void;
  setJoining: (joining: boolean) => void;
  reset: () => void;
}

export const useRoomStore = create<RoomState>()((set) => ({
  rooms: [],
  activeRoom: null,
  loadingRooms: false,
  joining: false,
  setRooms: (rooms) => set({ rooms }),
  setActiveRoom: (activeRoom) => set({ activeRoom }),
  upsertRoom: (room) =>
    set((state) => {
      const exists = state.rooms.some((r) => r.roomId === room.roomId);
      return {
        rooms: exists
          ? state.rooms.map((r) => (r.roomId === room.roomId ? room : r))
          : [room, ...state.rooms],
      };
    }),
  patchActiveRoom: (patch) =>
    set((state) =>
      state.activeRoom
        ? { activeRoom: { ...state.activeRoom, ...patch } }
        : {},
    ),
  setLoadingRooms: (loadingRooms) => set({ loadingRooms }),
  setJoining: (joining) => set({ joining }),
  reset: () => set({ rooms: [], activeRoom: null }),
}));