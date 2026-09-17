"use client";

import { cn } from "@/lib/cn";
import { Badge } from "@/components/ui/badge";
import { Coins, Shield, Flame, Bot, Skull } from "@/components/ui/icons";
import { botDifficultyLabel } from "@/lib/game/bot";
import type { GamePlayer } from "@/types/game";

export function PlayerSeat({
  player,
  className,
}: {
  player: GamePlayer;
  className?: string;
}) {
  const isTurn = player.isTurn;
  const isAlive = player.isAlive;
  const isBot = player.isBot || player.username.toLowerCase().includes("bot");
  const totalInfluence = player.influenceCards.length;
  const revealedCount = player.influenceCards.filter((c) => c.revealed).length;
  const activeInfluence = totalInfluence - revealedCount;

  return (
    <div
      className={cn(
        "relative flex flex-row items-center justify-between gap-3 rounded-2xl px-3 sm:px-4 py-2 select-none transition-all",
        "bg-[#051913]/90 backdrop-blur-md border",
        isTurn
          ? "border-gold-400/80 shadow-[0_0_24px_rgba(201,165,60,0.35)] ring-1 ring-gold-400/50"
          : "border-forest-500/25",
        !isAlive && "opacity-70 grayscale",
        className,
      )}
    >
      {/* Left: Avatar + Identity */}
      <div className="flex items-center gap-2.5 min-w-0">
        <div className="relative shrink-0">
          <span
            className={cn(
              "flex size-9 sm:size-10 items-center justify-center rounded-full font-bold text-sm shadow-md transition-transform",
              isTurn
                ? "bg-gradient-to-b from-gold-300 via-gold-400 to-gold-600 text-deep-950 ring-2 ring-gold-400 shadow-gold"
                : "bg-gradient-to-b from-forest-400 via-forest-500 to-forest-700 text-ivory",
              !isAlive && "bg-neutral-700 text-neutral-400 ring-0",
            )}
            aria-hidden
          >
            {player.displayName?.[0] ?? player.username[0]}
          </span>
          {isTurn && isAlive && (
            <span className="absolute -right-0.5 -top-0.5 flex size-2.5">
              <span className="absolute inset-0 animate-ping rounded-full bg-gold-400/80" />
              <span className="relative size-2.5 rounded-full border border-deep-950 bg-gold-400" />
            </span>
          )}
        </div>

        <div className="flex min-w-0 flex-col leading-tight">
          <div className="flex items-center gap-1.5">
            <span className={cn("truncate font-bold text-xs sm:text-sm", isTurn ? "text-gold-200" : "text-ivory")}>
              {player.displayName ?? player.username}
            </span>
            <span className="rounded-full bg-gold-500/20 border border-gold-500/40 px-1.5 py-0.2 text-[9px] font-bold text-gold-300 font-cinzel uppercase tracking-wider">
              YOU
            </span>
            {isBot && (
              <Badge tone="parchment" className="shrink-0 text-[9px] py-0">
                <Bot className="size-2.5 mr-0.5" aria-hidden />
                {botDifficultyLabel(player.botDifficulty)}
              </Badge>
            )}
          </div>
          <span className="truncate text-[10px] text-muted">@{player.username}</span>
        </div>
      </div>


      {/* Center/Right: Turn Badge + Coins & Influence Chips */}
      <div className="flex flex-wrap items-center gap-2.5">
        {/* Turn Status */}
        {isTurn ? (
          <span className="inline-flex items-center gap-1 rounded-full border border-gold-500/50 bg-gold-500/15 px-2.5 py-1 text-xs font-bold text-gold-300 animate-glow-pulse">
            <Flame className="size-3 text-gold-400" aria-hidden />
            YOUR TURN
          </span>
        ) : (
          <span className="inline-flex items-center rounded-full border border-forest-500/20 bg-deep-900/60 px-2.5 py-1 text-xs font-medium text-muted">
            Waiting
          </span>
        )}

        {/* Coins Chip */}
        <div className="flex items-center gap-1.5 rounded-xl border border-gold-500/25 bg-deep-950/70 px-3 py-1.5">
          <Coins className="size-4 text-gold-400 shrink-0" aria-hidden />
          <div className="flex flex-col leading-none">
            <span className="text-[9px] uppercase tracking-wider text-muted">Coins</span>
            <span className="font-mono text-sm font-bold text-gold-300">{player.coins}</span>
          </div>
        </div>

        {/* Influence Chip */}
        <div className="flex items-center gap-1.5 rounded-xl border border-forest-500/25 bg-deep-950/70 px-3 py-1.5">
          <Shield className="size-4 text-forest-400 shrink-0" aria-hidden />
          <div className="flex flex-col leading-none">
            <span className="text-[9px] uppercase tracking-wider text-muted">Influence</span>
            <div className="flex items-center gap-1 mt-0.5">
              {Array.from({ length: totalInfluence }).map((_, i) => {
                const isLost = i >= activeInfluence;
                return (
                  <span
                    key={i}
                    className={cn(
                      "inline-block rounded-xs h-3.5 w-2 transition-colors",
                      isLost
                        ? "border border-crimson-500/40 bg-crimson-950/40"
                        : "border border-forest-400/80 bg-forest-400 shadow-[0_0_4px_rgba(108,191,150,0.5)]",
                    )}
                    aria-hidden
                  />
                );
              })}
              <span className="font-mono text-xs font-bold text-ivory ml-1">{activeInfluence}/{totalInfluence}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Eliminated Overlay */}
      {!player.isAlive && (
        <div className="absolute inset-0 flex items-center justify-center rounded-2xl bg-deep-950/80 backdrop-blur-[2px]">
          <span className="flex items-center gap-1.5 rounded-full border border-crimson-500/50 bg-crimson-600/30 px-3 py-1 text-xs font-bold uppercase tracking-wider text-crimson-200">
            <Skull className="size-3.5" aria-hidden />
            Eliminated
          </span>
        </div>
      )}
    </div>
  );
}