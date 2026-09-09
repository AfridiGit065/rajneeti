"use client";

import { useState } from "react";
import Image from "next/image";
import { cn } from "@/lib/cn";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { CHARACTER_MAP } from "@/lib/game/characters";
import { getAction } from "@/lib/game/actions";
import {
  ShieldAlert,
  Gavel,
  CheckCircle,
  XCircle,
  X,
} from "@/components/ui/icons";
import type { GameActionId, GamePlayer } from "@/types/game";
import type { CharacterId } from "@/types/character";

export type BlockOutcome =
  | "block_succeeds"
  | "block_claim_is_bluff"
  | "challenger_loses_influence"
  | "blocker_loses_influence";

export interface BlockEvent {
  blocker: GamePlayer;
  targetOrActor: GamePlayer;
  actionId: GameActionId;
  claimedCharacter: CharacterId;
}

export interface BlockResultData {
  outcome: BlockOutcome;
  blocker: GamePlayer;
  challenger: GamePlayer;
  claimedCharacter: CharacterId;
  actionId: GameActionId;
}

/** 1. BlockResult: Dramatic visual outcome card for block resolutions */
export function BlockResult({
  result,
  onDismiss,
}: {
  result: BlockResultData;
  onDismiss: () => void;
}) {
  const character = CHARACTER_MAP[result.claimedCharacter];
  const action = getAction(result.actionId);

  const isSuccess =
    result.outcome === "block_succeeds" ||
    result.outcome === "challenger_loses_influence";

  return (
    <div className="animate-fade-in rounded-2xl border-2 border-gold-500/40 bg-surface panel-emboss panel-texture p-6 shadow-2xl space-y-4">
      <div className="flex items-center justify-between pb-3 border-b border-forest-500/20">
        <div className="flex items-center gap-2">
          {isSuccess ? (
            <CheckCircle className="size-6 text-forest-400" />
          ) : (
            <XCircle className="size-6 text-crimson-400" />
          )}
          <span className="font-cinzel text-xs font-bold uppercase tracking-widest text-muted">
            Block Resolution
          </span>
        </div>
        <Badge tone={isSuccess ? "emerald" : "crimson"}>
          {isSuccess ? "Block Succeeded" : "Block Failed"}
        </Badge>
      </div>

      <div className="flex items-center gap-4">
        {character && (
          <div className="relative size-16 shrink-0 overflow-hidden rounded-xl border border-gold-500/40 bg-deep-950 shadow-gold">
            <Image
              src={character.imagePath}
              alt={character.nameBn}
              fill
              className="object-cover object-top"
            />
          </div>
        )}
        <div>
          <h3 className="text-lg font-bold text-ivory">
            {result.blocker.displayName ?? result.blocker.username}&apos;s Block:{" "}
            <span className="text-gold-300 font-bengali">{character?.nameBn} Claim</span>
          </h3>
          <p className="text-xs text-muted mt-0.5">
            Action: {action.nameEn} ({action.nameBn})
          </p>
        </div>
      </div>

      {/* Outcome text */}
      <div
        className={cn(
          "rounded-xl border p-4 text-xs leading-relaxed",
          isSuccess
            ? "border-forest-500/30 bg-forest-950/40 text-forest-200"
            : "border-crimson-500/30 bg-crimson-950/40 text-crimson-200",
        )}
      >
        {result.outcome === "block_succeeds" && (
          <p>
            🛡️ <strong className="text-ivory">{result.blocker.displayName ?? result.blocker.username}</strong>&apos;s block was accepted! The {action.nameEn} action has been cancelled.
          </p>
        )}
        {result.outcome === "block_claim_is_bluff" && (
          <p>
            🎭 <strong>Bluff Caught!</strong> {result.blocker.displayName ?? result.blocker.username} does not hold {character?.nameBn}! Blocker loses 1 influence card for false block claim, and action resolves.
          </p>
        )}
        {result.outcome === "challenger_loses_influence" && (
          <p>
            ⚠️ <strong>Wrong Challenge!</strong> {result.blocker.displayName ?? result.blocker.username} proved their claim was genuine! Challenger <strong className="text-ivory">{result.challenger.displayName ?? result.challenger.username}</strong> loses 1 influence card and the block holds.
          </p>
        )}
        {result.outcome === "blocker_loses_influence" && (
          <p>
            ☠️ <strong>Blocker Influence Lost!</strong> Caught bluffing on block claim, {result.blocker.displayName ?? result.blocker.username} must lose an influence card.
          </p>
        )}
      </div>

      <Button variant="premium" fullWidth onClick={onDismiss}>
        Continue ➔
      </Button>
    </div>
  );
}

