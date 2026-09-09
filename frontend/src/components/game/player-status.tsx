"use client";

import type { ReactNode } from "react";
import { cn } from "@/lib/cn";
import { Check, Crown, UserIcon } from "@/components/ui/icons";

export type PlayerPresence =
  | "online"
  | "away"
  | "in-game"
  | "offline"
  | "eliminated";

export type PlayerTone = "gold" | "crimson" | "forest" | "parchment";

const PRESENCE_META: Record<PlayerPresence, { label: string; dot: string; text: string }> = {
  online: { label: "অনলাইন", dot: "bg-forest-400", text: "text-forest-300" },
  away: { label: "দূরে", dot: "bg-gold-400", text: "text-gold-300" },
  "in-game": { label: "খেলায়", dot: "bg-forest-300", text: "text-parchment-300" },
  offline: { label: "অফলাইন", dot: "bg-deep-650", text: "text-muted" },
  eliminated: { label: "বহিষ্কৃত", dot: "bg-crimson-400", text: "text-crimson-300" },
};

const TONE_RINGS: Record<PlayerTone, string> = {
  gold: "bg-gradient-to-br from-gold-400 to-gold-600 text-deep-950",
  crimson: "bg-gradient-to-br from-crimson-400 to-crimson-700 text-ivory",
  forest: "bg-gradient-to-br from-forest-400 to-deep-700 text-ivory",
  parchment: "bg-gradient-to-br from-parchment-400 to-parchment-600 text-deep-950",
};

interface PlayerStatusProps {
  name: string;
  subText?: string;
  presence?: PlayerPresence;
  tone?: PlayerTone;
  emblem?: ReactNode;
  isHost?: boolean;
  isReady?: boolean;
  seat?: number;
  className?: string;
}

/** Seated-player status chip: presence, host, ready and seat marks. */
export function PlayerStatus({
  name,
  subText,
  presence = "online",
  tone = "forest",
  emblem,
  isHost = false,
  isReady = false,
  seat,
  className,
}: PlayerStatusProps) {
  const meta = PRESENCE_META[presence];
  const eliminated = presence === "eliminated";

  return (
    <div
      className={cn(
        "flex items-center gap-3 rounded-xl border border-forest-500/25 bg-deep-900/70 px-3 py-2.5 panel-emboss",
        eliminated && "opacity-60 saturate-50",
        className,
      )}
    >
      <div className="relative shrink-0">
        <span
          className={cn(
            "flex size-10 items-center justify-center rounded-full border border-forest-500/30 text-sm font-bold shadow-[0_0_0_2px_var(--color-deep-900)]",
            TONE_RINGS[tone],
          )}
        >
          {emblem ?? <UserIcon className="size-4.5" aria-hidden />}
        </span>
        <span
          className={cn(
            "absolute -bottom-0.5 -right-0.5 size-3 rounded-full border-2 border-deep-900",
            meta.dot,
          )}
          aria-hidden
        />
        {isReady ? (
          <span
            className="absolute -left-1 -top-1 flex size-4 items-center justify-center rounded-full bg-gold-400 text-deep-950 shadow-gold"
            aria-label="প্রস্তুত"
          >
            <Check className="size-2.5" strokeWidth={3} aria-hidden />
          </span>
        ) : null}
      </div>

      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-1.5">
          <span className="truncate text-sm font-semibold text-ivory">{name}</span>
          {isHost ? (
            <span className="inline-flex items-center gap-0.5 rounded-full bg-gold-500/12 px-1.5 py-0.5 text-[0.62rem] font-semibold uppercase tracking-wider text-gold-300">
              <Crown className="size-2.5" aria-hidden />
              হোস্ট
            </span>
          ) : null}
        </div>
        {subText ? <p className="truncate text-xs text-muted">{subText}</p> : null}
        <div className="mt-0.5 flex items-center gap-1.5">
          <span aria-hidden>
            <span className={cn("inline-block size-1.5 rounded-full", meta.dot)} />
          </span>
          <span className={cn("text-[0.7rem] font-medium", meta.text)}>{meta.label}</span>
          {seat != null ? (
            <span className="text-[0.7rem] text-muted">• সিট {seat}</span>
          ) : null}
        </div>
      </div>
    </div>
  );
}