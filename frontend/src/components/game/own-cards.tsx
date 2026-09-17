"use client";

import { cn } from "@/lib/cn";
import { Eye, Lock } from "@/components/ui/icons";
import { InfluenceCard } from "./influence-card";
import type { InfluenceCard as InfluenceCardType } from "@/types/game";

export function OwnCards({
  cards,
  className,
  responseMode = false,
}: {
  cards: InfluenceCardType[];
  className?: string;
  responseMode?: boolean;
}) {
  // Deduplicate cards by id to prevent duplicate rendering
  const seenIds = new Set<string>();
  const uniqueCards = cards.filter((c) => {
    if (!c?.id || seenIds.has(c.id)) return false;
    seenIds.add(c.id);
    return true;
  });

  const activeCards = uniqueCards.filter((c) => !c.revealed);
  const lostCards = uniqueCards.filter((c) => c.revealed);

  return (
    <div className={cn("flex flex-col items-center select-none", className)}>
      {/* Header label */}
      <div className={cn("flex items-center gap-2", responseMode ? "mb-1.5" : "mb-2")}>
        <span
          className={cn(
            "flex items-center gap-1.5 rounded-full border border-gold-500/35 bg-gold-500/10 shadow-gold",
            responseMode ? "px-2.5 py-0.5" : "px-3 py-1",
          )}
        >
          <Eye className="size-3 text-gold-400" aria-hidden />
          <span
            className={cn(
              "font-cinzel font-bold uppercase tracking-widest text-gold-300",
              responseMode ? "text-[10px]" : "text-[11px]",
            )}
          >
            YOUR INFLUENCE CARDS
          </span>
          <span
            className={cn(
              "font-bengali text-gold-400/80 font-medium",
              responseMode ? "text-[10px]" : "text-[11px]",
            )}
          >
            (আপনার কার্ড)
          </span>
        </span>
        <span className="flex items-center gap-1 text-[10px] font-medium uppercase tracking-widest text-muted/60">
          <Lock className="size-2.5" aria-hidden />
          Secret
        </span>
      </div>

      {/* Cards — Normal: 190–215px wide; Response Mode: 165–195px wide */}
      <div
        className={cn(
          "flex items-end justify-center",
          responseMode ? "gap-3 sm:gap-4 lg:gap-5" : "gap-4 sm:gap-6 lg:gap-8",
        )}
      >
        {uniqueCards.length > 0 ? (
          uniqueCards.map((card, index) => (
            <div
              key={card.id}
              className="flex flex-col items-center gap-1.5 transition-transform hover:-translate-y-1"
            >
              <div
                className={cn(
                  "relative rounded-2xl",
                  !card.revealed &&
                    "shadow-[0_12px_36px_rgba(0,0,0,0.6),0_0_24px_rgba(201,165,60,0.18)]",
                  card.revealed && "opacity-60 grayscale-[40%]",
                )}
              >
                <InfluenceCard
                  characterId={card.characterId}
                  state={card.revealed ? "discarded" : "revealed"}
                  size="xl"
                  animation="draw"
                  style={{
                    // Normal state: 190-215px wide (Req 8).
                    // Response state: 165-195px wide (Req 8) maintaining 3:4 aspect ratio.
                    width: responseMode
                      ? "clamp(165px, 15svh, 192px)"
                      : "clamp(190px, 18svh, 215px)",
                    animationDelay: `${index * 120}ms`,
                  }}
                />
              </div>
              {/* Card status pill */}
              {card.revealed ? (
                <span className="inline-flex items-center gap-1 rounded-full border border-crimson-500/50 bg-crimson-950/70 px-2 py-0.2 text-[8.5px] font-bold uppercase tracking-wider text-crimson-300 shadow-sm">
                  Lost / উন্মোচিত
                </span>
              ) : (
                <span className="inline-flex items-center gap-1 rounded-full border border-forest-400/40 bg-forest-900/60 px-2 py-0.2 text-[8.5px] font-bold tracking-wide text-forest-300 shadow-sm">
                  Active / সচল
                </span>
              )}
            </div>
          ))
        ) : (
          <div className="flex items-center justify-center rounded-xl border border-crimson-500/30 bg-crimson-950/20 px-5 py-4">
            <p className="text-xs font-semibold text-crimson-300">
              কোনো প্রভাব অবশিষ্ট নেই — (No Influence Left)
            </p>
          </div>
        )}
      </div>

      {/* Active card count indicator */}
      {cards.length > 0 && (
        <div
          className={cn(
            "flex items-center gap-3 text-muted/60",
            responseMode ? "mt-1 text-[9.5px]" : "mt-2 text-[10px]",
          )}
        >
          <span className="flex items-center gap-1">
            <span className="size-1.5 rounded-full bg-forest-400 inline-block" />
            {activeCards.length} active
          </span>
          {lostCards.length > 0 && (
            <span className="flex items-center gap-1">
              <span className="size-1.5 rounded-full bg-crimson-400 inline-block" />
              {lostCards.length} lost
            </span>
          )}
        </div>
      )}
    </div>
  );
}