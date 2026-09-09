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

  useEffect(() => {
    if (!accessToken) {
      router.replace("/login");
      return;
    }
    repositories.auth.getCurrentUser().then((result) => {
      if (result.ok) setUser(result.data);
      else if (result.error.status === 401) {
        clearSession();
        router.replace("/login");
      }
      setChecked(true);
    });
  }, [accessToken, clearSession, router, setUser]);

  return checked && accessToken ? children : null;
}
