"use client";

import { cn } from "@/lib/cn";
import { InfluenceCard, INFLUENCE_CARD_WIDTHS } from "./influence-card";
import type { InfluenceCardSize } from "./influence-card";
import type { InfluenceCard as InfluenceCardType } from "@/types/game";

interface PlayerInfluenceCardsProps {
  cards: InfluenceCardType[];
  /** Opponent view — backs are shown until a card is revealed. */
  faceDown?: boolean;
  size?: InfluenceCardSize;
  className?: string;
}

export function PlayerInfluenceCards({
  cards,
  faceDown = false,
  size = "sm",
  className,
}: PlayerInfluenceCardsProps) {
  if (cards.length === 0) {
    return (
      <div
        className={cn("flex items-end gap-1", className)}
        aria-label="০ ইনফ্লুয়েন্স — অপসারিত"
      >
        {[0, 1].map((slot) => (
          <div
            key={slot}
            style={{ aspectRatio: "3 / 4" }}
            className={cn(
              "flex items-start justify-center rounded-2xl border border-dashed border-crimson-500/40 bg-deep-950/60 pt-2 sm:pt-2.5",
              INFLUENCE_CARD_WIDTHS[size],
            )}
            aria-hidden
          >
            <span className="font-bengali text-base font-bold text-crimson-400/80">০</span>
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className={cn("flex items-end gap-1", className)}>
      {cards.map((card, index) => {
        const showFront = !faceDown || card.revealed;
        return (
          <InfluenceCard
            key={card.id}
            characterId={card.characterId}
            state={showFront ? "revealed" : "hidden"}
            size={size}
            animation="draw"
            style={{ animationDelay: `${index * 90}ms` }}
          />
        );
      })}
    </div>
  );
}