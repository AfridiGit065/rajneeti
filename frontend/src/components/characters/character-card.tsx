"use client";

import Image from "next/image";
import { Shield, Coins } from "@/components/ui/icons";
import type { Character } from "@/types/character";

interface Props {
  character: Character;
  onClick: (character: Character) => void;
}

const ACCENT_CFG = {
  gold: {
    border: "border-gold-500/30 hover:border-gold-400/70",
    glow: "hover:shadow-gold",
    nameBg: "from-gold-950/90 to-deep-950/95",
    nameColor: "text-gold-gradient",
    abilityBg: "bg-forest-900/80 border-forest-500/30",
    blockBg: "bg-crimson-900/60 border-crimson-500/30",
    effect: "text-gold-300",
    shimmer: "from-transparent via-gold-300/10 to-transparent",
  },
  crimson: {
    border: "border-crimson-500/30 hover:border-crimson-400/70",
    glow: "",
    nameBg: "from-crimson-950/90 to-deep-950/95",
    nameColor: "text-crimson-300",
    abilityBg: "bg-forest-900/80 border-forest-500/30",
    blockBg: "bg-crimson-900/60 border-crimson-500/30",
    effect: "text-crimson-300",
    shimmer: "from-transparent via-crimson-300/10 to-transparent",
  },
  forest: {
    border: "border-forest-500/30 hover:border-forest-400/70",
    glow: "",
    nameBg: "from-forest-950/90 to-deep-950/95",
    nameColor: "text-forest-300",
    abilityBg: "bg-forest-900/80 border-forest-500/30",
    blockBg: "bg-crimson-900/60 border-crimson-500/30",
    effect: "text-forest-300",
    shimmer: "from-transparent via-forest-300/10 to-transparent",
  },
  parchment: {
    border: "border-parchment-500/25 hover:border-parchment-400/60",
    glow: "",
    nameBg: "from-deep-950/90 to-deep-950/95",
    nameColor: "text-parchment-200",
    abilityBg: "bg-forest-900/80 border-forest-500/30",
    blockBg: "bg-crimson-900/60 border-crimson-500/30",
    effect: "text-parchment-200",
    shimmer: "from-transparent via-parchment-300/8 to-transparent",
  },
};

export function CharacterCard({ character, onClick }: Props) {
  const cfg = ACCENT_CFG[character.accent];

  return (
    <button
      type="button"
      onClick={() => onClick(character)}
      className={`
        group relative flex flex-col overflow-hidden rounded-2xl border-2 bg-deep-900
        cursor-pointer select-none text-left transition-all duration-300
        ${cfg.border} ${cfg.glow}
        hover:-translate-y-2 hover:scale-[1.02]
        focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-gold-400/50
      `}
      aria-label={`${character.nameBn} - ${character.nameEn} বিস্তারিত দেখুন`}
    >
      {/* Shimmer sweep on hover */}
      <div
        className={`pointer-events-none absolute inset-0 z-10 -skew-x-12 translate-x-[-150%] bg-gradient-to-r ${cfg.shimmer} transition-transform duration-700 group-hover:translate-x-[150%]`}
      />

      {/* Card artwork — consistent 3:4 aspect ratio */}
      <div className="relative w-full aspect-[3/4] overflow-hidden bg-deep-950">
        <Image
          src={character.imagePath}
          alt={character.nameBn}
          fill
          className="object-cover object-top transition-transform duration-500 group-hover:scale-105"
          sizes="(max-width:640px) 100vw, (max-width:1024px) 50vw, 20vw"
        />
        {/* Bottom fade into the info section */}
        <div className="absolute bottom-0 left-0 right-0 h-24 bg-gradient-to-t from-deep-950 to-transparent" />
      </div>

      {/* Info section */}
      <div className={`relative flex flex-col gap-2 px-4 pb-4 pt-3 bg-gradient-to-b ${cfg.nameBg}`}>
        {/* Name */}
        <div>
          <h3 className={`font-bengali text-2xl font-bold leading-tight ${cfg.nameColor}`}>
            {character.nameBn}
          </h3>
          <p className="font-cinzel text-[10px] font-semibold uppercase tracking-widest text-muted">
            {character.nameEn}
          </p>
          <p className="font-bengali text-[11px] text-parchment-300/80 italic mt-0.5">
            {character.taglineBn}
          </p>
        </div>

        {/* Ability + Block mini-chips */}
        <div className="flex flex-col gap-1.5 mt-1">
          <div className={`flex items-start gap-2 rounded-lg border px-2.5 py-1.5 ${cfg.abilityBg}`}>
            <Coins className="size-3.5 text-gold-400 mt-0.5 shrink-0" />
            <div className="min-w-0">
              <p className="font-bengali text-xs font-bold text-ivory leading-tight">
                {character.ability.nameBn}
                <span className={`ml-1.5 font-cinzel text-[10px] font-bold ${cfg.effect}`}>
                  {character.ability.effect}
                </span>
              </p>
              <p className="text-[10px] text-muted">{character.ability.nameEn}</p>
            </div>
          </div>

          <div className={`flex items-start gap-2 rounded-lg border px-2.5 py-1.5 ${cfg.blockBg}`}>
            <Shield className="size-3.5 text-crimson-400 mt-0.5 shrink-0" />
            <div className="min-w-0">
              <p className="font-bengali text-xs font-bold text-ivory leading-tight">
                {character.block.nameBn}
              </p>
              <p className="text-[10px] text-muted">{character.block.nameEn}</p>
            </div>
          </div>
        </div>

        {/* CTA hint */}
        <p className="text-center text-[10px] text-muted/50 pt-1 group-hover:text-muted/80 transition-colors">
          Click to view details →
        </p>
      </div>
    </button>
  );
}
