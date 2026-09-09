"use client";

import type { ReactNode } from "react";
import { cn } from "@/lib/cn";
import { Landmark } from "@/components/ui/icons";

export type CharacterTone = "gold" | "crimson" | "forest" | "parchment";

const TONE_MEDALLION: Record<CharacterTone, string> = {
  gold: "bg-gradient-to-br from-gold-300 to-gold-600 text-deep-950",
  crimson: "bg-gradient-to-br from-crimson-400 to-crimson-700 text-ivory",
  forest: "bg-gradient-to-br from-forest-400 to-deep-700 text-ivory",
  parchment: "bg-gradient-to-br from-parchment-400 to-parchment-600 text-deep-950",
};

const TONE_BORDERS: Record<CharacterTone, string> = {
  gold: "border-gold-500/40",
  crimson: "border-crimson-500/40",
  forest: "border-forest-500/40",
  parchment: "border-parchment-500/35",
};

interface CharacterBadgeProps {
  name: string;
  enName?: string;
  role?: string;
  tone?: CharacterTone;
  emblem?: ReactNode;
  revealed?: boolean;
  active?: boolean;
  size?: "sm" | "md" | "lg";
  className?: string;
}

const SIZES = {
  sm: { emblem: "size-7 text-sm", name: "text-xs", role: "text-[0.6rem]", gap: "gap-2" },
  md: { emblem: "size-9 text-base", name: "text-sm", role: "text-[0.65rem]", gap: "gap-2.5" },
  lg: { emblem: "size-12 text-xl", name: "text-base", role: "text-xs", gap: "gap-3" },
};

/** Character identity chip with a tone-medallion emblem. */
export function CharacterBadge({
  name,
  enName,
  role,
  tone = "gold",
  emblem,
  revealed = true,
  active = false,
  size = "md",
  className,
}: CharacterBadgeProps) {
  const s = SIZES[size];
  const hidden = !revealed;

  return (
    <div
      className={cn(
        "inline-flex items-center rounded-full border py-1 pr-3 pl-1 bg-deep-900/70 panel-emboss",
        "transition-all duration-150",
        TONE_BORDERS[tone],
        s.gap,
        active && "border-gold-400 ring-2 ring-gold-400/50 shadow-gold",
        hidden && "opacity-75",
        className,
      )}
    >
      <span
        className={cn(
          "flex shrink-0 items-center justify-center rounded-full font-bold",
          "shadow-[0_0_0_2px_var(--color-deep-900)]",
          s.emblem,
          TONE_MEDALLION[tone],
          hidden && "brightness-50 saturate-0",
        )}
        aria-hidden
      >
        {emblem ?? (hidden ? "?" : name.slice(0, 1))}
      </span>
      <span className="flex min-w-0 flex-col">
        <span
          className={cn(
            "truncate font-semibold text-ivory",
            s.name,
            hidden && "tracking-[0.2em] text-muted",
          )}
        >
          {hidden ? "???" : name}
        </span>
        {(enName || role) && !hidden ? (
          <span className={cn("truncate uppercase tracking-[0.14em] text-muted", s.role)}>
            {enName ?? role}
          </span>
        ) : null}
      </span>
    </div>
  );
}

/** Fallback emblem glyph for characters without custom art. */
export function CharacterEmblem({ tone = "gold" }: { tone?: CharacterTone }) {
  return (
    <span className={TONE_MEDALLION[tone]}>
      <Landmark className="size-5" aria-hidden />
    </span>
  );
}