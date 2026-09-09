"use client";

import { cn } from "@/lib/cn";

export function LoadingState({
  label = "লোড হচ্ছে…",
  className,
}: {
  label?: string;
  className?: string;
}) {
  return (
    <div
      className={cn(
        "flex min-h-40 flex-col items-center justify-center gap-3 text-muted",
        className,
      )}
      role="status"
      aria-live="polite"
    >
      <span className="relative flex size-10 items-center justify-center">
        <span className="absolute inset-0 animate-ping rounded-full bg-gold-500/20" />
        <span className="relative block size-4 animate-spin rounded-full border-2 border-gold-500/30 border-t-gold-400" />
      </span>
      <p className="text-sm">{label}</p>
    </div>
  );
}