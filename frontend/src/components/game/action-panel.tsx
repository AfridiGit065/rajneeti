"use client";

import { ACTIONS } from "@/lib/game/actions";
import { CHARACTER_MAP } from "@/lib/game/characters";
import { cn } from "@/lib/cn";
import { Badge } from "@/components/ui/badge";
import { CoinDisplay } from "./coin-display";
import type { GameAction, GameActionId, GamePlayer } from "@/types/game";

function costTag(action: GameAction): string {
  const parts: string[] = [];
  if (action.requiresCharacter) {
    parts.push(CHARACTER_MAP[action.requiresCharacter]?.nameBn ?? "");
  }
  if (typeof action.cost === "number") {
    parts.push(`${action.cost} কয়েন`);
  } else if (typeof action.gain === "number") {
    parts.push(`+${action.gain}`);
  }
  return parts.filter(Boolean).join(" · ");
}

export function ActionPanel({
  player,
  busy = false,
  onAction,
  className,
}: {
  player: GamePlayer;
  busy?: boolean;
  onAction: (action: GameActionId) => void;
  className?: string;
}) {
  const coins = player.coins;
  const isAlive = player.isAlive;
  const isTurn = player.isTurn;
  const mandatoryCoup = isAlive && isTurn && coins >= 10;

  function isDisabled(action: GameAction): boolean {
    if (!isAlive || !isTurn) return true;
    if (action.id === "coup") return coins < 7;
    if (action.id === "assassinate") return coins < 3;
    return false;
  }

  return (
    <div
      className={cn(
        "rounded-2xl border bg-surface panel-emboss",
        isTurn ? "border-gold-500/30" : "border-forest-500/20",
        className,
      )}
    >
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-forest-500/20 px-4 py-3">
        <h2 className="font-bengali text-lg font-semibold text-ivory">অ্যাকশন</h2>
        {mandatoryCoup ? (
          <Badge tone="crimson">
            বাধ্যতামূলক: ক্ষমতা দখল
          </Badge>
        ) : (
          <CoinDisplay coins={coins} size="sm" />
        )}
      </div>

      <div className="p-4">
        {!isTurn ? (
          <p className="mb-3 text-sm text-muted">
            {isAlive
              ? "আপনার পালা নয় — প্রতিপক্ষের অ্যাকশনের অপেক্ষা করুন।"
              : "আপনি অপসারিত — খেলা এখন দেখা মোডে চলছে।"}
          </p>
        ) : null}

        <div className="grid grid-cols-2 gap-2 sm:grid-cols-4 xl:grid-cols-7">
          {ACTIONS.map((action) => {
            const disabled = isDisabled(action);
            const isCoup = action.id === "coup";
            return (
              <button
                key={action.id}
                type="button"
                disabled={disabled || busy}
                onClick={() => onAction(action.id)}
                title={action.description}
                className={cn(
                  "flex min-h-[3.4rem] flex-col items-start justify-center gap-1 rounded-lg border px-3 py-2 text-left transition-all duration-150",
                  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-400",
                  disabled
                    ? "cursor-not-allowed border-forest-500/15 bg-deep-900/40 text-muted/50"
                    : "border-gold-500/30 bg-deep-800/70 text-ivory hover:border-gold-400 hover:bg-gold-500/10",
                  isCoup && mandatoryCoup &&
                    "border-crimson-500/70 bg-crimson-600/15 text-crimson-200 ring-1 ring-inset ring-crimson-500/50 animate-glow-pulse",
                )}
              >
                <span className="text-sm font-semibold leading-tight">{action.nameBn}</span>
                <span
                  className={cn(
                    "text-[0.65rem] font-medium",
                    disabled ? "text-muted/40" : "text-muted",
                  )}
                >
                  {costTag(action)}
                </span>
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}