/** 2. BlockDialog: Interactive modal when a block occurs */
export function BlockDialog({
  event,
  currentPlayer,
  open,
  onClose,
  onResolve,
}: {
  event: BlockEvent;
  currentPlayer: GamePlayer;
  open: boolean;
  onClose: () => void;
  onResolve: (outcome: BlockOutcome) => void;
}) {
  const [mockOutcomeSelect, setMockOutcomeSelect] = useState<BlockOutcome | null>(null);

  if (!open) return null;

  const character = CHARACTER_MAP[event.claimedCharacter];
  const action = getAction(event.actionId);
  const isSelfBlocker = currentPlayer.id === event.blocker.id;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in"
      role="dialog"
      aria-modal="true"
    >
      <div className="relative w-full max-w-lg rounded-3xl border-2 border-crimson-500/50 bg-surface panel-emboss panel-texture p-6 sm:p-7 shadow-2xl space-y-5">
        <button
          type="button"
          onClick={onClose}
          className="absolute right-4 top-4 flex size-8 items-center justify-center rounded-full border border-white/10 bg-deep-950/80 text-muted hover:text-ivory transition-all cursor-pointer"
        >
          <X className="size-4" />
        </button>

        {/* Top banner: PLAYER BLOCKED ACTION */}
        <div className="text-center space-y-2">
          <div className="inline-flex size-14 items-center justify-center rounded-2xl border-2 border-crimson-500/50 bg-crimson-950/60 text-crimson-400 shadow-crimson">
            <ShieldAlert className="size-8" />
          </div>
          <div>
            <span className="font-cinzel text-xs font-bold uppercase tracking-[0.25em] text-crimson-400">
              Player Blocked Action
            </span>
            <h2 className="font-display text-2xl font-bold text-ivory mt-0.5">
              {event.blocker.displayName ?? event.blocker.username} blocked the action!
            </h2>
          </div>
          <p className="text-xs text-muted">
            Action: <strong className="text-parchment-200">{action.nameEn}</strong> ({action.nameBn})
          </p>
        </div>

        {/* Block Claim Details */}
        <div className="flex items-center gap-4 rounded-2xl border border-gold-500/30 bg-deep-950/80 p-4">
          {character && (
            <div className="relative size-16 shrink-0 overflow-hidden rounded-xl border-2 border-gold-500/50 bg-deep-900 shadow-gold">
              <Image
                src={character.imagePath}
                alt={character.nameBn}
                fill
                className="object-cover object-top"
              />
            </div>
          )}
          <div className="min-w-0 flex-1">
            <span className="text-[10px] font-cinzel font-bold uppercase tracking-wider text-muted">
              Claimed Character Role
            </span>
            <p className="font-bengali text-xl font-bold text-gold-gradient">
              {character?.nameBn}
            </p>
            <p className="font-cinzel text-xs text-muted uppercase tracking-widest">
              {character?.nameEn}
            </p>
            <p className="text-[11px] text-parchment-300 mt-1">
              {event.claimedCharacter === "minister" && "Claimed Minister (মন্ত্রী) to block Foreign Aid."}
              {(event.claimedCharacter === "amla" || event.claimedCharacter === "dalal") &&
                "Claimed Amla / Dalal (আমলা/দালাল) to block Extortion."}
              {event.claimedCharacter === "goyenda" && "Claimed Goyenda (গোয়েন্দা) to block Assassination."}
            </p>
          </div>
        </div>

        {/* Mock outcome simulation helper for testing */}
        <div className="rounded-xl border border-white/10 bg-deep-900/60 p-3 space-y-2 text-xs">
          <p className="font-cinzel text-[10px] uppercase tracking-wider text-muted font-bold">
            Simulate Mock Outcome:
          </p>
          <div className="grid grid-cols-2 gap-1.5 text-[11px]">
            <button
              type="button"
              onClick={() => setMockOutcomeSelect("block_succeeds")}
              className={cn(
                "p-2 rounded-lg border text-left transition-colors cursor-pointer",
                mockOutcomeSelect === "block_succeeds"
                  ? "border-forest-400 bg-forest-900/30 text-forest-200"
                  : "border-white/5 bg-deep-950 text-muted hover:border-white/20",
              )}
            >
              ✓ Block Succeeds
            </button>
            <button
              type="button"
              onClick={() => setMockOutcomeSelect("block_claim_is_bluff")}
              className={cn(
                "p-2 rounded-lg border text-left transition-colors cursor-pointer",
                mockOutcomeSelect === "block_claim_is_bluff"
                  ? "border-crimson-400 bg-crimson-900/30 text-crimson-200"
                  : "border-white/5 bg-deep-950 text-muted hover:border-white/20",
              )}
            >
              ✕ Caught Bluffing
            </button>
            <button
              type="button"
              onClick={() => setMockOutcomeSelect("challenger_loses_influence")}
              className={cn(
                "p-2 rounded-lg border text-left transition-colors cursor-pointer",
                mockOutcomeSelect === "challenger_loses_influence"
                  ? "border-crimson-400 bg-crimson-900/30 text-crimson-200"
                  : "border-white/5 bg-deep-950 text-muted hover:border-white/20",
              )}
            >
              ☠️ Challenger Loses Influence
            </button>
            <button
              type="button"
              onClick={() => setMockOutcomeSelect("blocker_loses_influence")}
              className={cn(
                "p-2 rounded-lg border text-left transition-colors cursor-pointer",
                mockOutcomeSelect === "blocker_loses_influence"
                  ? "border-crimson-400 bg-crimson-900/30 text-crimson-200"
                  : "border-white/5 bg-deep-950 text-muted hover:border-white/20",
              )}
            >
              ☠️ Blocker Loses Influence
            </button>
          </div>
        </div>

        {/* Action Buttons: CHALLENGE BLOCK / ALLOW */}
        <div className="flex flex-col sm:flex-row items-center gap-3 pt-2">
          <Button
            variant="outline"
            fullWidth
            onClick={() => {
              onResolve(mockOutcomeSelect ?? "block_succeeds");
              onClose();
            }}
            className="gap-1.5 font-cinzel font-semibold text-xs tracking-wider uppercase"
          >
            <CheckCircle className="size-4 text-forest-400" />
            Allow Block
          </Button>

          <Button
            variant="danger"
            fullWidth
            disabled={isSelfBlocker}
            onClick={() => {
              onResolve(mockOutcomeSelect ?? "block_claim_is_bluff");
              onClose();
            }}
            className="gap-1.5 shadow-crimson font-cinzel font-semibold text-xs tracking-wider uppercase"
          >
            <Gavel className="size-4" />
            Challenge Block
          </Button>
        </div>
      </div>
    </div>
  );
}

