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
              Character Gallery · চরিত্র পরিচিতি
            </span>
            <Sparkles className="size-4 text-gold-400" />
          </div>

          <h1 className="font-bengali text-3xl sm:text-4xl font-bold text-ivory">
            রাজনীতির পাঁচ খেলোয়াড়
          </h1>
          <p className="font-bengali text-sm sm:text-base text-parchment-300 leading-relaxed">
            প্রতিটি চরিত্রের নিজস্ব কৌশল আছে। সঠিক চরিত্র বেছে নিন, প্রতিপক্ষকে ধোঁকা দিন —
            ক্ষমতার খেলায় টিকে থাকুন।
          </p>

          <div className="flex items-center justify-center gap-2 pt-1">
            <ScrollText className="size-3.5 text-muted" />
            <p className="text-xs text-muted font-bengali">
              কার্ডে ক্লিক করলে বিস্তারিত তথ্য দেখা যাবে
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
          <h2 className="font-bengali font-bold text-base text-ivory">
            ক্ষমতা সারসংক্ষেপ · Ability Summary
          </h2>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-forest-500/15 text-xs font-semibold uppercase tracking-wider text-muted font-cinzel">
                <th className="px-4 py-3 text-left">চরিত্র</th>
                <th className="px-4 py-3 text-left">বিশেষ ক্ষমতা</th>
                <th className="px-4 py-3 text-left">প্রভাব</th>
                <th className="px-4 py-3 text-left">প্রতিরোধ ক্ষমতা</th>
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
                    <span className="font-bengali text-parchment-200">{c.ability.nameBn}</span>
                    <span className="block text-[10px] text-muted">{c.ability.nameEn}</span>
                  </td>
                  <td className="px-4 py-3">
                    <span className="font-cinzel font-bold text-gold-300">{c.ability.effect}</span>
                  </td>
                  <td className="px-4 py-3">
                    <span className="font-bengali text-parchment-200">{c.block.nameBn}</span>
                    <span className="block text-[10px] text-muted">{c.block.nameEn}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {/* ── Disclaimer ──────────────────────────────────── */}
      <p className="text-center text-xs text-muted/60 font-bengali max-w-lg mx-auto">
        ⚠️ সমস্ত চরিত্র সম্পূর্ণ কাল্পনিক এবং শুধুমাত্র গেমিং বিনোদনের উদ্দেশ্যে তৈরি।
        বাস্তব কোনো রাজনীতিবিদ, দল বা ঘটনার সাথে এর কোনো সম্পর্ক নেই।
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
