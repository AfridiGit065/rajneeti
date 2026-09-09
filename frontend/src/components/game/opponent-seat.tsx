"use client";

import { cn } from "@/lib/cn";
import { CoinDisplay } from "./coin-display";
import { InfluenceDisplay } from "./influence-display";
import { PlayerInfluenceCards } from "./influence-cards";
import { Skull } from "@/components/ui/icons";
import type { GamePlayer } from "@/types/game";

export function OpponentSeat({ player }: { player: GamePlayer }) {
  return (
    <div
      className={cn(
        "flex items-center gap-3 rounded-2xl border bg-surface px-4 py-3 panel-emboss",
        player.isTurn ? "border-gold-500/50 shadow-gold" : "border-forest-500/20",
        !player.isAlive && "opacity-75",
      )}
    >
      <span
        className={cn(
          "relative flex size-10 shrink-0 items-center justify-center rounded-full bg-gradient-to-b from-forest-500 to-forest-600 text-sm font-bold text-deep-950",
          player.isTurn && "ring-2 ring-gold-400/80",
          !player.isAlive && "grayscale",
        )}
        aria-hidden
      >
        {player.displayName?.[0] ?? player.username[0]}
        {player.isTurn ? (
          <span className="absolute -right-0.5 -top-0.5 flex size-3">
            <span className="absolute inset-0 animate-ping rounded-full bg-gold-400/70" />
            <span className="relative size-3 rounded-full border border-deep-950 bg-gold-400" />
          </span>
        ) : null}
      </span>

      <div className="flex min-w-0 flex-1 flex-col gap-1 leading-tight">
        <p className="truncate text-sm font-semibold text-ivory">
          {player.displayName ?? player.username}
          {!player.isAlive ? <span className="ml-1.5 text-xs font-medium text-crimson-300">অপসারিত</span> : null}
        </p>
        <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
          <CoinDisplay coins={player.coins} size="sm" />
          <InfluenceDisplay
            count={player.influenceCards.length}
            revealed={player.influenceCards.filter((c) => c.revealed).length}
            size="sm"
          />
        </div>
        <div className="mt-1.5">
          <PlayerInfluenceCards cards={player.influenceCards} faceDown size="xs" />
        </div>
      </div>

      {!player.isAlive ? (
        <Skull className="size-5 shrink-0 text-crimson-300" aria-hidden />
      ) : null}
    </div>
  );
}