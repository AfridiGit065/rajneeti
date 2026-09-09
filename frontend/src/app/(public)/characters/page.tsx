"use client";

import { useState } from "react";
import { Sparkles, ScrollText } from "@/components/ui/icons";
import { CHARACTERS } from "@/lib/game/characters";
import { CharacterCard } from "@/components/characters/character-card";
import { CharacterDetailModal } from "@/components/characters/character-detail-modal";
import type { Character } from "@/types/character";

export default function CharactersPage() {
  const [selected, setSelected] = useState<Character | null>(null);

  return (
    <div className="space-y-10 pb-16">
      {/* ── Page Header ─────────────────────────────────── */}
      <section className="relative overflow-hidden rounded-3xl border border-forest-500/25 bg-surface panel-emboss panel-texture p-8 sm:p-10 text-center">
        <div
          className="pointer-events-none absolute inset-0 opacity-50"
          style={{
            background:
              "radial-gradient(ellipse 70% 60% at 50% 0%, rgb(201 165 60 / 0.18), transparent 65%)," +
              "radial-gradient(ellipse 50% 40% at 20% 100%, rgb(44 110 82 / 0.15), transparent 60%)",
          }}
        />

        <div className="relative z-10 space-y-3 max-w-2xl mx-auto">
          <div className="flex items-center justify-center gap-2">
            <Sparkles className="size-4 text-gold-400" />
            <span className="font-cinzel text-xs font-bold uppercase tracking-widest text-gold-400">
              Character Gallery
            </span>
            <Sparkles className="size-4 text-gold-400" />
          </div>

          <h1 className="font-display text-3xl sm:text-4xl font-bold text-ivory">
            The Five Roles of Court
          </h1>
          <p className="text-sm sm:text-base text-parchment-300 leading-relaxed max-w-xl mx-auto">
            Each role holds distinct actions and counter-measures. Choose when to claim, when to bluff, and when to challenge.
          </p>

          <div className="flex items-center justify-center gap-2 pt-1">
            <ScrollText className="size-3.5 text-muted" />
            <p className="text-xs text-muted">
              Click any character card to view full abilities and lore
            </p>
          </div>
        </div>
      </section>

      {/* ── Character Grid ───────────────────────────────── */}
      <section>
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4 sm:gap-5">
          {CHARACTERS.map((character) => (
            <CharacterCard
              key={character.id}
              character={character}
              onClick={setSelected}
            />
          ))}
        </div>
      </section>

      {/* ── Summary Table ────────────────────────────────── */}
      <section className="rounded-2xl border border-forest-500/20 bg-surface panel-texture overflow-hidden">
        <div className="px-5 py-4 border-b border-forest-500/20 flex items-center gap-2">
          <ScrollText className="size-4 text-gold-400" />
          <h2 className="font-cinzel font-bold text-base text-ivory uppercase tracking-wider">
            Ability &amp; Counter-Action Summary
          </h2>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-forest-500/15 text-xs font-semibold uppercase tracking-wider text-muted font-cinzel">
                <th className="px-4 py-3 text-left">Character</th>
                <th className="px-4 py-3 text-left">Action</th>
                <th className="px-4 py-3 text-left">Effect</th>
                <th className="px-4 py-3 text-left">Counter-Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-forest-500/10">
              {CHARACTERS.map((c) => (
                <tr
                  key={c.id}
                  onClick={() => setSelected(c)}
                  className="hover:bg-deep-900/60 cursor-pointer transition-colors"
                >
                  <td className="px-4 py-3">
                    <span className="font-bengali font-bold text-ivory">{c.nameBn}</span>
                    <span className="block font-cinzel text-[10px] text-muted">{c.nameEn}</span>
                  </td>
                  <td className="px-4 py-3">
                    <span className="text-parchment-200">{c.ability.nameEn}</span>
                    <span className="block font-bengali text-[10px] text-muted">{c.ability.nameBn}</span>
                  </td>
                  <td className="px-4 py-3">
                    <span className="font-cinzel font-bold text-gold-300">{c.ability.effect}</span>
                  </td>
                  <td className="px-4 py-3">
                    <span className="text-parchment-200">{c.block.nameEn}</span>
                    <span className="block font-bengali text-[10px] text-muted">{c.block.nameBn}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {/* ── Disclaimer ──────────────────────────────────── */}
      <p className="text-center text-xs text-muted/60 max-w-lg mx-auto">
        ⚠️ All characters and abilities are purely fictional and designed for game entertainment purposes.
      </p>

      {/* ── Detail Modal ─────────────────────────────────── */}
      {selected && (
        <CharacterDetailModal
          character={selected}
          onClose={() => setSelected(null)}
        />
      )}
    </div>
  );
}
