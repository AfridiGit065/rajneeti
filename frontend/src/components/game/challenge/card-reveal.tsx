"use client";

import { cn } from "@/lib/cn";
import { CHARACTER_MAP } from "@/lib/game/characters";
import { InfluenceCard } from "../influence-card";
import { CheckCheck, Eye, RefreshCw } from "@/components/ui/icons";
import type { CharacterId } from "@/types/character";

interface CardRevealProps {
  characterId: CharacterId;
  variant?: "reveal" | "replacement";
  label?: string;
  className?: string;
}

/**
 * Dramatic full-screen reveal. `reveal` flips a character card face-up with a
 * glow burst; `replacement` deals a fresh hidden card back in from the deck.
 */
export function CardReveal({
  characterId,
  variant = "reveal",
  label,
  className,
}: CardRevealProps) {
  const character = CHARACTER_MAP[characterId];
  const isReplacement = variant === "replacement";

  return (
    <div
      className={cn(
        "fixed inset-0 z-[70] flex flex-col items-center justify-center gap-6 bg-deep-950/90 px-4 backdrop-blur-md animate-fade-in",
        className,
      )}
      role="dialog"
      aria-modal="true"
      aria-label={isReplacement ? "Replacement Card" : "Revealed Card"}
    >
      <div className="flex flex-col items-center gap-2 text-center animate-fade-up">
        <span
          className={cn(
            "inline-flex items-center gap-2 rounded-full border px-4 py-1.5 font-cinzel text-xs font-semibold tracking-wider uppercase",
            isReplacement
              ? "border-forest-400/45 bg-forest-500/12 text-forest-200"
              : "border-gold-500/45 bg-gold-500/12 text-gold-200",
          )}
        >
          {isReplacement ? (
            <RefreshCw className="size-4" aria-hidden />
          ) : (
            <Eye className="size-4" aria-hidden />
          )}
          {label ?? (isReplacement ? "Replacement Card" : "Revealed Card")}
        </span>
        <h3 className="font-display text-3xl font-bold text-ivory sm:text-4xl">
          {isReplacement ? "Drawing new card from deck…" : `${character.nameBn} (${character.nameEn})`}
        </h3>
        {isReplacement ? (
          <p className="max-w-md text-sm text-muted">
            Drawing a fresh hidden influence card to replace the revealed card.
          </p>
        ) : null}
      </div>

      <div className="flex items-end gap-4 animate-zoom-in">
        {isReplacement ? (
          <div className="scale-[1.45] sm:scale-[1.6]">
            <InfluenceCard
              characterId={characterId}
              state="hidden"
              size="lg"
              animation="draw"
            />
          </div>
        ) : (
          <div className="relative">
            <div className="absolute -inset-6 rounded-full bg-gold-500/25 blur-2xl animate-glow-pulse" aria-hidden />
            <div className="relative scale-[1.45] sm:scale-[1.6] animate-zoom-in">
              <InfluenceCard
                characterId={characterId}
                state="revealed"
                size="lg"
                animation="reveal"
              />
            </div>
          </div>
        )}
      </div>

      {!isReplacement ? (
        <p className="flex items-center gap-2 text-sm font-medium text-muted animate-fade-up">
          <CheckCheck className="size-4 text-forest-300" aria-hidden />
          The card returns to the court deck and a replacement card is drawn.
        </p>
      ) : null}
    </div>
  );
}