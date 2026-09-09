"use client";

import type { ReactNode } from "react";
import { Toaster } from "@/components/ui/toast";

export function AppProviders({ children }: { children: ReactNode }) {
  return (
    <>
      {children}
      <Toaster />
    </>
  );
}