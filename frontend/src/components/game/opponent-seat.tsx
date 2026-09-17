"use client";

import { cn } from "@/lib/cn";
import { Coins, Bot, Skull, Shield } from "@/components/ui/icons";
import { botDifficultyLabel } from "@/lib/game/bot";
import type { GamePlayer } from "@/types/game";

export function OpponentSeat({
  player,
  compact = false,
  className,
}: {
  player: GamePlayer;
  compact?: boolean;
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
        "opponent-seat relative flex flex-col gap-1.5 rounded-2xl p-2 sm:p-2.5 transition-all select-none",
        "bg-[#051913]/85 backdrop-blur-md border",
        isTurn
          ? "border-gold-400/80 shadow-[0_0_20px_rgba(201,165,60,0.35)] ring-1 ring-gold-400/40"
          : "border-forest-500/25 hover:border-gold-500/30",
        !isAlive && "opacity-50 grayscale",
        className,
      )}
      role="region"
      aria-label={`${player.displayName ?? player.username}${player.isBot ? " (AI)" : ""}, ${player.coins} coins, ${activeInfluence} influence`}
    >
      {/* Top Row: Avatar + Name + Badges */}
      <div className="flex items-center gap-2 min-w-0">
        <div className="relative shrink-0">
          <span
            className={cn(
              "flex shrink-0 items-center justify-center rounded-full font-bold text-deep-950 shadow-md transition-all",
              compact ? "size-7 text-xs" : "size-8 sm:size-9 text-xs sm:text-sm",
              isTurn
                ? "bg-gradient-to-b from-gold-300 via-gold-400 to-gold-600 ring-2 ring-gold-300 shadow-gold"
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

        <div className="flex min-w-0 flex-1 flex-col leading-tight">
          <div className="flex items-center gap-1 truncate">
            <span
              className={cn(
                "truncate text-xs font-bold",
                isTurn ? "text-gold-200" : "text-ivory",
                !isAlive && "line-through text-muted/70",
              )}
            >
              {player.displayName ?? player.username}
            </span>

            {isBot && (
              <span
                className="inline-flex shrink-0 items-center gap-0.5 rounded px-1 py-0.2 text-[9px] font-semibold bg-forest-900/80 text-forest-300 border border-forest-500/30"
                title={`Bot (${botDifficultyLabel(player.botDifficulty)})`}
              >
                <Bot className="size-2.5 text-forest-300" aria-hidden />
                <span>AI</span>
              </span>
            )}

            {!isAlive && (
              <span className="ml-auto inline-flex items-center gap-0.5 text-[10px] font-bold uppercase tracking-wider text-crimson-400">
                <Skull className="size-3" aria-hidden />
              </span>
            )}
          </div>

          {/* Bot thinking status or turn indicator */}
          {isTurn && isAlive ? (
            <div className="flex items-center gap-1 text-[10px] font-medium text-gold-300/90 mt-0.5">
              {isBot ? (
                <>
                  <span className="truncate">thinking</span>
                  <span className="flex items-center gap-0.5">
                    <span className="size-1 rounded-full bg-gold-400 animate-dot-1" />
                    <span className="size-1 rounded-full bg-gold-400 animate-dot-2" />
                    <span className="size-1 rounded-full bg-gold-400 animate-dot-3" />
                  </span>
                </>
              ) : (
                <span className="text-gold-400 font-semibold tracking-wide uppercase text-[9px]">
                  Their Turn
                </span>
              )}
            </div>
          ) : (
            <span className="text-[10px] text-muted/60 truncate">
              {activeInfluence > 0 ? `${activeInfluence} cards hidden` : "Eliminated"}
            </span>
          )}
        </div>
      </div>

      {/* Bottom Row: Coins + Influence Shields + Hidden Card Backs */}
      <div className="flex items-center justify-between gap-2 border-t border-forest-500/20 pt-1.5 mt-0.5">
        {/* Coins Pill */}
        <div className="flex items-center gap-1 rounded-md bg-gold-500/10 px-1.5 py-0.5 border border-gold-500/25">
          <Coins className="size-3 text-gold-400 shrink-0" aria-hidden />
          <span className="font-mono text-xs font-bold text-gold-300">{player.coins}</span>
        </div>

        {/* Influence Shields */}
        <div
          className="flex items-center gap-0.5"
          title={`${activeInfluence} active influence (${revealedCount} lost)`}
        >
          {Array.from({ length: totalInfluence }).map((_, i) => {
            const isLost = i >= activeInfluence;
            return (
              <Shield
                key={i}
                className={cn(
                  "size-3 transition-colors",
                  isLost
                    ? "text-crimson-500/40"
                    : "text-forest-400 fill-forest-400/40 drop-shadow-[0_0_3px_rgba(108,191,150,0.5)]",
                )}
                aria-hidden
              />
            );
          })}
        </div>

        {/* Hidden Mini Card Backs (only card backs, never revealing cards!) */}
        <div className="flex items-center gap-1">
          {Array.from({ length: activeInfluence }).map((_, i) => (
            <div
              key={i}
              className="relative flex h-6 w-4.5 items-center justify-center rounded-xs border border-gold-500/50 bg-gradient-to-b from-[#0e3b2e] to-[#061e16] shadow-sm"
              title="Hidden Influence Card"
            >
              <div className="size-2 rounded-full border border-gold-400/60 bg-gold-500/20 flex items-center justify-center">
                <span className="font-bengali text-[6px] font-bold text-gold-400 leading-none">র</span>
              </div>
            </div>
          ))}
          {Array.from({ length: revealedCount }).map((_, i) => (
            <div
              key={`lost-${i}`}
              className="relative flex h-6 w-4.5 items-center justify-center rounded-xs border border-crimson-500/30 bg-crimson-950/40 opacity-50"
              title="Lost Card"
            >
              <span className="text-[7px] text-crimson-400">✕</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}