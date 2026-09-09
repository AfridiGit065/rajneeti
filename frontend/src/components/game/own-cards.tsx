"use client";

import { CHARACTER_MAP } from "@/lib/game/characters";
import { cn } from "@/lib/cn";
import { Eye } from "@/components/ui/icons";
import type { InfluenceCard } from "@/types/game";
import type { Character } from "@/types/character";

const ACCENT: Record<Character["accent"], { border: string; text: string; bg: string }> = {
  gold: { border: "border-gold-500/50", text: "text-gold-300", bg: "from-gold-500/15" },
  crimson: { border: "border-crimson-500/50", text: "text-crimson-300", bg: "from-crimson-500/15" },
  forest: { border: "border-forest-500/50", text: "text-forest-300", bg: "from-forest-500/15" },
  parchment: { border: "border-parchment-500/50", text: "text-parchment-300", bg: "from-parchment-500/15" },
};

function SecretCard({ card }: { card: InfluenceCard }) {
  const character = CHARACTER_MAP[card.characterId];
  if (!character) return null;
  const accent = ACCENT[character.accent];

  return (
    <div
      className={cn(
        "flex h-32 w-20 flex-col items-center justify-center gap-1 rounded-xl border bg-gradient-to-b to-deep-900 panel-emboss",
        accent.border,
        accent.bg,
      )}
    >
      <span className={cn("font-bengali text-3xl font-bold", accent.text)}>
        {character.nameBn.slice(0, 1)}
      </span>
      <span className={cn("px-1 text-center text-[0.55rem] font-semibold uppercase tracking-wider", accent.text)}>
        {character.nameEn}
      </span>
      <span className="mt-1 rounded-full border border-gold-500/30 bg-gold-500/10 px-2 py-0.5 text-[0.55rem] font-semibold text-gold-300">
        গোপন
      </span>
    </div>
  );
}

export function OwnCards({
  cards,
  className,
}: {
  cards: InfluenceCard[];
  className?: string;
}) {
  return (
    <div className={cn("w-full max-w-md", className)}>
      <p className="flex items-center justify-center gap-1.5 text-xs font-semibold uppercase tracking-[0.15em] text-muted">
        <Eye className="size-3.5 text-gold-400" aria-hidden />
        আপনার গোপন কার্ড
        <span className="normal-case tracking-normal text-muted/70">— শুধু আপনি দেখবেন</span>
      </p>
      <div className="mt-2 flex items-end justify-center gap-3">
        {cards.map((card) => (
          <SecretCard key={card.id} card={card} />
        ))}
        {cards.length === 0 ? (
          <p className="text-sm font-semibold text-crimson-300">কোনো ইনফ্লুয়েন্স বাকি নেই</p>
        ) : null}
      </div>
    </div>
  );
}