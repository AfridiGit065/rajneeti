"use client";

import { cn } from "@/lib/cn";
import { Eye } from "@/components/ui/icons";
import { InfluenceCard } from "./influence-card";
import type { InfluenceCard as InfluenceCardType } from "@/types/game";

export function OwnCards({
  cards,
  className,
}: {
  cards: InfluenceCardType[];
  className?: string;
}) {
  return (
    <div className={cn("w-full max-w-md", className)}>
      <p className="flex items-center justify-center gap-1.5 text-xs font-semibold uppercase tracking-[0.15em] text-muted">
        <Eye className="size-3.5 text-gold-400" aria-hidden />
        আপনার গোপন কার্ড
        <span className="normal-case tracking-normal text-muted/70">— শুধু আপনি দেখবেন</span>
      </p>
      <div className="mt-2 flex items-end justify-center gap-3 sm:gap-4">
        {cards.length > 0 ? (
          cards.map((card, index) => (
            <InfluenceCard
              key={card.id}
              characterId={card.characterId}
              state="revealed"
              size="md"
              animation="draw"
              style={{ animationDelay: `${index * 120}ms` }}
            />
          ))
        ) : (
          <p className="text-sm font-semibold text-crimson-300">কোনো ইনফ্লুয়েন্স বাকি নেই</p>
        )}
      </div>
    </div>
  );
}