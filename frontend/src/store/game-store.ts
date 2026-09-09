"use client";

import { create } from "zustand";
import type { GameState, GamePhase } from "@/types/game";

interface GameStateStore {
  gameState: GameState | null;
  loading: boolean;
  setGameState: (state: GameState) => void;
  mergeGameState: (patch: Partial<GameState>) => void;
  setPhase: (phase: GamePhase) => void;
  setLoading: (loading: boolean) => void;
  reset: () => void;
}

export const useGameStore = create<GameStateStore>()((set) => ({
  gameState: null,
  loading: false,
  setGameState: (gameState) => set({ gameState }),
  mergeGameState: (patch) =>
    set((state) =>
      state.gameState ? { gameState: { ...state.gameState, ...patch } } : {},
    ),
  setPhase: (phase) =>
    set((state) =>
      state.gameState ? { gameState: { ...state.gameState, phase } } : {},
    ),
  setLoading: (loading) => set({ loading }),
  reset: () => set({ gameState: null, loading: false }),
}));