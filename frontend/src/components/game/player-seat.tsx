"use client";

import { cn } from "@/lib/cn";
import { Badge } from "@/components/ui/badge";
import { CoinDisplay } from "./coin-display";
import { InfluenceDisplay } from "./influence-display";
import { Skull } from "@/components/ui/icons";
import type { GamePlayer } from "@/types/game";

export function PlayerSeat({ player }: { player: GamePlayer }) {
  return (
    <div
      className={cn(
        "relative overflow-hidden rounded-2xl border bg-surface p-5 panel-emboss",
        player.isTurn ? "border-gold-500/50 shadow-gold" : "border-forest-500/25",
      )}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="flex min-w-0 items-center gap-3">
          <span
            className={cn(
              "flex size-12 shrink-0 items-center justify-center rounded-full bg-gradient-to-b from-forest-500 to-forest-600 text-lg font-bold text-deep-950",
              player.isTurn && "ring-2 ring-gold-400/80",
            )}
            aria-hidden
          >
            {player.displayName?.[0] ?? player.username[0]}
          </span>
          <span className="flex min-w-0 flex-col leading-tight">
            <span className="truncate font-bold text-ivory">
              {player.displayName ?? player.username}
            </span>
            <span className="truncate text-xs text-muted">@{player.username}</span>
          </span>
        </div>
        {player.isTurn ? <Badge tone="gold">আপনার পালা</Badge> : <Badge tone="neutral">অপেক্ষা</Badge>}
      </div>

      <div className="mt-4 grid grid-cols-2 gap-3">
        <div className="rounded-xl border border-gold-500/20 bg-deep-800/70 px-3 py-2.5">
          <p className="text-[0.68rem] font-semibold uppercase tracking-wider text-muted">কয়েন</p>
          <CoinDisplay coins={player.coins} size="lg" className="mt-1" />
        </div>
        <div className="rounded-xl border border-forest-500/20 bg-deep-800/70 px-3 py-2.5">
          <p className="text-[0.68rem] font-semibold uppercase tracking-wider text-muted">
            ইনফ্লুয়েন্স
          </p>
          <InfluenceDisplay
            count={player.influenceCards.length}
            revealed={player.influenceCards.filter((c) => c.revealed).length}
            size="lg"
            className="mt-1"
          />
        </div>
      </div>

      {!player.isAlive ? (
        <div className="absolute inset-0 flex items-center justify-center rounded-2xl bg-deep-950/75 backdrop-blur-[2px]">
          <span className="flex items-center gap-2 rounded-full border border-crimson-500/50 bg-crimson-600/30 px-3 py-1.5 text-sm font-semibold text-crimson-200">
            <Skull className="size-4" aria-hidden />
            অপসারিত
          </span>
        </div>
      ) : null}
    </div>
  );
}