"use client";

import { useEffect, useRef } from "react";
import { cn } from "@/lib/cn";
import { CHARACTER_MAP } from "@/lib/game/characters";
import { CHALLENGE_RESPONSE_SECONDS } from "@/lib/game/challenge";
import { useCountdown } from "@/hooks/use-countdown";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Swords, TimerReset, EyeOff } from "@/components/ui/icons";
import type { CharacterId } from "@/types/character";
import type { GamePlayer } from "@/types/game";

interface ChallengePanelProps {
  claimant: GamePlayer;
  claimedCharacter: CharacterId;
  seconds?: number;
  onChallenge: () => void;
  onAllow: () => void;
  className?: string;
}

const ACCENT_CHIP: Record<string, string> = {
  gold: "border-gold-500/45 bg-gold-500/10 text-gold-200",
  crimson: "border-crimson-500/45 bg-crimson-500/10 text-crimson-200",
  forest: "border-forest-500/45 bg-forest-500/10 text-forest-200",
  parchment: "border-parchment-500/40 bg-parchment-500/10 text-parchment-200",
};

export function ChallengePanel({
  claimant,
  claimedCharacter,
  seconds = CHALLENGE_RESPONSE_SECONDS,
  onChallenge,
  onAllow,
  className,
}: ChallengePanelProps) {
  const character = CHARACTER_MAP[claimedCharacter];
  const { display, expired } = useCountdown(seconds);

  const allowFired = useRef(false);
  useEffect(() => {
    if (expired && !allowFired.current) {
      allowFired.current = true;
      onAllow();
    }
  }, [expired, onAllow]);

  return (
    <div
      className={cn(
        "relative overflow-hidden rounded-2xl border border-crimson-500/45 bg-surface panel-emboss animate-fade-up",
        "shadow-[0_0_44px_-14px_rgb(176_58_76/0.55)]",
        className,
      )}
      role="region"
      aria-label={`Challenge Opportunity — ${claimant.displayName ?? claimant.username} claimed: ${character?.nameBn}`}
    >
      <div
        className="pointer-events-none absolute inset-x-0 -top-16 h-36 bg-gradient-to-b from-crimson-500/22 to-transparent blur-2xl animate-glow-pulse"
        aria-hidden
      />

      <div className="relative flex flex-wrap items-center justify-between gap-3 border-b border-crimson-500/25 px-4 py-3">
        <div className="flex items-center gap-2">
          <span className="flex size-8 items-center justify-center rounded-lg border border-crimson-500/50 bg-crimson-600/15">
            <Swords className="size-4 text-crimson-300" aria-hidden />
          </span>
          <div>
            <h2 className="font-cinzel text-sm font-bold text-ivory tracking-wide uppercase">Challenge Window</h2>
            <p className="text-[0.68rem] uppercase tracking-[0.2em] text-muted">
              Opponent Claim Detected
            </p>
          </div>
        </div>

        <span
          className={cn(
            "inline-flex items-center gap-2 rounded-lg border px-3 py-1.5 font-mono text-sm font-semibold tabular-nums",
            expired
              ? "border-crimson-500/60 bg-crimson-600/15 text-crimson-300 animate-glow-pulse"
              : "border-gold-500/40 bg-gold-500/8 text-gold-300",
          )}
          role="timer"
          aria-label={`Time to respond: ${display}`}
        >
          <TimerReset className="size-4" aria-hidden />
          {display}
          <span className="text-xs font-medium text-muted">s</span>
        </span>
      </div>

      <div className="relative grid gap-4 p-4 sm:grid-cols-[auto_minmax(0,1fr)] sm:items-center">
        <div className="flex items-center gap-3">
          <span className="flex size-12 shrink-0 items-center justify-center rounded-full bg-gradient-to-b from-forest-500 to-forest-600 text-lg font-bold text-deep-950">
            {(claimant.displayName ?? claimant.username).slice(0, 1)}
          </span>
          <div className="leading-tight">
            <p className="font-bold text-ivory">{claimant.displayName ?? claimant.username}</p>
            <p className="text-xs text-muted">@{(claimant.username).toLowerCase()}</p>
          </div>
        </div>

        {character ? (
          <div className="rounded-xl border border-gold-500/25 bg-deep-900/70 px-4 py-3">
            <div className="flex items-center justify-between gap-3">
              <p className="text-sm text-muted">
                Claims — <span className="font-semibold text-ivory">&ldquo;I am {character.nameBn}&rdquo;</span>
              </p>
            </div>
            <div className="mt-2 flex flex-wrap items-center gap-2">
              <Badge tone="neutral" className={cn(ACCENT_CHIP[character.accent])}>
                {character.nameBn}
              </Badge>
              <span className="font-cinzel text-[0.65rem] font-semibold uppercase tracking-[0.2em] text-muted">
                {character.nameEn}
              </span>
            </div>
          </div>
        ) : null}
      </div>

      <div className="relative flex flex-col-reverse gap-2 border-t border-crimson-500/25 p-4 sm:flex-row sm:items-center sm:justify-between">
        <p className="flex items-center gap-1.5 text-xs text-muted">
          <EyeOff className="size-3.5" aria-hidden />
          If timer expires, action will be allowed automatically.
        </p>
        <div className="flex gap-2">
          <Button variant="outline" size="md" onClick={onAllow} className="font-cinzel text-xs font-semibold uppercase tracking-wider">
            Allow
          </Button>
          <Button variant="danger" size="md" onClick={onChallenge} className="animate-glow-pulse font-cinzel text-xs font-semibold uppercase tracking-wider">
            <Swords className="size-4" aria-hidden />
            Challenge
          </Button>
        </div>
      </div>
    </div>
  );
}