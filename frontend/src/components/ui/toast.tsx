"use client";

import { useEffect } from "react";
import { cn } from "@/lib/cn";
import { useUiStore, type ToastItem } from "@/store/ui-store";
import { Check, X, AlertTriangle, Info } from "./icons";

const KIND_STYLES = {
  success: {
    ring: "border-forest-400/40",
    icon: <Check className="size-4" aria-hidden />,
    iconWrap: "bg-forest-500/15 text-forest-300",
  },
  error: {
    ring: "border-crimson-400/40",
    icon: <X className="size-4" aria-hidden />,
    iconWrap: "bg-crimson-600/15 text-crimson-300",
  },
  warning: {
    ring: "border-gold-400/40",
    icon: <AlertTriangle className="size-4" aria-hidden />,
    iconWrap: "bg-gold-500/15 text-gold-300",
  },
  info: {
    ring: "border-forest-500/35",
    icon: <Info className="size-4" aria-hidden />,
    iconWrap: "bg-forest-500/10 text-forest-300",
  },
};

export function Toaster() {
  const toasts = useUiStore((s) => s.toasts);
  return (
    <div
      className="pointer-events-none fixed bottom-4 right-4 z-[60] flex w-[calc(100vw-2rem)] max-w-sm flex-col gap-2"
      aria-live="polite"
      aria-label="বিজ্ঞপ্তি"
    >
      {toasts.map((toast) => (
        <ToastCard key={toast.id} toast={toast} />
      ))}
    </div>
  );
}

function ToastCard({ toast }: { toast: ToastItem }) {
  const dismiss = useUiStore((s) => s.dismissToast);
  const style = KIND_STYLES[toast.kind];

  useEffect(() => {
    if (!toast.duration) return;
    const timer = window.setTimeout(() => dismiss(toast.id), toast.duration);
    return () => window.clearTimeout(timer);
  }, [toast.id, toast.duration, dismiss]);

  return (
    <div
      role="status"
      className={cn(
        "pointer-events-auto flex items-start gap-3 rounded-xl border bg-deep-900/95 px-4 py-3 panel-emboss animate-toast-in",
        style.ring,
      )}
    >
      <span
        className={cn(
          "mt-0.5 flex size-6 shrink-0 items-center justify-center rounded-full",
          style.iconWrap,
        )}
      >
        {style.icon}
      </span>
      <div className="min-w-0 flex-1">
        <p className="text-sm font-medium text-ivory">{toast.title}</p>
        {toast.message ? <p className="mt-0.5 text-xs text-muted">{toast.message}</p> : null}
      </div>
      <button
        type="button"
        aria-label="বিজ্ঞপ্তি বন্ধ"
        onClick={() => dismiss(toast.id)}
        className="rounded p-1 text-muted transition-colors hover:text-ivory focus-visible:outline-2 focus-visible:outline-gold-400"
      >
        <X className="size-4" aria-hidden />
      </button>
    </div>
  );
}