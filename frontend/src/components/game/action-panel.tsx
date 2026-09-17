"use client";

import { useState } from "react";
import { ACTIONS } from "@/lib/game/actions";
import { CHARACTER_MAP } from "@/lib/game/characters";
import { cn } from "@/lib/cn";
import {
  Coins,
  Globe,
  ScrollText,
  Hand,
  RefreshCw,
  Skull,
  Crown,
  AlertTriangle,
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
    <div className={cn("select-none", className)}>
      {/* Mandatory Coup Banner if 10+ coins */}
      {mandatoryCoup && (
        <div className="mb-2 flex items-center justify-center gap-2 rounded-xl border border-crimson-500/50 bg-crimson-950/80 px-4 py-1.5 text-xs font-bold text-crimson-200 animate-pulse">
          <AlertTriangle className="size-4 text-crimson-400" aria-hidden />
          <span>১০+ কয়েন সংগৃহীত: অভ্যুত্থান (Coup) বাধ্যতামূলক!</span>
        </div>
      )}

      {/* Action Control Dock */}
      <div className="action-dock rounded-2xl p-1.5 sm:p-2">
        <div className="flex items-center justify-center gap-1 sm:gap-2 overflow-x-auto py-0.5">
          {ACTIONS.map((action) => {
            const disabled = isDisabled(action);
            const isCoup = action.id === "coup";
            const Icon = ACTION_ICONS[action.id];
            const character = action.requiresCharacter ? CHARACTER_MAP[action.requiresCharacter] : null;

            return (
              <button
                key={action.id}
                type="button"
                disabled={disabled || busy}
                onClick={() => handleActionClick(action.id)}
                className={cn(
                  "action-btn group relative flex min-w-[70px] sm:min-w-[92px] flex-col items-center justify-between rounded-xl px-2 py-2 text-center transition-all cursor-pointer",
                  "focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-gold-400",
                  disabled
                    ? "cursor-not-allowed border border-transparent bg-deep-950/40 text-muted/30 opacity-40"
                    : isCoup && mandatoryCoup
                      ? "border border-crimson-500/80 bg-crimson-950/60 text-crimson-200 shadow-crimson ring-1 ring-crimson-400/80 animate-pulse"
                      : "border border-forest-500/20 bg-deep-900/70 hover:border-gold-400 hover:bg-deep-850 hover:shadow-gold",
                )}
                title={`${action.nameEn} (${action.nameBn}) — ${
                  character ? `Claims ${character.nameBn}` : "No character needed"
                }${action.cost ? ` | Cost: ${action.cost} coins` : ""}${action.gain ? ` | Gain: +${action.gain} coins` : ""}`}
              >
                {/* Cost / Gain Pill or Character Tag */}
                <div className="flex items-center justify-between w-full px-0.5 mb-1">
                  <span
                    className={cn(
                      "flex size-5 shrink-0 items-center justify-center rounded-md text-[10px]",
                      action.cost
                        ? "bg-crimson-600/20 text-crimson-300"
                        : action.gain
                          ? "bg-forest-600/20 text-forest-300"
                          : "bg-gold-500/15 text-gold-400",
                    )}
                  >
                    <Icon className="size-3" aria-hidden />
                  </span>

                  {typeof action.gain === "number" && action.gain > 0 ? (
                    <span className="font-mono text-[10px] font-bold text-forest-300">+{action.gain}</span>
                  ) : action.cost ? (
                    <span className="font-mono text-[10px] font-bold text-crimson-300">−{action.cost}</span>
                  ) : (
                    <span className="text-[10px] text-muted/40">•</span>
                  )}
                </div>

                {/* English Name */}
                <span className="text-[11px] sm:text-xs font-bold leading-tight text-ivory group-hover:text-gold-200 transition-colors">
                  {action.nameEn}
                </span>

                {/* Bangla Name */}
                <span className="font-bengali text-[10px] sm:text-[11px] text-muted/80 leading-tight mt-0.5">
                  {action.nameBn}
                </span>

                {/* Claimed Character Pill */}
                {character && !disabled && (
                  <span className="mt-1 inline-block truncate rounded px-1 py-0.2 font-bengali text-[9px] font-medium text-gold-400/90 bg-gold-500/10 border border-gold-500/20">
                    {character.nameBn}
                  </span>
                )}
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