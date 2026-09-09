"use client";

import { useState } from "react";
import Image from "next/image";
import type { CSSProperties } from "react";
import { cn } from "@/lib/cn";
import { CHARACTER_MAP } from "@/lib/game/characters";
import { Coins } from "@/components/ui/icons";
import type { Character, CharacterId } from "@/types/character";

export type InfluenceCardState = "hidden" | "revealed" | "discarded";
export type InfluenceCardAnimation = "none" | "draw" | "return" | "discard" | "reveal";
export type InfluenceCardSize = "xs" | "sm" | "md" | "lg";

export const INFLUENCE_CARD_WIDTHS: Record<InfluenceCardSize, string> = {
  xs: "w-10 sm:w-12",
  sm: "w-12 sm:w-16",
  md: "w-16 sm:w-24 lg:w-28",
  lg: "w-24 sm:w-32 lg:w-40",
};

const SHOW_DETAILS: Record<InfluenceCardSize, { en: boolean; role: boolean; ability: boolean }> = {
  xs: { en: false, role: false, ability: false },
  sm: { en: false, role: true, ability: false },
  md: { en: true, role: true, ability: true },
  lg: { en: true, role: true, ability: true },
};

const ACCENT_MAP: Record<
  Character["accent"],
  { ring: string; text: string; border: string; chip: string }
> = {
  gold: {
    ring: "from-gold-500/30",
    text: "text-gold-300",
    border: "border-gold-500/50",
    chip: "border-gold-500/40 bg-gold-500/10 text-gold-200",
  },
  crimson: {
    ring: "from-crimson-500/30",
    text: "text-crimson-300",
    border: "border-crimson-500/50",
    chip: "border-crimson-500/40 bg-crimson-500/10 text-crimson-200",
  },
  forest: {
    ring: "from-forest-500/30",
    text: "text-forest-300",
    border: "border-forest-500/50",
    chip: "border-forest-500/40 bg-forest-500/10 text-forest-200",
  },
  parchment: {
    ring: "from-parchment-500/30",
    text: "text-parchment-300",
    border: "border-parchment-500/50",
    chip: "border-parchment-500/40 bg-parchment-500/10 text-parchment-100",
  },
};

interface InfluenceCardProps {
  characterId: CharacterId;
  state?: InfluenceCardState;
  size?: InfluenceCardSize;
  animation?: InfluenceCardAnimation;
  label?: string;
  onClick?: () => void;
  className?: string;
  style?: CSSProperties;
}

function CardBack() {
  return (
    <div className="influence-card-back flex h-full w-full rounded-2xl p-1.5 sm:p-2">
      <div className="influence-card-back-inner relative flex h-full w-full flex-col items-center justify-center rounded-xl">
        <span className="absolute left-1 top-1 size-1 rounded-full bg-gold-400/80 sm:size-1.5" aria-hidden />
        <span className="absolute right-1 top-1 size-1 rounded-full bg-gold-400/80 sm:size-1.5" aria-hidden />
        <span className="absolute bottom-1 left-1 size-1 rounded-full bg-gold-400/80 sm:size-1.5" aria-hidden />
        <span className="absolute bottom-1 right-1 size-1 rounded-full bg-gold-400/80 sm:size-1.5" aria-hidden />
        <span className="absolute left-0 top-1/2 h-8 w-px -translate-y-1/2 bg-gradient-to-b from-transparent via-gold-500/40 to-transparent" aria-hidden />
        <span className="absolute right-0 top-1/2 h-8 w-px -translate-y-1/2 bg-gradient-to-b from-transparent via-gold-500/40 to-transparent" aria-hidden />

        <span className="flex size-6 items-center justify-center rounded-full border border-gold-500/50 bg-deep-950/70 font-bengali text-xs font-bold text-gold-gradient shadow-gold sm:size-8 sm:text-sm">
          র
        </span>
        <span className="mt-0.5 max-w-full truncate px-1 font-cinzel text-[0.4rem] font-bold uppercase tracking-[0.3em] text-gold-300/90 sm:text-[0.5rem]">
          রাজনীতি
        </span>
      </div>
    </div>
  );
}

