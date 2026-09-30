"use client";

import { useCallback, useMemo, useState } from "react";
import { cn } from "@/lib/cn";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Check } from "@/components/ui/icons";
import { CHARACTER_MAP } from "@/lib/game/characters";
import {
  EXCHANGE_KEEP_COUNT,
  createExchangeSelection,
  exchangeKeepCardIds,
  exchangeSelectionCount,
  isExchangeSelectionComplete,
  pruneExchangeSelection,
  toggleExchangeCard,
  type ExchangeSelectionState,
} from "@/lib/game/exchange-selection";
import { InfluenceCard } from "./influence-card";
import { GameService } from "@/services/game-service";
import { useToast } from "@/hooks/use-toast";
import type { GameState, InfluenceCard as InfluenceCardType } from "@/types/game";

interface ExchangeSelectionProps {
  matchId: string;
  cards: InfluenceCardType[];
  onResolved?: (next: GameState) => void;
}

/**
 * Overlay for keeping exactly 2 of the 4 offered cards during an Exchange.
 * The pool is the actor's hand (the server draws the 2 new cards straight into
 * it), so every id here is a card the backend knows about.
 *
 * Selection rules live in `@/lib/game/exchange-selection` so the UI can never
 * offer the server anything but two distinct pool ids; confirming reuses the
 * existing Exchange endpoint, which stays authoritative over the hand and the
 * deck.
 */
export function ExchangeSelection({
  matchId,
  cards,
  onResolved,
}: ExchangeSelectionProps) {
  const [selection, setSelection] = useState<ExchangeSelectionState>(
    createExchangeSelection,
  );
  const [busy, setBusy] = useState(false);
  const { success, error: notifyError } = useToast();

  const poolIds = useMemo(() => cards.map((card) => card.id), [cards]);
  // A challenge that resolves the action rebuilds the pool, so drop any pick
  // that is no longer offered instead of sending a stale id.
  const live = useMemo(
    () => pruneExchangeSelection(selection, poolIds),
    [selection, poolIds],
  );
  const selectedCount = exchangeSelectionCount(live);
  const complete = isExchangeSelectionComplete(live);

  const handleToggle = useCallback(
    (cardId: string) => {
      if (busy) return;
      setSelection((prev) => toggleExchangeCard(prev, cardId, poolIds));
    },
    [busy, poolIds],
  );

  async function handleConfirm() {
    if (busy || !complete) return;
    const keepCardIds = exchangeKeepCardIds(live, poolIds);
    if (keepCardIds.length !== EXCHANGE_KEEP_COUNT) return;

    setBusy(true);
    const result = await GameService.confirmExchange(matchId, keepCardIds);
    setBusy(false);

    if (result.ok) {
      success("কার্ড বদল সম্পন্ন হয়েছে!");
      onResolved?.(result.data);
    } else {
      notifyError(result.error.message);
    }
  }

  return (
    <div
      className="fixed inset-0 z-[55] flex items-center justify-center overflow-y-auto bg-black/85 backdrop-blur-md p-4 animate-fade-in"
      role="dialog"
      aria-modal="true"
      aria-label="কার্ড বদল নির্বাচন"
    >
      <div className="relative w-full max-w-lg space-y-5 rounded-3xl border-2 border-gold-500/40 bg-surface p-6 panel-emboss panel-texture shadow-2xl">
        <div>
          <h3 className="font-bengali text-xl font-bold text-ivory">
            কার্ড বদল (Exchange)
          </h3>
          <p className="mt-1 font-cinzel text-xs text-muted">
            Keep Selected Cards — choose exactly {EXCHANGE_KEEP_COUNT}
          </p>
        </div>

        <p className="font-bengali text-xs leading-relaxed text-parchment-300">
          ডেক থেকে ২টি নতুন কার্ড তোলা হয়েছে। আপনার হাতে এখন ৪টি কার্ড।
          নিচের কার্ডগুলোর মধ্য থেকে{" "}
          <strong className="text-gold-300">ঠিক ২টি রাখুন</strong> — বাকিগুলো ডেকে
          ফেরত যাবে। কার্ডের উপর ক্লিক করে বেছে নিন।
        </p>

        <div className="grid grid-cols-2 items-start gap-3 sm:grid-cols-4">
          {cards.map((card) => {
            const character = CHARACTER_MAP[card.characterId];
            const isSelected = live.selectedIds.includes(card.id);
            return (
              <button
                key={card.id}
                type="button"
                onClick={() => handleToggle(card.id)}
                disabled={busy}
                aria-pressed={isSelected}
                aria-label={`${character?.nameBn ?? card.characterId} — ${
                  isSelected ? "রাখা হয়েছে" : "ফেরত যাবে"
                }`}
                data-selected={isSelected}
                className={cn(
                  "group flex w-full cursor-pointer select-none flex-col items-center gap-2 rounded-2xl p-2 text-center",
                  "transition-[box-shadow,background-color,opacity] duration-200 motion-reduce:transition-none",
                  "focus-visible:outline-2 focus-visible:outline-gold-400",
                  isSelected
                    ? "bg-gold-500/10 shadow-gold ring-2 ring-gold-400"
                    : "bg-transparent ring-1 ring-white/10 hover:ring-white/25",
                  busy && "cursor-wait opacity-70",
                )}
              >
                {/* The lift is a transform, so selecting a card never resizes
                    its slot or reflows the grid. */}
                <span
                  className="relative block w-full transition-transform duration-200 motion-reduce:transition-none"
                  style={{
                    transform: isSelected ? "translateY(-4px)" : "none",
                  }}
                >
                  <InfluenceCard
                    characterId={card.characterId}
                    state="revealed"
                    size="sm"
                    label={`${character?.nameBn ?? card.characterId} — ${
                      isSelected ? "রাখা হয়েছে" : "ফেরত যাবে"
                    }`}
                    style={{ width: "100%" }}
                  />
                  {isSelected ? (
                    <span
                      aria-hidden
                      className="absolute -right-1 -top-1 flex size-6 items-center justify-center rounded-full bg-gold-400 text-deep-950 shadow-gold"
                    >
                      <Check className="size-4" />
                    </span>
                  ) : null}
                </span>

                <Badge
                  tone={isSelected ? "gold" : "neutral"}
                  className="h-5 px-2 text-[10px] leading-none"
                >
                  {isSelected ? "✓ রাখবেন" : "ফেরত"}
                </Badge>
              </button>
            );
          })}
        </div>

        <p
          aria-live="polite"
          className={cn(
            "text-center font-bengali text-[11px]",
            complete ? "font-bold text-gold-300" : "text-muted",
          )}
        >
          নির্বাচিত: {selectedCount}/{EXCHANGE_KEEP_COUNT}
          {complete ? " — দুটি কার্ড প্রস্তুত, নিশ্চিত করুন" : ""}
        </p>

        <Button
          variant="premium"
          fullWidth
          loading={busy}
          disabled={!complete || busy}
          onClick={() => void handleConfirm()}
        >
          <span className="flex flex-col items-center leading-tight">
            <span className="font-bengali">
              ঠিক ২টি কার্ড রাখুন
            </span>
            <span className="font-cinzel text-[10px] opacity-80">
              Confirm Exchange
            </span>
          </span>
        </Button>
      </div>
    </div>
  );
}