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
            Block Resolution · ব্লক সমাধান
          </span>
        </div>
        <Badge tone={isSuccess ? "emerald" : "crimson"}>
          {isSuccess ? "সফল ব্লক" : "ব্যর্থ ব্লক"}
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
          <h3 className="font-bengali text-lg font-bold text-ivory">
            {result.blocker.displayName ?? result.blocker.username}-এর ব্লক:{" "}
            <span className="text-gold-300">{character?.nameBn} দাবি</span>
          </h3>
          <p className="text-xs text-muted font-bengali mt-0.5">
            অ্যাকশন: {action.nameBn} ({action.nameEn})
          </p>
        </div>
      </div>

      {/* Outcome text */}
      <div
        className={cn(
          "rounded-xl border p-4 text-xs font-bengali leading-relaxed",
          isSuccess
            ? "border-forest-500/30 bg-forest-950/40 text-forest-200"
            : "border-crimson-500/30 bg-crimson-950/40 text-crimson-200",
        )}
      >
        {result.outcome === "block_succeeds" && (
          <p>
            🛡️ <strong className="text-ivory">{result.blocker.displayName ?? result.blocker.username}</strong>-এর ব্লক সফলভাবে গৃহীত হয়েছে! {action.nameBn} অ্যাকশনটি বাতিল হয়েছে।
          </p>
        )}
        {result.outcome === "block_claim_is_bluff" && (
          <p>
            🎭 <strong>মিথ্যা ব্লক ফাঁস!</strong> {result.blocker.displayName ?? result.blocker.username} এর হাতে {character?.nameBn} ছিল না! ব্লকার মিথ্যা বলার অপরাধে ১টি ইনফ্লুয়েন্স কার্ড হারাল এবং মূল অ্যাকশন কার্যকর হচ্ছে।
          </p>
        )}
        {result.outcome === "challenger_loses_influence" && (
          <p>
            ⚠️ <strong>ভুল চ্যালেঞ্জ!</strong> {result.blocker.displayName ?? result.blocker.username} প্রমাণ করেছে তার দাবি সত্য ছিল! চ্যালেঞ্জকারী <strong className="text-ivory">{result.challenger.displayName ?? result.challenger.username}</strong> ১টি ইনফ্লুয়েন্স হারাল এবং ব্লকটি বহাল রইল।
          </p>
        )}
        {result.outcome === "blocker_loses_influence" && (
          <p>
            ☠️ <strong>ব্লকারের ইনফ্লুয়েন্স ধ্বংস!</strong> চ্যালেঞ্জের মুখে ব্লাফ প্রমাণিত হওয়ায় {result.blocker.displayName ?? result.blocker.username} একটি প্রভাব কার্ড হারাতে বাধ্য হচ্ছে।
          </p>
        )}
      </div>

      <Button variant="premium" fullWidth onClick={onDismiss} className="font-bengali">
        চালিয়ে যান ➔
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
            <h2 className="font-bengali text-2xl font-bold text-ivory mt-0.5">
              {event.blocker.displayName ?? event.blocker.username} অ্যাকশন ব্লক করেছেন!
            </h2>
          </div>
          <p className="font-bengali text-xs text-muted">
            অ্যাকশন: <strong className="text-parchment-200">{action.nameBn}</strong> ({action.nameEn})
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
              Block Claim · দাবি করা চরিত্র
            </span>
            <p className="font-bengali text-xl font-bold text-gold-gradient">
              {character?.nameBn}
            </p>
            <p className="font-cinzel text-xs text-muted uppercase tracking-widest">
              {character?.nameEn}
            </p>
            <p className="font-bengali text-[11px] text-parchment-300 mt-1">
              {event.claimedCharacter === "minister" && "বিদেশি অনুদান ব্লক করতে মন্ত্রী দাবি করেছেন।"}
              {(event.claimedCharacter === "amla" || event.claimedCharacter === "dalal") &&
                "চুরি ঠেকাতে আমলা/দালাল দাবি করেছেন।"}
              {event.claimedCharacter === "goyenda" && "গুপ্তহত্যা ঠেকাতে গোয়েন্দা দাবি করেছেন।"}
            </p>
          </div>
        </div>

        {/* Mock outcome simulation helper for testing */}
        <div className="rounded-xl border border-white/10 bg-deep-900/60 p-3 space-y-2 text-xs">
          <p className="font-cinzel text-[10px] uppercase tracking-wider text-muted font-bold">
            Simulate Mock Outcome (মক ফলাফল বাছাই করুন):
          </p>
          <div className="grid grid-cols-2 gap-1.5 font-bengali text-[11px]">
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
              ✓ ব্লক সফল (Block Succeeds)
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
              ✕ ব্লাফ ধরা পড়ল (Is Bluff)
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
              ☠️ চ্যালেঞ্জার ইনফ্লুয়েন্স হারাল
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
              ☠️ ব্লকার ইনফ্লুয়েন্স হারাল
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
            className="font-bengali gap-1.5"
          >
            <CheckCircle className="size-4 text-forest-400" />
            অনুমোদন দিন · ALLOW
          </Button>

          <Button
            variant="danger"
            fullWidth
            disabled={isSelfBlocker}
            onClick={() => {
              onResolve(mockOutcomeSelect ?? "block_claim_is_bluff");
              onClose();
            }}
            className="font-bengali gap-1.5 shadow-crimson"
          >
            <Gavel className="size-4" />
            চ্যালেঞ্জ ব্লক · CHALLENGE BLOCK
          </Button>
        </div>
      </div>
    </div>
  );
}

/** 3. BlockPanel: In-game active block indicator / controller */
export function BlockPanel({
  activeBlock,
  currentPlayer: _currentPlayer,
  onOpenDialog,
}: {
  activeBlock: BlockEvent | null;
  currentPlayer: GamePlayer;
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
            <span className="font-bengali text-sm font-bold text-ivory">
              {activeBlock.blocker.displayName ?? activeBlock.blocker.username}
            </span>
            <Badge tone="crimson">অ্যাকশন ব্লক করেছে</Badge>
          </div>
          <p className="text-xs text-muted font-bengali mt-0.5">
            অ্যাকশন: {action.nameBn} · দাবি: <span className="text-gold-300 font-semibold">{character?.nameBn}</span>
          </p>
        </div>
      </div>

      <Button variant="premium" size="sm" onClick={onOpenDialog} className="font-bengali gap-2">
        <Gavel className="size-3.5" />
        ব্লক প্রতিক্রিয়া দেখুন · View Block
      </Button>
    </div>
  );
}
