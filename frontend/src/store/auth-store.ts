"use client";

import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { AuthResult } from "@/services/auth-service";
import type { UserPublic } from "@/types/user";

interface AuthState {
  user: UserPublic | null;
  accessToken: string | null;
  status: "idle" | "loading" | "authenticated" | "unauthenticated";
  setSession: (result: AuthResult) => void;
  setUser: (user: UserPublic) => void;
  clearSession: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      status: "idle",
      setSession: (result) =>
        set({
          user: {
            id: result.user.id,
            username: result.user.username,
            displayName: result.user.displayName,
            avatarInitial: result.user.avatarInitial,
            level: result.user.level ?? 1,
          },
          accessToken: result.session.accessToken,
          status: "authenticated",
        }),
      setUser: (user) => set({ user }),
      clearSession: () =>
        set({ user: null, accessToken: null, status: "unauthenticated" }),
    }),
    {
      name: "rajneeti-auth",
      partialize: (state) => ({
        user: state.user,
        accessToken: state.accessToken,
        status: state.status,
      }),
    },
  ),
);