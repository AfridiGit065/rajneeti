"use client";

import type { ReactNode } from "react";
import { cn } from "@/lib/cn";

export type BadgeTone = "gold" | "emerald" | "crimson" | "neutral" | "parchment";
export type BadgeKind = "status" | "player" | "action";

const TONES: Record<BadgeTone, string> = {
  gold: "bg-gold-500/12 text-gold-300 border-gold-500/35",
  emerald: "bg-forest-500/12 text-forest-300 border-forest-500/35",
  crimson: "bg-crimson-500/12 text-crimson-300 border-crimson-500/35",
  neutral: "bg-deep-700/50 text-muted border-forest-500/20",
  parchment: "bg-parchment-500/10 text-parchment-300 border-parchment-500/30",
};

/** Status = compact cap label (default, matches legacy look); player = dot label; action = interactive control. */
const KINDS: Record<BadgeKind, string> = {
  status: "px-2.5 py-0.5 text-xs font-medium tracking-wide",
  player: "px-2 py-0.5 text-xs font-medium tracking-wide",
  action:
    "px-3 py-1 text-xs font-semibold tracking-wide cursor-pointer " +
    "hover:brightness-125 active:brightness-95 transition-[filter] duration-150",
};

interface BadgeProps {
  tone?: BadgeTone;
  kind?: BadgeKind;
  children: ReactNode;
  dot?: boolean;
  className?: string;
}

export function Badge({
  tone = "neutral",
  kind = "status",
  children,
  dot = false,
  className,
}: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full border",
        TONES[tone],
        KINDS[kind],
        className,
      )}
    >
      {dot ? (
        <span
          className={cn(
            "mr-1 inline-block size-1.5 rounded-full bg-current opacity-80",
            kind === "player" && "size-2",
          )}
          aria-hidden
        />
      ) : null}
      {children}
    </span>
  );
}