"use client";

import { useCallback, useMemo, useState } from "react";
import { Check } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils/cn";
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
import { audioEngine } from "@/lib/audio/audio-engine";
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
      // Decide the sound from the state we are about to move to, so the pick
      // and the unpick each get their own effect.
      const willSelect = !live.selectedIds.includes(cardId);
      setSelection((prev) => toggleExchangeCard(prev, cardId, poolIds));
      audioEngine.play(willSelect ? "cardPick" : "cardUnpick");
    },
    [busy, live.selectedIds, poolIds],
  );

  async function handleConfirm() {
    if (busy || !complete) return;
    const keepCardIds = exchangeKeepCardIds(live, poolIds);
    if (keepCardIds.length !== EXCHANGE_KEEP_COUNT) return;

    setBusy(true);
    const result = await GameService.confirmExchange(matchId, keepCardIds);
    setBusy(false);

    if (result.ok) {
      audioEngine.play("confirm");
      success("Exchange completed!");
      onResolved?.(result.data);
    } else {
      audioEngine.play("error");
      notifyError(result.error.message);
    }
  }

  return (
    <div
      className="fixed inset-0 z-[55] flex items-center justify-center overflow-y-auto bg-black/85 backdrop-blur-md p-4 animate-fade-in"
      role="dialog"
      aria-modal="true"
      aria-label="Exchange — Select Cards"
    >
      <div className="relative w-full max-w-lg space-y-5 rounded-3xl border-2 border-gold-500/40 bg-surface p-6 panel-emboss panel-texture shadow-2xl">
        <div>
          <h3 className="font-display text-xl font-bold text-ivory">
            Exchange
          </h3>
          <p className="mt-1 font-cinzel text-xs text-muted">
            Keep Selected Cards — choose exactly {EXCHANGE_KEEP_COUNT}
          </p>
        </div>

        <p className="text-xs leading-relaxed text-parchment-300">
          2 new cards have been drawn from the deck. You now hold 4 cards.
          From the cards below,{" "}
          <strong className="text-gold-300">keep exactly 2</strong> — the rest will be returned
          to the deck. Click a card directly, or use the{" "}
          <strong className="text-gold-300">Pick</strong> button below each card.
        </p>

        <div className="grid grid-cols-2 items-start gap-2 sm:grid-cols-4">
          {cards.map((card) => {
            const character = CHARACTER_MAP[card.characterId];
            const isSelected = live.selectedIds.includes(card.id);
            const cardLabel = `${character?.nameEn ?? card.characterId} — ${
              isSelected ? "Selected" : "Return"
            }`;
            return (
              <div
                key={card.id}
                data-selected={isSelected}
                className={cn(
                  "flex w-full select-none flex-col items-center gap-1.5 rounded-2xl p-1.5 text-center",
                  "transition-[box-shadow,background-color,opacity] duration-200 motion-reduce:transition-none",
                  isSelected
                    ? "bg-gold-500/10 shadow-gold ring-2 ring-gold-400"
                    : "bg-transparent ring-1 ring-white/10",
                  busy && "opacity-70",
                )}
              >
                {/* The lift is a transform, so picking a card never resizes
                    its slot or reflows the grid. */}
                <span
                  className="relative block transition-transform duration-200 motion-reduce:transition-none"
                  style={{
                    transform: isSelected ? "translateY(-4px)" : "none",
                  }}
                >
                  <InfluenceCard
                    characterId={card.characterId}
                    state="revealed"
                    size="xs"
                    label={cardLabel}
                    onClick={() => handleToggle(card.id)}
                    style={{ width: "clamp(58px, 15vw, 76px)" }}
                  />
                  {isSelected ? (
                    <span
                      aria-hidden
                      className="absolute -right-1 -top-1 flex size-5 items-center justify-center rounded-full bg-gold-400 text-deep-950 shadow-gold"
                    >
                      <Check className="size-3.5" />
                    </span>
                  ) : null}
                </span>

                <Button
                  variant={isSelected ? "gold" : "outline"}
                  size="sm"
                  fullWidth
                  disabled={busy}
                  onClick={() => handleToggle(card.id)}
                  aria-pressed={isSelected}
                  aria-label={cardLabel}
                  className="text-[11px] font-cinzel uppercase tracking-wider"
                >
                  {isSelected ? "✓ SELECTED" : "PICK"}
                </Button>
              </div>
            );
          })}
        </div>

        <p
          aria-live="polite"
          className={cn(
            "text-center text-[11px]",
            complete ? "font-bold text-gold-300" : "text-muted",
          )}
        >
          Selected: {selectedCount}/{EXCHANGE_KEEP_COUNT}
          {complete ? " — 2 cards ready, please confirm" : ""}
        </p>

        <Button
          variant="premium"
          fullWidth
          loading={busy}
          disabled={!complete || busy}
          onClick={() => void handleConfirm()}
        >
          <span className="font-cinzel text-xs font-bold uppercase tracking-wider">
            Confirm Exchange
          </span>
        </Button>
      </div>
    </div>
  );
}