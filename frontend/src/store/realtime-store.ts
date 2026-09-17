"use client";

import { create } from "zustand";
import type { RealtimeStatus } from "@/lib/websocket/stomp-client";
import type { ChatMessage } from "@/types/websocket";
import type { BackendGameState } from "@/types/backend";
import {
  detectVersionGap,
  isSuperseded,
  type GameSnapshotLike,
} from "@/lib/realtime/state-version";

export const MAX_CHAT_MESSAGES = 100;

interface DrawnCard {
  matchId: string;
  playerId: string;
  characterId: string;
}

/** Module 23 — latest authoritative snapshot a client keeps per match. */
export interface GameSnapshot extends GameSnapshotLike {
  state: BackendGameState;
}

interface RealtimeState {
  status: RealtimeStatus;
  chatMessages: Record<string, ChatMessage[]>;
  errorMessage: string | null;
  lastDraw: DrawnCard | null;
  gameSnapshots: Record<string, GameSnapshot>;
  resyncRequested: Record<string, boolean>;
  setStatus: (status: RealtimeStatus) => void;
  pushChat: (roomId: string, message: ChatMessage) => void;
  clearChat: (roomId: string) => void;
  setErrorMessage: (message: string | null) => void;
  setLastDraw: (draw: DrawnCard | null) => void;
  applyPublicState: (matchId: string, state: BackendGameState) => void;
  applyPrivateState: (matchId: string, state: BackendGameState) => void;
  markResyncRequested: (matchId: string) => void;
  consumeResyncRequest: (matchId: string) => boolean;
  resetMatchState: (matchId: string) => void;
}

function mergeSnapshot(
  state: Pick<RealtimeState, "gameSnapshots" | "resyncRequested">,
  matchId: string,
  incoming: BackendGameState,
  scope: "public" | "private",
): Partial<RealtimeState> {
  const stateVersion = incoming.stateVersion ?? 0;
  const current = state.gameSnapshots[matchId];
  const nextKey = { stateVersion, scope };

  // Never regress the board: stale or equal supersets are dropped, while a
  // private snapshot of the same version replaces the public one (cards).
  if (isSuperseded(current, nextKey)) return {};

  const next: GameSnapshot = {
    state: incoming,
    stateVersion,
    scope,
    receivedAt: Date.now(),
  };

  const updated: Partial<RealtimeState> = {
    gameSnapshots: { ...state.gameSnapshots, [matchId]: next },
  };

  // The client jumped ahead of our local revision — we missed a broadcast
  // (e.g. during a disconnect) and should resync to confirm the gap.
  if (detectVersionGap(current, nextKey)) {
    updated.resyncRequested = { ...state.resyncRequested, [matchId]: true };
  }
  return updated;
}

export const useRealtimeStore = create<RealtimeState>()((set) => ({
  status: "idle",
  chatMessages: {},
  errorMessage: null,
  lastDraw: null,
  gameSnapshots: {},
  resyncRequested: {},
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
  applyPublicState: (matchId, state) =>
    set((current) => mergeSnapshot(current, matchId, state, "public")),
  applyPrivateState: (matchId, state) =>
    set((current) => mergeSnapshot(current, matchId, state, "private")),
  markResyncRequested: (matchId) =>
    set((state) => ({
      resyncRequested: { ...state.resyncRequested, [matchId]: true },
    })),
  consumeResyncRequest: (matchId) => {
    let requested = false;
    set((state) => {
      if (!state.resyncRequested[matchId]) return {};
      requested = true;
      const resyncRequested = { ...state.resyncRequested };
      delete resyncRequested[matchId];
      return { resyncRequested };
    });
    return requested;
  },
  resetMatchState: (matchId) =>
    set((state) => {
      const gameSnapshots = { ...state.gameSnapshots };
      delete gameSnapshots[matchId];
      const resyncRequested = { ...state.resyncRequested };
      delete resyncRequested[matchId];
      return { gameSnapshots, resyncRequested };
    }),
}));