"use client";

import { useState } from "react";
import { cn } from "@/lib/cn";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { InfluenceCard } from "./influence-card";
import { GameService } from "@/services/game-service";
import { useToast } from "@/hooks/use-toast";
import type { GameState, InfluenceCard as InfluenceCardType } from "@/types/game";

interface ExchangeSelectionProps {
  matchId: string;
  cards: InfluenceCardType[];
  onResolved?: (next: GameState) => void;
  onClose?: () => void;
}

/**
 * Overlay for selecting exactly 2 cards from the 4-card exchange pool.
 * Cards are the real cards from the actor's hand (server puts drawn cards
 * into the hand so the pool is just currentPlayer.influenceCards during
 * a pending Exchange).
 */
export function ExchangeSelection({
  matchId,
  cards,
  onResolved,
  onClose,
}: ExchangeSelectionProps) {
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [busy, setBusy] = useState(false);
  const { success, error: notifyError } = useToast();

  function toggle(cardId: string) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(cardId)) {
        next.delete(cardId);
      } else if (next.size < 2) {
        next.add(cardId);
      } else {
        // Replace the third selection with the new one (keep exactly 2)
        const first = Array.from(next)[0];
        if (first) next.delete(first);
        next.add(cardId);
      }
      return next;
    });
  }

  async function handleConfirm() {
    if (selected.size !== 2) return;
    setBusy(true);
    const result = await GameService.confirmExchange(matchId, Array.from(selected));
    setBusy(false);

    if (result.ok) {
      success("কার্ড বদল সম্পন্ন হয়েছে!");
      onResolved?.(result.data);
      onClose?.();
    } else {
      notifyError(result.error.message);
    }
  }

  return (
    <div
      className="fixed inset-0 z-[55] flex items-center justify-center bg-black/85 backdrop-blur-md animate-fade-in"
      role="dialog"
      aria-modal="true"
      aria-label="কার্ড বদল নির্বাচন"
    >
      <div className="relative w-full max-w-lg rounded-3xl border-2 border-gold-500/40 bg-surface panel-emboss p-6 space-y-5">
        <div>
          <h3 className="font-bengali text-xl font-bold text-ivory">
            কার্ড বদল (Exchange)
          </h3>
          <p className="font-cinzel text-xs text-muted mt-1">
            Select exactly 2 cards to keep
          </p>
        </div>

        <p className="text-xs text-parchment-300 font-bengali leading-relaxed">
          ডেক থেকে ২টি নতুন কার্ড তোলা হয়েছে। আপনার হাতে এখন ৪টি কার্ড।
          নিচের কার্ডগুলোর মধ্য থেকে <strong className="text-gold-300">ঠিক ২টি রাখুন</strong> —
          বাকিগুলো ডেকে ফেরত যাবে।
        </p>

        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 justify-items-center">
          {cards.map((card) => {
            const isSelected = selected.has(card.id);
            return (
              <button
                key={card.id}
                type="button"
                onClick={() => toggle(card.id)}
                className={cn(
                  "relative flex flex-col items-center rounded-xl p-1.5 transition-all cursor-pointer select-none",
                  isSelected
                    ? "ring-2 ring-gold-400 ring-offset-2 ring-offset-surface"
                    : "ring-1 ring-white/10 hover:ring-white/25",
                )}
              >
                <InfluenceCard
                  characterId={card.characterId}
                  state="revealed"
                  size="sm"
                  label={`${card.characterId} — ${isSelected ? "রাখবেন" : "ফেরত"}`}
                />
                <Badge
                  tone={isSelected ? "gold" : "neutral"}
                  className="mt-1.5"
                >
                  {isSelected ? "✓ রাখবেন" : "ফেরত"}
                </Badge>
              </button>
            );
          })}
        </div>

        <p className="text-[11px] text-center text-muted font-bengali">
          নির্বাচিত: {selected.size}/২
        </p>

        <div className="flex gap-2">
          <Button variant="ghost" fullWidth onClick={onClose} disabled={busy}>
            বাতিল
          </Button>
          <Button
            variant="premium"
            fullWidth
            disabled={selected.size !== 2 || busy}
            onClick={() => void handleConfirm()}
          >
            বদল সম্পন্ন করুন
          </Button>
        </div>
      </div>
    </div>
  );
}