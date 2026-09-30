"use client";

import Image from "next/image";
import { cn } from "@/lib/cn";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Coins,
  Globe,
  ScrollText,
  Hand,
  Skull,
  Crown,
  AlertTriangle,
  Users,
  X,
} from "@/components/ui/icons";
import type { GameActionId, GamePlayer } from "@/types/game";

export type ActionModalStep =
  | { type: "none" }
  | { type: "income_success" }
  | { type: "foreign_aid_block_window" }
  | { type: "claim_minister" }
  | { type: "select_target_steal" }
  | { type: "claim_dalal"; targetPlayer: GamePlayer }
  | { type: "exchange_ui" }
  | { type: "select_target_assassinate" }
  | { type: "confirm_assassinate"; targetPlayer: GamePlayer }
  | { type: "select_target_coup" }
  | { type: "confirm_coup"; targetPlayer: GamePlayer };

interface ActionModalsProps {
  modalStep: ActionModalStep;
  aliveOpponents: GamePlayer[];
  onClose: () => void;
  setModalStep: (step: ActionModalStep) => void;
  onConfirm: (action: GameActionId, targetPlayerId?: string) => void;
}

export function ActionModals({
  modalStep,
  aliveOpponents,
  onClose,
  setModalStep,
  onConfirm,
}: ActionModalsProps) {
  if (modalStep.type === "none") return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in"
      role="dialog"
      aria-modal="true"
    >
      <div className="relative w-full max-w-lg rounded-3xl border-2 border-gold-500/40 bg-surface panel-emboss panel-texture p-6 sm:p-7 shadow-2xl space-y-5">
        <button
          type="button"
          onClick={onClose}
          className="absolute right-4 top-4 flex size-8 items-center justify-center rounded-full border border-white/10 bg-deep-950/80 text-muted hover:text-ivory transition-all cursor-pointer"
        >
          <X className="size-4" />
        </button>

        {/* 1. Income Success State */}
        {modalStep.type === "income_success" && (
          <div className="space-y-4 text-center">
            <div className="mx-auto flex size-14 items-center justify-center rounded-2xl border border-forest-500/40 bg-forest-500/15 text-forest-300 shadow-lg">
              <Coins className="size-8" />
            </div>
            <div>
              <h3 className="font-display text-2xl font-bold text-ivory">
                Income
              </h3>
              <p className="font-cinzel text-xs text-muted tracking-widest mt-0.5">
                ACTION: INCOME (+1 COIN)
              </p>
            </div>
            <div className="rounded-xl border border-forest-500/25 bg-deep-900/70 p-4 text-xs text-parchment-200 leading-relaxed">
              Income is a basic action. It does not claim any character and cannot be challenged or blocked. +1 coin will be added to your treasury directly.
            </div>
            <div className="flex gap-2">
              <Button variant="ghost" fullWidth onClick={onClose}>
                Cancel
              </Button>
              <Button
                variant="premium"
                fullWidth
                onClick={() => {
                  onConfirm("income");
                  onClose();
                }}
              >
                Confirm (+1 Coin)
              </Button>
            </div>
          </div>
        )}

        {/* 2. Foreign Aid Block State */}
        {modalStep.type === "foreign_aid_block_window" && (
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <span className="flex size-12 shrink-0 items-center justify-center rounded-2xl border border-gold-500/40 bg-gold-500/15 text-gold-300">
                <Globe className="size-6" />
              </span>
              <div>
                <h3 className="font-display text-xl font-bold text-ivory">
                  Foreign Aid
                </h3>
                <p className="font-cinzel text-xs text-muted">
                  +2 Coins · Blockable by Minister
                </p>
              </div>
            </div>

            <div className="rounded-xl border border-gold-500/30 bg-deep-950/70 p-4 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-gold-300">
                  Block Opportunity Window:
                </span>
                <Badge tone="crimson">Blockable by Minister</Badge>
              </div>
              <p className="text-xs text-parchment-300 leading-relaxed">
                You are claiming 2 coins from the treasury. Any player claiming <strong className="text-ivory">Minister (মন্ত্রী)</strong> may attempt to block this foreign aid.
              </p>
            </div>

            <div className="flex gap-2">
              <Button variant="ghost" fullWidth onClick={onClose}>
                Cancel
              </Button>
              <Button
                variant="premium"
                fullWidth
                onClick={() => {
                  onConfirm("foreign_aid");
                  onClose();
                }}
              >
                Claim Foreign Aid (+2)
              </Button>
            </div>
          </div>
        )}

        {/* 3. Tax Claim Minister State */}
        {modalStep.type === "claim_minister" && (
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <span className="flex size-12 shrink-0 items-center justify-center rounded-2xl border border-gold-500/40 bg-gold-500/15 text-gold-300">
                <ScrollText className="size-6" />
              </span>
              <div>
                <h3 className="font-display text-xl font-bold text-ivory">
                  Tax
                </h3>
                <p className="font-cinzel text-xs text-muted">
                  +3 Coins · Requires Minister Claim
                </p>
              </div>
            </div>

            <div className="rounded-xl border border-gold-500/40 bg-gradient-to-b from-deep-900 to-deep-950 p-4 space-y-3">
              <div className="flex items-center gap-3">
                <div className="relative size-12 rounded-lg overflow-hidden border border-gold-400 shrink-0">
                  <Image
                    src="/assets/cards/minister.png"
                    alt="Minister"
                    fill
                    className="object-cover object-top"
                  />
                </div>
                <div>
                  <span className="text-xs font-bold text-gold-300">
                    Claim Character: Minister (মন্ত্রী)
                  </span>
                  <p className="text-[11px] text-muted">
                    To collect Tax, you must openly claim the Minister.
                  </p>
                </div>
              </div>

              <div className="rounded-lg border border-white/5 bg-deep-950 p-2.5 text-xs text-parchment-300 leading-relaxed">
                ⚠️ This claim is <strong className="text-gold-300">challengeable</strong>. You can bluff without having the Minister, but if challenged and caught bluffing, you will lose 1 influence.
              </div>
            </div>

            <div className="flex gap-2">
              <Button variant="ghost" fullWidth onClick={onClose}>
                Cancel
              </Button>
              <Button
                variant="premium"
                fullWidth
                onClick={() => {
                  onConfirm("tax");
                  onClose();
                }}
              >
                Claim Minister & Collect Tax (+3)
              </Button>
            </div>
          </div>
        )}

        {/* 4. Steal - Select Target */}
        {modalStep.type === "select_target_steal" && (
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <span className="flex size-12 shrink-0 items-center justify-center rounded-2xl border border-forest-500/40 bg-forest-500/15 text-forest-300">
                <Hand className="size-6" />
              </span>
              <div>
                <h3 className="font-display text-xl font-bold text-ivory">
                  Steal — Select Target
                </h3>
                <p className="font-cinzel text-xs text-muted">
                  SELECT TARGET PLAYER (+2 COINS)
                </p>
              </div>
            </div>

            <p className="text-xs text-parchment-300">
              Choose a player to steal from. The selected player will lose 2 coins.
            </p>

            <div className="space-y-2 max-h-56 overflow-y-auto pr-1">
              {aliveOpponents.map((opp) => (
                <button
                  key={opp.id}
                  type="button"
                  disabled={opp.coins === 0}
                  onClick={() => setModalStep({ type: "claim_dalal", targetPlayer: opp })}
                  className={cn(
                    "flex items-center justify-between w-full p-3 rounded-xl border text-left transition-all cursor-pointer",
                    opp.coins === 0
                      ? "border-white/5 bg-deep-950/40 opacity-50 cursor-not-allowed"
                      : "border-forest-500/25 bg-deep-900 hover:border-gold-400 hover:bg-deep-850",
                  )}
                >
                  <div className="flex items-center gap-2.5">
                    <Users className="size-4 text-gold-400" />
                    <div>
                      <p className="text-sm font-bold text-ivory">
                        {opp.displayName ?? opp.username}
                      </p>
                      <p className="text-[10px] text-muted">
                        INFLUENCE: {opp.influenceCards.filter((c) => !c.revealed).length}
                      </p>
                    </div>
                  </div>
                  <div className="text-right">
                    <span className="font-cinzel text-sm font-bold text-gold-300">
                      {opp.coins} COINS
                    </span>
                    {opp.coins === 0 && (
                      <span className="block text-[10px] text-crimson-400 font-semibold">
                        NO COINS
                      </span>
                    )}
                  </div>
                </button>
              ))}
            </div>

            <Button variant="ghost" fullWidth onClick={onClose}>
              Cancel
            </Button>
          </div>
        )}

        {/* 4b. Steal - Claim Dalal */}
        {modalStep.type === "claim_dalal" && (
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <div className="relative size-12 rounded-lg overflow-hidden border border-forest-400 shrink-0">
                <Image
                  src="/assets/cards/dalal.png"
                  alt="Broker"
                  fill
                  className="object-cover object-top"
                />
              </div>
              <div>
                <h3 className="font-display text-xl font-bold text-ivory">
                  Steal — Claim Broker
                </h3>
                <p className="font-cinzel text-xs text-muted">
                  Target: {modalStep.targetPlayer.displayName ?? modalStep.targetPlayer.username} (+2 COINS)
                </p>
              </div>
            </div>

            <div className="rounded-xl border border-forest-500/30 bg-deep-950/80 p-4 space-y-2 text-xs text-parchment-200">
              <p>
                You are claiming <strong className="text-gold-300">Broker (দালাল)</strong> to steal 2 coins from <strong className="text-ivory">{modalStep.targetPlayer.displayName ?? modalStep.targetPlayer.username}</strong>.
              </p>
              <p className="text-muted text-[11px]">
                🛡️ The target player may claim Bureaucrat or Broker to block this steal.
              </p>
            </div>

            <div className="flex gap-2">
              <Button
                variant="ghost"
                fullWidth
                onClick={() => setModalStep({ type: "select_target_steal" })}
              >
                Change Target
              </Button>
              <Button
                variant="premium"
                fullWidth
                onClick={() => {
                  onConfirm("steal", modalStep.targetPlayer.id);
                  onClose();
                }}
              >
                Confirm Steal (+2 Coins)
              </Button>
            </div>
          </div>
        )}

        {/* 5. Exchange UI - Intent Confirmation */}
        {modalStep.type === "exchange_ui" && (
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <div className="relative size-12 rounded-lg overflow-hidden border border-parchment-400 shrink-0">
                <Image
                  src="/assets/cards/amla.png"
                  alt="Bureaucrat"
                  fill
                  className="object-cover object-top"
                />
              </div>
              <div>
                <h3 className="font-display text-xl font-bold text-ivory">
                  Exchange
                </h3>
                <p className="font-cinzel text-xs text-muted">
                  Claim Bureaucrat · Draw 2 & Keep 2
                </p>
              </div>
            </div>

            <div className="rounded-xl border border-gold-500/40 bg-gradient-to-b from-deep-900 to-deep-950 p-4 space-y-3">
              <div className="flex items-center gap-3">
                <div className="relative size-12 rounded-lg overflow-hidden border border-gold-400 shrink-0">
                  <Image
                    src="/assets/cards/amla.png"
                    alt="Bureaucrat"
                    fill
                    className="object-cover object-top"
                  />
                </div>
                <div>
                  <span className="text-xs font-bold text-gold-300">
                    Claim Character: Bureaucrat (আমলা)
                  </span>
                  <p className="text-[11px] text-muted">
                    To exchange cards, you must openly claim the Bureaucrat.
                  </p>
                </div>
              </div>

              <div className="rounded-lg border border-white/5 bg-deep-950 p-2.5 text-xs text-parchment-300 leading-relaxed">
                ⚠️ This claim is <strong className="text-gold-300">challengeable</strong>. You will draw 2 cards from the deck, keep 2, and return the rest. If challenged and caught bluffing, you will lose 1 influence.
              </div>
            </div>

            <div className="flex gap-2">
              <Button variant="ghost" fullWidth onClick={onClose}>
                Cancel
              </Button>
              <Button
                variant="premium"
                fullWidth
                onClick={() => {
                  onConfirm("exchange");
                  onClose();
                }}
              >
                Claim Bureaucrat & Start Exchange
              </Button>
            </div>
          </div>
        )}

        {/* 6. Assassinate - Select Target */}
        {modalStep.type === "select_target_assassinate" && (
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <span className="flex size-12 shrink-0 items-center justify-center rounded-2xl border border-crimson-500/40 bg-crimson-500/15 text-crimson-300">
                <Skull className="size-6" />
              </span>
              <div>
                <h3 className="font-display text-xl font-bold text-ivory">
                  Assassinate — Select Target
                </h3>
                <p className="font-cinzel text-xs text-muted">
                  Cost: 3 Coins · Target Elimination
                </p>
              </div>
            </div>

            <p className="text-xs text-parchment-300">
              Select a player to eliminate. Cost: 3 COINS.
            </p>

            <div className="space-y-2 max-h-56 overflow-y-auto pr-1">
              {aliveOpponents.map((opp) => (
                <button
                  key={opp.id}
                  type="button"
                  onClick={() =>
                    setModalStep({ type: "confirm_assassinate", targetPlayer: opp })
                  }
                  className="flex items-center justify-between w-full p-3 rounded-xl border border-crimson-500/30 bg-deep-900 hover:border-crimson-400 hover:bg-deep-850 text-left transition-all cursor-pointer"
                >
                  <div className="flex items-center gap-2.5">
                    <Skull className="size-4 text-crimson-400" />
                    <div>
                      <p className="text-sm font-bold text-ivory">
                        {opp.displayName ?? opp.username}
                      </p>
                      <p className="text-[10px] text-muted">
                        INFLUENCE: {opp.influenceCards.filter((c) => !c.revealed).length}
                      </p>
                    </div>
                  </div>
                  <span className="text-xs text-crimson-300 font-bold uppercase tracking-wider font-cinzel">
                    Select Target ➔
                  </span>
                </button>
              ))}
            </div>

            <Button variant="ghost" fullWidth onClick={onClose}>
              Cancel
            </Button>
          </div>
        )}

        {/* 6b. Assassinate - Cost Confirmation */}
        {modalStep.type === "confirm_assassinate" && (
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <div className="relative size-12 rounded-lg overflow-hidden border border-crimson-400 shrink-0">
                <Image
                  src="/assets/cards/ghatok.png"
                  alt="Assassin"
                  fill
                  className="object-cover object-top"
                />
              </div>
              <div>
                <h3 className="font-display text-xl font-bold text-crimson-300">
                  Assassinate — Confirm Target
                </h3>
                <p className="font-cinzel text-xs text-muted">
                  Claim Assassin · Cost: 3 Coins
                </p>
              </div>
            </div>

            <div className="rounded-xl border border-crimson-500/40 bg-crimson-950/40 p-4 space-y-2 text-xs text-parchment-200">
              <div className="flex justify-between items-center text-sm font-bold pb-2 border-b border-crimson-500/20">
                <span>Target: {modalStep.targetPlayer.displayName ?? modalStep.targetPlayer.username}</span>
                <span className="text-crimson-300 font-cinzel">-3 COINS</span>
              </div>
              <p>
                You are claiming <strong className="text-ivory">Assassin (ঘাতক)</strong>. If successful, the target will lose 1 influence card.
              </p>
              <p className="text-muted text-[11px]">
                🛡️ The target may claim Detective (গোয়েন্দা) to block this assassination.
              </p>
            </div>

            <div className="flex gap-2">
              <Button
                variant="ghost"
                fullWidth
                onClick={() => setModalStep({ type: "select_target_assassinate" })}
              >
                Change Target
              </Button>
              <Button
                variant="danger"
                fullWidth
                onClick={() => {
                  onConfirm("assassinate", modalStep.targetPlayer.id);
                  onClose();
                }}
              >
                Confirm Assassination (-3 Coins)
              </Button>
            </div>
          </div>
        )}

        {/* 7. Coup - Select Target */}
        {modalStep.type === "select_target_coup" && (
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <span className="flex size-12 shrink-0 items-center justify-center rounded-2xl border border-gold-500/50 bg-gold-500/15 text-gold-300 shadow-gold">
                <Crown className="size-6" />
              </span>
              <div>
                <h3 className="font-display text-xl font-bold text-gold-gradient">
                  Coup — Select Target
                </h3>
                <p className="font-cinzel text-xs text-muted">
                  Cost: 7 Coins · Unblockable & Unchallengeable
                </p>
              </div>
            </div>

            <p className="text-xs text-parchment-300">
              Select a player to eliminate. Cost: 7 COINS.
            </p>

            <div className="space-y-2 max-h-56 overflow-y-auto pr-1">
              {aliveOpponents.map((opp) => (
                <button
                  key={opp.id}
                  type="button"
                  onClick={() => setModalStep({ type: "confirm_coup", targetPlayer: opp })}
                  className="flex items-center justify-between w-full p-3 rounded-xl border border-gold-500/30 bg-deep-900 hover:border-gold-400 hover:bg-deep-850 text-left transition-all cursor-pointer"
                >
                  <div className="flex items-center gap-2.5">
                    <Crown className="size-4 text-gold-400" />
                    <div>
                      <p className="text-sm font-bold text-ivory">
                        {opp.displayName ?? opp.username}
                      </p>
                      <p className="text-[10px] text-muted">
                        INFLUENCE: {opp.influenceCards.filter((c) => !c.revealed).length}
                      </p>
                    </div>
                  </div>
                  <span className="text-xs text-gold-300 font-bold uppercase tracking-wider font-cinzel">
                    Coup ➔
                  </span>
                </button>
              ))}
            </div>

            <Button variant="ghost" fullWidth onClick={onClose}>
              Cancel
            </Button>
          </div>
        )}

        {/* 7b. Coup - Destructive Confirmation */}
        {modalStep.type === "confirm_coup" && (
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <div className="flex size-12 items-center justify-center rounded-2xl border-2 border-crimson-500/70 bg-crimson-950 text-crimson-400 shrink-0">
                <AlertTriangle className="size-7" />
              </div>
              <div>
                <h3 className="font-display text-xl font-bold text-crimson-300">
                  Coup — Confirm Target
                </h3>
                <p className="font-cinzel text-xs text-muted">
                  Cost: 7 Coins · Unblockable & Unchallengeable
                </p>
              </div>
            </div>

            <div className="rounded-xl border border-crimson-500/50 bg-crimson-950/60 p-4 space-y-2 text-xs text-parchment-200">
              <div className="flex justify-between items-center text-sm font-bold pb-2 border-b border-crimson-500/30">
                <span className="text-ivory">
                  Target: {modalStep.targetPlayer.displayName ?? modalStep.targetPlayer.username}
                </span>
                <span className="text-crimson-400 font-cinzel">-7 COINS</span>
              </div>
              <p className="text-crimson-200">
                ⚠️ This action is unblockable and cannot be challenged!
              </p>
              <p className="text-muted text-[11px] leading-relaxed">
                The selected player must immediately reveal and lose 1 influence card. No player can block or challenge a Coup.
              </p>
            </div>

            <div className="flex gap-2">
              <Button
                variant="ghost"
                fullWidth
                onClick={() => setModalStep({ type: "select_target_coup" })}
              >
                Change Target
              </Button>
              <Button
                variant="danger"
                fullWidth
                onClick={() => {
                  onConfirm("coup", modalStep.targetPlayer.id);
                  onClose();
                }}
              >
                Confirm Coup (-7 Coins)
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
