"use client";

import { useEffect, useState, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/store/auth-store";
import { repositories } from "@/repositories";

export function RequireAuth({ children }: { children: ReactNode }) {
  const router = useRouter();
  const accessToken = useAuthStore((state) => state.accessToken);
  const setUser = useAuthStore((state) => state.setUser);
  const clearSession = useAuthStore((state) => state.clearSession);
  const [checked, setChecked] = useState(false);
  // A stored session is restored after the first render, so the token is
  // briefly null on a fresh load. Reading that as "signed out" bounced a
  // signed-in player to /login, and that redirect tore down the realtime
  // socket the room page listens on — so the player who joined never received
  // the start-of-match event and never reached the board.
  const [hydrated, setHydrated] = useState(false);

  useEffect(() => {
    if (useAuthStore.persist.hasHydrated()) {
      setHydrated(true);
      return;
    }
    return useAuthStore.persist.onFinishHydration(() => setHydrated(true));
  }, []);

  useEffect(() => {
    // Nothing may be decided until the persisted session has been restored.
    if (!hydrated) return;
    if (!accessToken) {
      router.replace("/login");
      return;
    }
    let cancelled = false;
    repositories.auth.getCurrentUser().then((result) => {
      if (cancelled) return;
      if (result.ok) setUser(result.data);
      else if (result.error.status === 401) {
        clearSession();
        router.replace("/login");
      }
      setChecked(true);
    });
    return () => {
      cancelled = true;
    };
  }, [hydrated, accessToken, clearSession, router, setUser]);

  return checked && accessToken ? children : null;
}