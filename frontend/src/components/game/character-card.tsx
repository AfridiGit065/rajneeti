"use client";

import Image from "next/image";
import { useState } from "react";
import type { Character } from "@/types/character";
import { cn } from "@/lib/cn";
import { Coins } from "@/components/ui/icons";

const ACCENT_MAP: Record<Character["accent"], { ring: string; glow: string; border: string; text: string }> = {
  gold: { ring: "from-gold-500/25", glow: "text-gold-300", border: "border-gold-500/40", text: "text-gold-300" },
  crimson: { ring: "from-crimson-500/25", glow: "text-crimson-300", border: "border-crimson-500/40", text: "text-crimson-300" },
  forest: { ring: "from-forest-500/25", glow: "text-forest-300", border: "border-forest-500/40", text: "text-forest-300" },
  parchment: { ring: "from-parchment-500/25", glow: "text-parchment-300", border: "border-parchment-500/40", text: "text-parchment-300" },
};

interface CharacterCardProps {
  character: Character;
  size?: "sm" | "md" | "lg";
  imageFailed?: boolean;
  onImageError?: () => void;
  className?: string;
}

export function CharacterCard({
  character,
  size = "md",
  imageFailed: externalImageFailed,
  onImageError,
  className,
}: CharacterCardProps) {
  const accent = ACCENT_MAP[character.accent];
  const [internalImageFailed, setInternalImageFailed] = useState(false);
  const imageFailed = externalImageFailed ?? internalImageFailed;

  const artSizes = { sm: "h-44", md: "h-64", lg: "h-80" };
  const nameSizes = { sm: "text-base", md: "text-xl", lg: "text-2xl" };
  const enNameSizes = { sm: "text-[0.55rem]", md: "text-xs", lg: "text-sm" };

  function handleError() {
    setInternalImageFailed(true);
    onImageError?.();
  }

  return (
    <div
      className={cn(
        "group relative flex flex-col overflow-hidden rounded-2xl border bg-deep-900 panel-emboss",
        accent.border,
        className,
      )}
    >
      <div
        className={cn(
          "relative w-full overflow-hidden bg-gradient-to-b to-deep-950",
          accent.ring,
          artSizes[size],
        )}
      >
        {imageFailed ? (
          <div className="flex h-full w-full flex-col items-center justify-center gap-2 px-4">
            <span className={cn("font-bengali text-5xl font-bold opacity-80", accent.glow)}>
              {character.nameBn.slice(0, 1)}
            </span>
            <span className={cn("text-center text-sm font-medium", accent.text)}>
              {character.nameEn}
            </span>
          </div>
        ) : (
          <Image
            src={character.imagePath}
            alt={`${character.nameBn} — ${character.nameEn} কার্ড`}
            fill
            sizes="(max-width: 768px) 50vw, 300px"
            className="object-cover transition-transform duration-300 group-hover:scale-[1.04]"
            onError={handleError}
          />
        )}
        <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-deep-950/90 via-transparent to-deep-950/20" />
      </div>

      <div className="relative z-10 -mt-10 px-4 pb-4">
        <h3 className={cn("font-bengali font-semibold text-ivory", nameSizes[size])}>
          {character.nameBn}
        </h3>
        <p className={cn("mt-0.5 uppercase tracking-[0.25em] text-gold-400", enNameSizes[size])}>
          {character.nameEn}
        </p>
        <div className="divider-gold my-2.5" />
        <div className="space-y-1.5 text-parchment-300">
          <div className="flex items-start gap-2">
            <span className={cn("text-xs font-bold uppercase tracking-wider", accent.text)}>
              ক্ষমতা
            </span>
            <p className="text-sm leading-snug">
              <span className="font-medium text-ivory">{character.ability.nameBn}</span>
              {" — "}
              {character.ability.effect}
            </p>
          </div>
          {typeof character.ability.cost === "number" && character.ability.cost > 0 ? (
            <div className="flex items-center gap-1.5 text-xs text-gold-300">
              <Coins className="size-3.5" aria-hidden />
              খরচ: {character.ability.cost} কয়েন
            </div>
          ) : null}
        </div>
      </div>
    </div>
  );
}