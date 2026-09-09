"use client";

import { useEffect, type ReactNode } from "react";
import { Toaster } from "@/components/ui/toast";
import { useUiStore } from "@/store/ui-store";

export function AppProviders({ children }: { children: ReactNode }) {
  const theme = useUiStore((s) => s.settings.theme);
  const reducedMotion = useUiStore((s) => s.settings.reducedMotion);

  useEffect(() => {
    const root = document.documentElement;
    root.dataset.theme = theme;
    root.classList.toggle("reduce-motion", reducedMotion);
  }, [theme, reducedMotion]);

  return (
    <>
      {children}
      <Toaster />
    </>
  );
}