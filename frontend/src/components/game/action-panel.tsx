"use client";

import { useState } from "react";
import { ACTIONS } from "@/lib/game/actions";
import { CHARACTER_MAP } from "@/lib/game/characters";
import { cn } from "@/lib/cn";
import { Badge } from "@/components/ui/badge";
import { CoinDisplay } from "./coin-display";
import {
  Coins,
  Globe,
  ScrollText,
  Hand,
  RefreshCw,
  Skull,
  Crown,
  type LucideIcon,
} from "@/components/ui/icons";
import { ActionModals, type ActionModalStep } from "./action-modals";
import type { GameAction, GameActionId, GamePlayer } from "@/types/game";

const ACTION_ICONS: Record<GameActionId, LucideIcon> = {
  income: Coins,
  foreign_aid: Globe,
  tax: ScrollText,
  steal: Hand,
  exchange: RefreshCw,
  assassinate: Skull,
  coup: Crown,
};

export function ActionPanel({
  player,
  opponents = [],
  busy = false,
  onAction,
  className,
}: {
  player: GamePlayer;
  opponents?: GamePlayer[];
  busy?: boolean;
  onAction: (action: GameActionId, targetPlayerId?: string) => void;
  className?: string;
}) {
  const coins = player.coins;
  const isAlive = player.isAlive;
  const isTurn = player.isTurn;
  const mandatoryCoup = isAlive && isTurn && coins >= 10;

  const [modalStep, setModalStep] = useState<ActionModalStep>({ type: "none" });

  const aliveOpponents = opponents.filter((o) => o.isAlive);

  function isDisabled(action: GameAction): boolean {
    if (!isAlive || !isTurn) return true;
    if (mandatoryCoup && action.id !== "coup") return true;
    if (action.id === "coup") return coins < 7;
    if (action.id === "assassinate") return coins < 3;
    if (action.id === "steal" && aliveOpponents.length > 0 && aliveOpponents.every((o) => o.coins === 0)) return true;
    return false;
  }

  function handleActionClick(actionId: GameActionId) {
    if (actionId === "income") {
      setModalStep({ type: "income_success" });
    } else if (actionId === "foreign_aid") {
      setModalStep({ type: "foreign_aid_block_window" });
    } else if (actionId === "tax") {
      setModalStep({ type: "claim_minister" });
    } else if (actionId === "steal") {
      setModalStep({ type: "select_target_steal" });
    } else if (actionId === "exchange") {
      setModalStep({ type: "exchange_ui" });
    } else if (actionId === "assassinate") {
      setModalStep({ type: "select_target_assassinate" });
    } else if (actionId === "coup") {
      setModalStep({ type: "select_target_coup" });
    }
  }

  function closeModal() {
    setModalStep({ type: "none" });
  }

  return (
    <div
      className={cn(
        "rounded-2xl border bg-surface panel-emboss shadow-lg transition-all",
        isTurn ? "border-gold-500/35" : "border-forest-500/20",
        className,
      )}
    >
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-forest-500/20 px-4 py-3">
        <div className="flex items-center gap-2">
          <h2 className="text-lg font-bold text-ivory">Select Action</h2>
          <span className="font-cinzel text-xs text-muted uppercase tracking-wider">
            (Action System)
          </span>
        </div>
        {mandatoryCoup ? (
          <Badge tone="crimson" className="animate-pulse">
            ⚠️ 10+ Coins: Mandatory Coup!
          </Badge>
        ) : (
          <div className="flex items-center gap-2">
            <span className="text-xs text-muted">Your Treasury:</span>
            <CoinDisplay coins={coins} size="sm" />
          </div>
        )}
      </div>

      <div className="p-4">
        {!isTurn ? (
          <p className="mb-3 text-xs sm:text-sm text-muted">
            {isAlive
              ? "Not your turn — waiting for other players."
              : "You are eliminated — observing match."}
          </p>
        ) : mandatoryCoup ? (
          <p className="mb-3 text-xs sm:text-sm text-crimson-300">
            You hold 10 or more coins. By game rules, you must launch a Coup.
          </p>
        ) : null}

        {/* Action Grid */}
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-4 lg:grid-cols-7">
          {ACTIONS.map((action) => {
            const disabled = isDisabled(action);
            const isCoup = action.id === "coup";
            const Icon = ACTION_ICONS[action.id];
            const character = action.requiresCharacter
              ? CHARACTER_MAP[action.requiresCharacter]
              : null;

            return (
              <button
                key={action.id}
                type="button"
                disabled={disabled || busy}
                onClick={() => handleActionClick(action.id)}
                className={cn(
                  "group relative flex flex-col justify-between rounded-xl border p-3 text-left transition-all duration-200 cursor-pointer select-none",
                  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-400",
                  disabled
                    ? "cursor-not-allowed border-forest-500/15 bg-deep-950/40 text-muted/40 opacity-60"
                    : "border-forest-500/25 bg-deep-850 hover:border-gold-400/80 hover:bg-deep-800 hover:shadow-gold hover:-translate-y-0.5",
                  isCoup && mandatoryCoup &&
                    "border-crimson-500/80 bg-crimson-950/40 text-crimson-200 ring-2 ring-crimson-500/60 animate-pulse",
                )}
              >
                {/* Top: Icon + Name */}
                <div className="space-y-1.5 w-full">
                  <div className="flex items-center justify-between">
                    <span
                      className={cn(
                        "flex size-8 shrink-0 items-center justify-center rounded-lg border transition-colors",
                        action.cost
                          ? "border-crimson-500/30 bg-crimson-600/15 text-crimson-300 group-hover:border-crimson-400"
                          : "border-gold-500/30 bg-gold-500/10 text-gold-400 group-hover:border-gold-400",
                      )}
                    >
                      <Icon className="size-4" aria-hidden />
                    </span>

                    {/* Cost / Gain tag */}
                    {typeof action.gain === "number" && action.gain > 0 ? (
                      <span className="font-cinzel text-xs font-bold text-forest-300">
                        +{action.gain}
                      </span>
                    ) : action.cost ? (
                      <span className="font-cinzel text-xs font-bold text-crimson-300">
                        -{action.cost}
                      </span>
                    ) : (
                      <span className="text-[10px] text-parchment-300">
                        Swap
                      </span>
                    )}
                  </div>

                  <div>
                    <h3 className="text-sm font-bold text-ivory group-hover:text-gold-300 transition-colors">
                      {action.nameEn}
                    </h3>
                    <p className="font-bengali text-[10px] text-muted">
                      {action.nameBn}
                    </p>
                  </div>
                </div>

                {/* Requirement & Chips */}
                <div className="mt-2 pt-2 border-t border-forest-500/15 w-full space-y-1">
                  {character ? (
                    <div className="flex items-center gap-1 text-[10px] text-gold-400">
                      <span>Requires:</span>
                      <span className="font-bold text-gold-300 font-bengali">{character.nameBn}</span>
                    </div>
                  ) : (
                    <div className="text-[10px] text-muted">
                      No Character Required
                    </div>
                  )}

                  <div className="flex flex-wrap gap-1 text-[9px]">
                    <span
                      className={cn(
                        "rounded px-1.5 py-0.2 border",
                        action.challengeable
                          ? "border-gold-500/20 bg-gold-500/10 text-gold-400/90"
                          : "border-white/5 bg-deep-900 text-muted/60",
                      )}
                    >
                      {action.challengeable ? "Challengeable" : "No Challenge"}
                    </span>
                    <span
                      className={cn(
                        "rounded px-1.5 py-0.2 border",
                        action.blockable
                          ? "border-crimson-500/20 bg-crimson-500/10 text-crimson-300/90"
                          : "border-white/5 bg-deep-900 text-muted/60",
                      )}
                    >
                      {action.blockable ? "Blockable" : "Unblockable"}
                    </span>
                  </div>
                </div>
              </button>
            );
          })}
        </div>
      </div>

      <ActionModals
        modalStep={modalStep}
        aliveOpponents={aliveOpponents}
        onClose={closeModal}
        setModalStep={setModalStep}
        onConfirm={onAction}
      />
    </div>
  );
}