export function InfluenceCard({
  characterId,
  state = "hidden",
  size = "md",
  animation = "none",
  label,
  onClick,
  className,
  style,
}: InfluenceCardProps) {
  const character = CHARACTER_MAP[characterId];
  const [imageFailed, setImageFailed] = useState(false);
  if (!character) return null;

  const accent = ACCENT_MAP[character.accent];
  const details = SHOW_DETAILS[size];
  const flipped = state === "revealed" || state === "discarded";
  const animationClass =
    animation === "none" ? "" : `animate-card-${animation}`;

  return (
    <div
      data-state={state}
      role={onClick ? "button" : undefined}
      tabIndex={onClick ? 0 : undefined}
      aria-label={
        label ??
        (flipped
          ? `${character.nameBn} — ${character.ability.nameBn}`
          : "গোপন প্রভাব কার্ড")
      }
      onClick={onClick}
      onKeyDown={
        onClick
          ? (event) => {
              if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                onClick();
              }
            }
          : undefined
      }
      className={cn(
        "influence-card relative aspect-[3/4] select-none",
        INFLUENCE_CARD_WIDTHS[size],
        animationClass,
        onClick && "cursor-pointer ring-gold-focus",
        state === "discarded" && "is-discarded opacity-80 grayscale",
        className,
      )}
      style={style}
    >
      <div
        className={cn(
          "influence-card-inner absolute inset-0",
          flipped && "is-revealed",
        )}
      >
        {/* Back face — shown while the card is hidden */}
        <div className="influence-card-face influence-card-face-back absolute inset-0">
          <CardBack />
        </div>

        {/* Front face — artwork, name, ability and role styling */}
        <div className="influence-card-face influence-card-face-front absolute inset-0">
          <div
            key={state}
            className={cn(
              "relative flex h-full w-full flex-col overflow-hidden rounded-2xl border bg-deep-900 panel-emboss",
              accent.border,
              state === "revealed" && animation === "reveal" && "animate-card-reveal",
            )}
          >
            <div
              className={cn(
                "relative w-full flex-1 overflow-hidden bg-gradient-to-b to-deep-950",
                accent.ring,
              )}
            >
              {imageFailed ? (
                <div className="flex h-full w-full flex-col items-center justify-center gap-1 p-1">
                  <span className={cn("font-bengali text-2xl font-bold sm:text-3xl", accent.text)}>
                    {character.nameBn.slice(0, 1)}
                  </span>
                  <span className="text-[0.5rem] font-bold uppercase tracking-wider text-muted">
                    {character.nameEn}
                  </span>
                </div>
              ) : (
                <Image
                  src={character.imagePath}
                  alt=""
                  fill
                  sizes="(max-width: 640px) 40vw, 180px"
                  className="object-cover object-top"
                  onError={() => setImageFailed(true)}
                />
              )}
              <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-deep-950 via-deep-950/35 to-transparent" />
            </div>

            <div className="relative -mt-7 space-y-1 px-1.5 pb-1.5 sm:-mt-9 sm:px-2 sm:pb-2">
              {details.en ? (
                <p className="truncate font-cinzel text-[0.5rem] font-bold uppercase tracking-[0.2em] text-gold-400 sm:text-[0.6rem]">
                  {character.nameEn}
                </p>
              ) : null}
              <h4 className="truncate font-bengali font-bold leading-tight text-ivory">
                {size === "xs" || size === "sm" ? (
                  <span className="text-[0.6rem] sm:text-[0.7rem]">{character.nameBn}</span>
                ) : (
                  <span className="text-xs sm:text-sm lg:text-base">{character.nameBn}</span>
                )}
              </h4>

              {details.role ? (
                <span
                  className={cn(
                    "inline-flex max-w-full truncate rounded-full border px-1.5 py-px font-bengali text-[0.5rem] font-semibold sm:text-[0.6rem]",
                    accent.chip,
                  )}
                >
                  {character.role}
                </span>
              ) : null}

              {details.ability ? (
                <div className="flex items-center gap-1 pt-0.5 text-muted">
                  {typeof character.ability.cost === "number" && character.ability.cost > 0 ? (
                    <Coins className="size-2.5 shrink-0 text-gold-400 sm:size-3" aria-hidden />
                  ) : null}
                  <p className="truncate text-[0.55rem] leading-tight text-muted sm:text-[0.65rem]">
                    <span className={cn("font-semibold", accent.text)}>{character.ability.nameBn}</span>
                    {" — "}
                    {character.ability.effect}
                  </p>
                </div>
              ) : null}
            </div>

            {/* Lost-influence marker */}
            {state === "discarded" ? (
              <span className="pointer-events-none absolute inset-x-0 top-1/3 z-10 -rotate-6 border-y border-crimson-500/60 bg-crimson-600/75 px-1 py-px text-center text-[0.5rem] font-black uppercase tracking-[0.2em] text-ivory sm:text-[0.6rem]">
                অপসারিত
              </span>
            ) : null}
          </div>
        </div>
      </div>
    </div>
  );
}