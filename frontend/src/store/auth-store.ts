"use client";

import { create } from "zustand";
import { persist } from "zustand/middleware";
import { AuthService, type AuthResult } from "@/services/auth-service";
import type { LoginInput, UserPublic } from "@/types/user";
import type { Result } from "@/types/api";

interface AuthState {
  user: UserPublic | null;
  accessToken: string | null;
  status: "idle" | "loading" | "authenticated" | "unauthenticated";
  remember: boolean;
  login: (input: LoginInput, remember: boolean) => Promise<Result<AuthResult>>;
  logout: () => Promise<void>;
  setSession: (result: AuthResult) => void;
  setUser: (user: UserPublic) => void;
  clearSession: () => void;
  setRemember: (remember: boolean) => void;
}

function toSessionState(result: AuthResult) {
  return {
    user: {
      id: result.user.id,
      username: result.user.username,
      displayName: result.user.displayName,
      avatarInitial: result.user.avatarInitial,
      level: result.user.level ?? 1,
    },
    accessToken: result.session.accessToken,
    status: "authenticated" as const,
  };
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      status: "idle",
      remember: false,
      login: async (input, remember) => {
        const result = await AuthService.login(input);
        if (!result.ok) return result;
        set({ remember, ...toSessionState(result.data) });
        return result;
      },
      logout: async () => {
        await AuthService.logout();
        useAuthStore.persist.clearStorage();
        set({ user: null, accessToken: null, status: "unauthenticated", remember: false });
      },
      setSession: (result) => set({ ...toSessionState(result) }),
      setUser: (user) => set({ user }),
      clearSession: () =>
        set({ user: null, accessToken: null, status: "unauthenticated" }),
      setRemember: (remember) => set({ remember }),
    }),
    {
      name: "rajneeti-auth",
      partialize: (state) =>
        state.remember
          ? { remember: true, user: state.user, accessToken: state.accessToken, status: state.status }
          : { remember: false },
    },
  ),
);