"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import { cn } from "@/lib/cn";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { CHARACTER_MAP } from "@/lib/game/characters";
import {
  Skull,
  AlertTriangle,
  Eye,
} from "@/components/ui/icons";
import type { GamePlayer, InfluenceCard } from "@/types/game";

/** 1. CardReveal: Interactive single card reveal with flip animation */
export function CardReveal({
  card,
  isSelected,
  disabled = false,
  onClick,
}: {
  card: InfluenceCard;
  isSelected: boolean;
  disabled?: boolean;
  onClick: () => void;
}) {
  const character = CHARACTER_MAP[card.characterId];

  return (
    <button
      type="button"
      disabled={disabled}
      onClick={onClick}
      className={cn(
        "group relative flex flex-col items-center rounded-2xl border-2 p-3 text-center transition-all duration-300 cursor-pointer select-none",
        "focus-visible:outline-2 focus-visible:outline-gold-400",
        isSelected
          ? "border-crimson-400 bg-crimson-950/60 shadow-crimson -translate-y-2 scale-105 ring-2 ring-crimson-400/50"
          : "border-forest-500/30 bg-deep-900/80 hover:border-gold-400/70 hover:bg-deep-850 hover:-translate-y-1",
        disabled && "opacity-40 cursor-not-allowed",
      )}
    >
      <div className="relative w-28 sm:w-32 aspect-[3/4] overflow-hidden rounded-xl border border-white/10 bg-deep-950">
        {character ? (
          <Image
            src={character.imagePath}
            alt={character.nameBn}
            fill
            className={cn(
              "object-cover object-top transition-transform duration-500",
              isSelected && "scale-105 brightness-90",
            )}
          />
        ) : null}

        {/* Selection overlay badge */}
        {isSelected && (
          <div className="absolute inset-0 bg-crimson-950/50 backdrop-blur-[1px] flex flex-col items-center justify-center p-2 text-center animate-fade-in">
            <Skull className="size-8 text-crimson-300 animate-bounce" />
            <span className="mt-1 font-bengali text-xs font-bold text-crimson-200">
              উন্মোচন নির্বাচিত
            </span>
          </div>
        )}
      </div>

      <div className="mt-2 text-center w-full">
        <p className="font-bengali text-sm font-bold text-ivory">
          {character?.nameBn}
        </p>
        <p className="font-cinzel text-[10px] uppercase tracking-wider text-muted">
          {character?.nameEn}
        </p>
      </div>

      <div className="mt-1">
        <Badge tone={isSelected ? "crimson" : "neutral"} className="text-[10px]">
          {isSelected ? "উন্মোচিত হবে" : "রক্ষা করা"}
        </Badge>
      </div>
    </button>
  );
}

