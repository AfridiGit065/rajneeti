"use client";

import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { GameSettings } from "@/types/user";

export type ToastKind = "success" | "error" | "info" | "warning";

export interface ToastItem {
  id: string;
  kind: ToastKind;
  title: string;
  message?: string;
  duration?: number;
}

type ModalId =
  | "challenge"
  | "block"
  | "card-reveal"
  | "confirm-leave"
  | "confirm-start"
  | "settings"
  | "rules"
  | null;

interface UiState {
  toasts: ToastItem[];
  modal: ModalId;
  modalPayload: Record<string, unknown>;
  settings: GameSettings;
  pushToast: (toast: Omit<ToastItem, "id">) => void;
  dismissToast: (id: string) => void;
  openModal: (modal: Exclude<ModalId, null>, payload?: Record<string, unknown>) => void;
  closeModal: () => void;
  updateSettings: (patch: Partial<GameSettings>) => void;
}

const DEFAULT_SETTINGS: GameSettings = {
  soundEnabled: true,
  musicEnabled: true,
  notificationsEnabled: true,
  language: "en",
  reducedMotion: false,
  theme: "emerald",
};

export const useUiStore = create<UiState>()(
  persist(
    (set) => ({
      toasts: [],
      modal: null,
      modalPayload: {},
      settings: DEFAULT_SETTINGS,
      pushToast: (toast) =>
        set((state) => {
          const item: ToastItem = {
            ...toast,
            id: `toast-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
            duration: toast.duration ?? 4200,
          };
          return {
            toasts: [...state.toasts, item].slice(-4),
          };
        }),
      dismissToast: (id) =>
        set((state) => ({ toasts: state.toasts.filter((t) => t.id !== id) })),
      openModal: (modal, payload = {}) => set({ modal, modalPayload: payload }),
      closeModal: () => set({ modal: null, modalPayload: {} }),
      updateSettings: (patch) =>
        set((state) => ({ settings: { ...state.settings, ...patch } })),
    }),
    {
      name: "rajneeti-ui",
      partialize: (state) => ({ settings: state.settings }),
    },
  ),
);