"use client";

import { create } from "zustand";
import type { RealtimeStatus } from "@/lib/websocket/stomp-client";
import type { ChatMessage } from "@/types/websocket";

export const MAX_CHAT_MESSAGES = 100;

interface DrawnCard {
  matchId: string;
  playerId: string;
  characterId: string;
}

interface RealtimeState {
  status: RealtimeStatus;
  chatMessages: Record<string, ChatMessage[]>;
  errorMessage: string | null;
  lastDraw: DrawnCard | null;
  setStatus: (status: RealtimeStatus) => void;
  pushChat: (roomId: string, message: ChatMessage) => void;
  clearChat: (roomId: string) => void;
  setErrorMessage: (message: string | null) => void;
  setLastDraw: (draw: DrawnCard | null) => void;
}

export const useRealtimeStore = create<RealtimeState>()((set) => ({
  status: "idle",
  chatMessages: {},
  errorMessage: null,
  lastDraw: null,
  setStatus: (status) => set({ status }),
  pushChat: (roomId, message) =>
    set((state) => {
      const current = state.chatMessages[roomId] ?? [];
      const next = [...current, message];
      if (next.length > MAX_CHAT_MESSAGES) next.splice(0, next.length - MAX_CHAT_MESSAGES);
      return { chatMessages: { ...state.chatMessages, [roomId]: next } };
    }),
  clearChat: (roomId) =>
    set((state) => {
      if (!(roomId in state.chatMessages)) return {};
      const chatMessages = { ...state.chatMessages };
      delete chatMessages[roomId];
      return { chatMessages };
    }),
  setErrorMessage: (errorMessage) => set({ errorMessage }),
  setLastDraw: (lastDraw) => set({ lastDraw }),
}));