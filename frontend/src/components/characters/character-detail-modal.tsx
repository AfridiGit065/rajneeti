"use client";

import Image from "next/image";
import { X, Shield, Coins } from "@/components/ui/icons";
import type { Character } from "@/types/character";

interface Props {
  character: Character | null;
  onClose: () => void;
}

const ACCENT_CFG = {
  gold: {
    border: "border-gold-500/50",
    glow: "shadow-gold",
    heading: "text-gold-gradient",
    abilityBg: "bg-forest-900/60 border-forest-500/30",
    blockBg: "bg-crimson-900/30 border-crimson-500/30",
    effectColor: "text-gold-300",
    overlayGrad: "radial-gradient(ellipse 80% 60% at 50% 0%, rgb(201 165 60 / 0.15), transparent 70%)",
  },
  crimson: {
    border: "border-crimson-500/50",
    glow: "",
    heading: "text-crimson-300",
    abilityBg: "bg-forest-900/60 border-forest-500/30",
    blockBg: "bg-crimson-900/30 border-crimson-500/30",
    effectColor: "text-crimson-300",
    overlayGrad: "radial-gradient(ellipse 80% 60% at 50% 0%, rgb(185 28 28 / 0.18), transparent 70%)",
  },
  forest: {
    border: "border-forest-500/50",
    glow: "",
    heading: "text-forest-300",
    abilityBg: "bg-forest-900/60 border-forest-500/30",
    blockBg: "bg-crimson-900/30 border-crimson-500/30",
    effectColor: "text-forest-300",
    overlayGrad: "radial-gradient(ellipse 80% 60% at 50% 0%, rgb(44 110 82 / 0.18), transparent 70%)",
  },
  parchment: {
    border: "border-parchment-500/40",
    glow: "",
    heading: "text-parchment-200",
    abilityBg: "bg-forest-900/60 border-forest-500/30",
    blockBg: "bg-crimson-900/30 border-crimson-500/30",
    effectColor: "text-parchment-200",
    overlayGrad: "radial-gradient(ellipse 80% 60% at 50% 0%, rgb(212 184 120 / 0.12), transparent 70%)",
  },
};

export function CharacterDetailModal({ character, onClose }: Props) {
  if (!character) return null;
  const cfg = ACCENT_CFG[character.accent];

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      role="dialog"
      aria-modal="true"
    >
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/80 backdrop-blur-sm"
        onClick={onClose}
      />

      {/* Panel */}
      <div
        className={`relative z-10 w-full max-w-2xl max-h-[92vh] overflow-y-auto rounded-3xl border-2 ${cfg.border} bg-surface panel-emboss panel-texture shadow-2xl`}
      >
        <div
          className="pointer-events-none absolute inset-0 rounded-3xl"
          style={{ background: cfg.overlayGrad }}
        />

        {/* Close */}
        <button
          type="button"
          onClick={onClose}
          className="absolute right-4 top-4 z-20 flex size-8 items-center justify-center rounded-full border border-white/10 bg-deep-950/70 text-muted hover:text-ivory transition-all"
        >
          <X className="size-4" />
        </button>

        <div className="relative z-10 p-6 sm:p-8 space-y-6">
          {/* Top: image + identity */}
          <div className="flex flex-col sm:flex-row gap-6">
            <div
              className={`relative shrink-0 w-full sm:w-48 aspect-[3/4] rounded-2xl overflow-hidden border-2 ${cfg.border} ${cfg.glow}`}
            >
              <Image
                src={character.imagePath}
                alt={character.nameBn}
                fill
                className="object-cover object-top"
                sizes="(max-width:640px) 100vw, 192px"
                priority
              />
            </div>

            <div className="flex flex-col justify-between gap-3 flex-1">
              <div className="space-y-1.5">
                <p className="text-xs font-bold uppercase tracking-widest text-muted font-cinzel">
                  {character.role}
                </p>
                <h2 className={`font-bengali text-4xl sm:text-5xl font-bold leading-tight ${cfg.heading}`}>
                  {character.nameBn}
                </h2>
                <p className="font-cinzel text-sm font-semibold text-muted tracking-widest">
                  {character.nameEn}
                </p>
                <p className="font-bengali text-sm text-parchment-300 italic">
                  {character.taglineBn}
                </p>
              </div>

              <p className="font-bengali text-sm leading-relaxed text-parchment-200 bg-deep-950/40 border border-white/5 rounded-xl p-3">
                {character.description}
              </p>

              <p className="font-cinzel text-xs text-muted italic text-right">
                &ldquo;{character.quote}&rdquo;
              </p>
            </div>
          </div>

          {/* Abilities */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className={`rounded-2xl border p-4 space-y-2 ${cfg.abilityBg}`}>
              <div className="flex items-center gap-2 mb-1">
                <Coins className="size-4 text-gold-400" />
                <span className="font-cinzel font-bold text-xs text-forest-300 uppercase tracking-wide">
                  Special Action
                </span>
              </div>
              <p className="font-display font-bold text-xl text-ivory">{character.ability.nameEn}</p>
              <p className="font-bengali text-xs text-muted">{character.ability.nameBn}</p>
              <p className={`text-2xl font-bold font-cinzel ${cfg.effectColor}`}>
                {character.ability.effect}
              </p>
              {character.ability.effectDetail && (
                <p className="text-xs text-parchment-300 leading-relaxed font-bengali">
                  {character.ability.effectDetail}
                </p>
              )}
            </div>

            <div className={`rounded-2xl border p-4 space-y-2 ${cfg.blockBg}`}>
              <div className="flex items-center gap-2 mb-1">
                <Shield className="size-4 text-crimson-400" />
                <span className="font-cinzel font-bold text-xs text-crimson-300 uppercase tracking-wide">
                  Counter-Action (Block)
                </span>
              </div>
              <p className="font-display font-bold text-xl text-ivory">{character.block.nameEn}</p>
              <p className="font-bengali text-xs text-muted">{character.block.nameBn}</p>
              <p className="text-xs text-parchment-300 leading-relaxed font-bengali">
                {character.block.detail}
              </p>
            </div>
          </div>

          <p className="text-center text-[10px] text-muted/60">
            * All characters and roles are purely fictional game elements.
          </p>
        </div>
      </div>
    </div>
  );
}
