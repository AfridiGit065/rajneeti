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

/** 2. BlockDialog: Modal shown when a block is active */
export function BlockDialog({
  event,
  currentPlayer,
  open,
  busy = false,
  onClose,
  onAllowBlock,
  onChallenge,
}: {
  event: BlockEvent;
  currentPlayer: GamePlayer;
  open: boolean;
  busy?: boolean;
  onClose: () => void;
  /** Actor accepting the block — resolves the pending action as blocked. */
  onAllowBlock: () => void;
  /** Any alive non-blocker challenging the block claim. */
  onChallenge: () => void;
}) {
  if (!open) return null;

  const character = CHARACTER_MAP[event.claimedCharacter];
  const action = getAction(event.actionId);
  const isSelfBlocker = currentPlayer.id === event.blocker.id;
  const isActor = currentPlayer.id === event.targetOrActor.id;
  const canChallenge = !isSelfBlocker && currentPlayer.isAlive;

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

        {isSelfBlocker ? (
          <p className="text-center text-xs text-muted font-bengali">
            আপনি নিজের ব্লক চ্যালেঞ্জ করতে পারবেন না।
          </p>
        ) : !isActor ? (
          <p className="text-center text-xs text-muted font-bengali">
            অ্যাকশনের কর্তা ছাড়া অন্যরা চ্যালেঞ্জ করতে পারে — ব্লক সত্য হলে চ্যালেঞ্জকারী ১টি ইনফ্লুয়েন্স হারাবে।
          </p>
        ) : null}

        {/* Action Buttons: ALLOW BLOCK (actor) / CHALLENGE BLOCK */}
        <div className="flex flex-col sm:flex-row items-center gap-3 pt-2">
          <Button
            variant="outline"
            fullWidth
            onClick={onClose}
            className="gap-1.5 font-cinzel font-semibold text-xs tracking-wider uppercase"
          >
            <XCircle className="size-4 text-muted" />
            Close
          </Button>

          {isActor ? (
            <Button
              variant="premium"
              fullWidth
              disabled={busy}
              onClick={onAllowBlock}
              className="gap-1.5 shadow-gold font-cinzel font-semibold text-xs tracking-wider uppercase"
            >
              <CheckCircle className="size-4 text-forest-400" />
              Allow Block
            </Button>
          ) : null}

          <Button
            variant="danger"
            fullWidth
            disabled={!canChallenge || busy}
            onClick={onChallenge}
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

/** 2b. BlockOffer: Banner inviting a non-actor to block a blockable action. */
export function BlockOffer({
  action,
  busy = false,
  onBlock,
}: {
  action: GameActionId;
  busy?: boolean;
  onBlock: (claimed: CharacterId) => void;
}) {
  const [choosing, setChoosing] = useState(false);
  const act = getAction(action);
  const claimants = (act.blockableBy ?? []) as readonly CharacterId[];

  if (choosing) {
    return (
      <div
        className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in"
        role="dialog"
        aria-modal="true"
      >
        <div className="relative w-full max-w-md rounded-3xl border-2 border-crimson-500/50 bg-surface panel-emboss panel-texture p-6 sm:p-7 shadow-2xl space-y-5">
          <button
            type="button"
            onClick={() => setChoosing(false)}
            className="absolute right-4 top-4 flex size-8 items-center justify-center rounded-full border border-white/10 bg-deep-950/80 text-muted hover:text-ivory transition-all cursor-pointer"
          >
            <X className="size-4" />
          </button>

          <div className="text-center space-y-2">
            <div className="inline-flex size-14 items-center justify-center rounded-2xl border-2 border-crimson-500/50 bg-crimson-950/60 text-crimson-400 shadow-crimson">
              <ShieldAlert className="size-8" />
            </div>
            <div>
              <span className="font-cinzel text-xs font-bold uppercase tracking-[0.25em] text-crimson-400">
                Block Opportunity
              </span>
              <h2 className="font-display text-2xl font-bold text-ivory mt-0.5">
                React to {act.nameEn} ({act.nameBn})
              </h2>
            </div>
          </div>

          <p className="text-xs text-parchment-300 font-bengali leading-relaxed">
            কোন চরিত্রের মালিকানা দাবি করে {act.nameBn} অ্যাকশনটি ব্লক করবেন? ব্লাফ করাও
            বৈধ — হাতে কার্ড না থাকলেও দাবি করতে পারেন, তবে চ্যালেঞ্জ করে ফাঁস করলে ১টি
            ইনফ্লুয়েন্স হারাবেন।
          </p>

          <div className="grid gap-2 sm:grid-cols-2">
            {claimants.map((id) => {
              const ch = CHARACTER_MAP[id]!;
              return (
                <button
                  key={id}
                  type="button"
                  disabled={busy}
                  onClick={() => onBlock(id)}
                  className="flex items-center gap-3 rounded-xl border border-crimson-500/30 bg-deep-950 p-3 text-left transition-all cursor-pointer hover:border-gold-400 hover:bg-deep-900 disabled:opacity-50"
                >
                  <div className="relative size-10 shrink-0 overflow-hidden rounded-lg border border-gold-500/40 bg-deep-900">
                    <Image
                      src={ch.imagePath}
                      alt={ch.nameBn}
                      fill
                      className="object-cover object-top"
                    />
                  </div>
                  <div className="min-w-0">
                    <p className="font-bengali text-sm font-bold text-gold-300">
                      {ch.nameBn}
                    </p>
                    <p className="font-cinzel text-[10px] text-muted uppercase tracking-wider">
                      {ch.nameEn}
                    </p>
                  </div>
                </button>
              );
            })}
          </div>

          <Button
            variant="ghost"
            fullWidth
            onClick={() => setChoosing(false)}
          >
            Cancel
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border-2 border-crimson-500/50 bg-gradient-to-r from-crimson-950/70 via-deep-900 to-deep-950 p-4 shadow-crimson animate-fade-in">
      <div className="flex items-center gap-3">
        <span className="flex size-10 items-center justify-center rounded-xl border border-crimson-500/50 bg-crimson-600/20 text-crimson-300">
          <ShieldAlert className="size-5" />
        </span>
        <div>
          <div className="flex items-center gap-2">
            <span className="text-sm font-bold text-ivory">Block Opportunity</span>
            <Badge tone="crimson">Can React</Badge>
          </div>
          <p className="text-xs text-muted mt-0.5">
            {act.nameEn} ({act.nameBn}) — claim{" "}
            <span className="text-gold-300 font-semibold">
              {claimants.map((id) => CHARACTER_MAP[id]?.nameBn ?? id).join(" / ")}
            </span>{" "}
            to block.
          </p>
        </div>
      </div>

      <Button
        variant="premium"
        size="sm"
        disabled={busy}
        onClick={() => setChoosing(true)}
        className="gap-2 font-cinzel font-semibold text-xs tracking-wider uppercase"
      >
        <ShieldAlert className="size-3.5" />
        Block
      </Button>
    </div>
  );
}

/** 2c. BlockWindowPanel: Actor resolve control while their blockable action is pending. */
export function BlockWindowPanel({
  action,
  busy = false,
  onResolve,
}: {
  action: GameActionId;
  busy?: boolean;
  onResolve: () => void;
}) {
  const act = getAction(action);
  const claimants = (act.blockableBy ?? []) as readonly CharacterId[];

  return (
    <div className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border-2 border-gold-500/40 bg-gradient-to-r from-deep-950 via-deep-900 to-deep-950 p-4 shadow-gold animate-fade-in">
      <div className="flex items-center gap-3">
        <span className="flex size-10 items-center justify-center rounded-xl border border-gold-500/50 bg-gold-500/10 text-gold-300">
          <ShieldAlert className="size-5" />
        </span>
        <div>
          <div className="flex items-center gap-2">
            <span className="text-sm font-bold text-ivory">Block Window Open</span>
            <Badge tone="gold">Waiting</Badge>
          </div>
          <p className="text-xs text-muted mt-0.5">
            {act.nameEn} ({act.nameBn}) pending — blockable by{" "}
            <span className="text-gold-300 font-semibold">
              {claimants.map((id) => CHARACTER_MAP[id]?.nameBn ?? id).join(" / ")}
            </span>
            . Resolve now (no block submitted yet)?
          </p>
        </div>
      </div>

      <Button
        variant="premium"
        size="sm"
        disabled={busy}
        onClick={onResolve}
        className="gap-2 font-cinzel font-semibold text-xs tracking-wider uppercase"
      >
        <CheckCircle className="size-3.5 text-forest-400" />
        Resolve Action
      </Button>
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