/** 3. BlockPanel: In-game active block indicator / controller */
export function BlockPanel({
  activeBlock,
  onOpenDialog,
}: {
  activeBlock: BlockEvent | null;
  currentPlayer?: GamePlayer;
  onOpenDialog: () => void;
}) {
  if (!activeBlock) return null;

  const character = CHARACTER_MAP[activeBlock.claimedCharacter];
  const action = getAction(activeBlock.actionId);

  return (
    <div className="rounded-2xl border-2 border-crimson-500/60 bg-gradient-to-r from-crimson-950/70 via-deep-900 to-deep-950 p-4 shadow-crimson flex flex-wrap items-center justify-between gap-4 animate-fade-in">
      <div className="flex items-center gap-3">
        <span className="flex size-10 items-center justify-center rounded-xl border border-crimson-500/50 bg-crimson-600/20 text-crimson-300">
          <ShieldAlert className="size-5" />
        </span>
        <div>
          <div className="flex items-center gap-2">
            <span className="text-sm font-bold text-ivory">
              {activeBlock.blocker.displayName ?? activeBlock.blocker.username}
            </span>
            <Badge tone="crimson">Blocked Action</Badge>
          </div>
          <p className="text-xs text-muted mt-0.5">
            Action: {action.nameEn} ({action.nameBn}) · Claimed: <span className="text-gold-300 font-semibold">{character?.nameBn}</span> ({character?.nameEn})
          </p>
        </div>
      </div>

      <Button variant="premium" size="sm" onClick={onOpenDialog} className="gap-2 font-cinzel font-semibold text-xs tracking-wider uppercase">
        <Gavel className="size-3.5" />
        View Block
      </Button>
    </div>
  );
}
