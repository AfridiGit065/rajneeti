"use client";

import type { ReactNode } from "react";
import { cn } from "@/lib/cn";

export type BadgeTone = "gold" | "emerald" | "crimson" | "neutral" | "parchment";

const TONES: Record<BadgeTone, string> = {
  gold: "bg-gold-500/12 text-gold-300 border-gold-500/35",
  emerald: "bg-forest-500/12 text-forest-300 border-forest-500/35",
  crimson: "bg-crimson-500/12 text-crimson-300 border-crimson-500/35",
  neutral: "bg-deep-700/50 text-muted border-forest-500/20",
  parchment: "bg-parchment-500/10 text-parchment-300 border-parchment-500/30",
};

interface BadgeProps {
  tone?: BadgeTone;
  children: ReactNode;
  className?: string;
}

export function Badge({ tone = "neutral", children, className }: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5",
        "text-xs font-medium tracking-wide",
        TONES[tone],
        className,
      )}
    >
      {children}
    </span>
  );
}