/** 2. InfluenceLostModal: Modal prompt when player must choose which card to reveal */
export function InfluenceLostModal({
  open,
  player,
  reason = "আক্রমণ বা ভুল চ্যালেঞ্জের কারণে ১টি ইনফ্লুয়েন্স হারাচ্ছেন",
  onConfirmReveal,
}: {
  open: boolean;
  player: GamePlayer;
  reason?: string;
  onConfirmReveal: (cardId: string) => void;
}) {
  const hiddenCards = player.influenceCards.filter((c) => !c.revealed);
  const [selectedCardId, setSelectedCardId] = useState<string | null>(null);

  const activeSelectedId =
    selectedCardId && hiddenCards.some((c) => c.id === selectedCardId)
      ? selectedCardId
      : hiddenCards[0]?.id ?? null;

  if (!open || hiddenCards.length === 0) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md animate-fade-in"
      role="dialog"
      aria-modal="true"
    >
      <div className="relative w-full max-w-lg rounded-3xl border-2 border-crimson-500/70 bg-surface panel-emboss panel-texture p-6 sm:p-7 shadow-2xl space-y-5 text-center">
        {/* Banner */}
        <div className="space-y-2">
          <div className="mx-auto flex size-14 items-center justify-center rounded-2xl border-2 border-crimson-500/60 bg-crimson-950/70 text-crimson-300 shadow-crimson">
            <AlertTriangle className="size-8 animate-pulse" />
          </div>
          <div>
            <span className="font-cinzel text-xs font-bold uppercase tracking-[0.25em] text-crimson-400">
              Influence Lost · প্রভাব হারানো
            </span>
            <h2 className="font-bengali text-2xl font-bold text-ivory mt-0.5">
              একটি কার্ড উন্মোচন করুন
            </h2>
          </div>
          <p className="font-bengali text-xs text-muted max-w-sm mx-auto">
            {reason}। নিচের যেকোনো ১টি কার্ড বেছে নিয়ে প্রকাশ্যে আনুন।
          </p>
        </div>

        {/* Card choices */}
        <div className="flex flex-wrap items-center justify-center gap-4 py-2">
          {hiddenCards.map((card) => (
            <CardReveal
              key={card.id}
              card={card}
              isSelected={activeSelectedId === card.id}
              onClick={() => setSelectedCardId(card.id)}
            />
          ))}
        </div>

        <div className="rounded-xl border border-crimson-500/30 bg-deep-950/80 p-3 text-xs text-muted font-bengali">
          ⚠️ উন্মোচিত কার্ডের ক্ষমতা আর ব্যবহার করা যাবে না এবং এটি টেবিলে সবার সামনে থাকবে।
        </div>

        <Button
          variant="danger"
          fullWidth
          disabled={!activeSelectedId}
          onClick={() => {
            if (activeSelectedId) {
              onConfirmReveal(activeSelectedId);
            }
          }}
          className="font-bengali gap-2 shadow-crimson text-sm font-bold"
        >
          <Eye className="size-4" />
          নির্বাচিত কার্ডটি উন্মোচন নিশ্চিত করুন
        </Button>
      </div>
    </div>
  );
}

/** 3. EliminationOverlay: Dramatic non-violent elimination screen */
export function EliminationOverlay({
  open,
  eliminatedPlayer,
  onFinish,
}: {
  open: boolean;
  eliminatedPlayer: GamePlayer;
  onFinish: () => void;
}) {
  const [countdown, setCountdown] = useState(4);

  useEffect(() => {
    if (!open) return;
    const interval = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          clearInterval(interval);
          onFinish();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [open, onFinish]);

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/90 backdrop-blur-lg animate-fade-in"
      role="dialog"
      aria-modal="true"
    >
      <div className="relative w-full max-w-md rounded-3xl border-2 border-crimson-600 bg-gradient-to-b from-deep-950 via-deep-900 to-black p-8 text-center shadow-[0_0_80px_rgba(220,38,38,0.4)] space-y-6">
        {/* Animated skull emblem */}
        <div className="mx-auto flex size-20 items-center justify-center rounded-3xl border-2 border-crimson-500 bg-crimson-950/80 text-crimson-400 shadow-crimson animate-pulse">
          <Skull className="size-12" />
        </div>

        {/* Text */}
        <div className="space-y-1.5">
          <span className="font-cinzel text-xs font-bold uppercase tracking-[0.35em] text-crimson-400">
            Player Eliminated
          </span>
          <h2 className="font-cinzel text-3xl sm:text-4xl font-black text-ivory tracking-widest">
            ELIMINATED
          </h2>
          <p className="font-bengali text-xl font-bold text-crimson-200 mt-1">
            {eliminatedPlayer.displayName ?? eliminatedPlayer.username} ছিটকে পড়েছেন!
          </p>
          <p className="font-bengali text-xs text-muted max-w-xs mx-auto leading-relaxed pt-1">
            উভয় রাজনৈতিক প্রভাব কার্ড উন্মোচিত হওয়ায় খেলোয়াড় এই ম্যাচ থেকে অপসারিত হলেন।
          </p>
        </div>

        {/* Auto return countdown pill */}
        <div className="flex items-center justify-center gap-2 rounded-xl border border-white/10 bg-deep-950 px-4 py-2.5 text-xs text-parchment-300 font-bengali">
          <span>খেলা বোর্ডে ফিরে যাওয়া হচ্ছে… ({countdown}s)</span>
        </div>

        <Button
          variant="outline"
          fullWidth
          onClick={onFinish}
          className="font-bengali text-xs"
        >
          বোর্ডে ফিরে যান (Return to Board)
        </Button>
      </div>
    </div>
  );
}
