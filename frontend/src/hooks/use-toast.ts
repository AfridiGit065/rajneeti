"use client";

import { useSyncExternalStore } from "react";
import { useUiStore } from "@/store/ui-store";
import type { ToastKind } from "@/store/ui-store";

export function useToast() {
  const pushToast = useUiStore((s) => s.pushToast);
  const dismissToast = useUiStore((s) => s.dismissToast);

  return {
    toast: (kind: ToastKind, title: string, message?: string) =>
      pushToast({ kind, title, message }),
    success: (title: string, message?: string) =>
      pushToast({ kind: "success", title, message }),
    error: (title: string, message?: string) =>
      pushToast({ kind: "error", title, message }),
    info: (title: string, message?: string) =>
      pushToast({ kind: "info", title, message }),
    warning: (title: string, message?: string) =>
      pushToast({ kind: "warning", title, message }),
    dismiss: dismissToast,
  };
}

const subscribeToMount = () => () => {};

/** True once the component has mounted on the client (avoids hydration mismatches). */
export function useMounted(): boolean {
  return useSyncExternalStore(
    subscribeToMount,
    () => true,
    () => false,
  );
}