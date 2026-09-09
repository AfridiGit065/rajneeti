import Link from "next/link";
import { CHARACTERS } from "@/lib/game/characters";
import { RULES } from "@/lib/game/rules";
import { CharacterCard } from "@/components/game/character-card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { ArrowRight, Crown, Swords, ScrollText, Users, Landmark } from "@/components/ui/icons";

const HERO_FEATURES = [
  { icon: <Users className="size-4" aria-hidden />, text: `${RULES.minPlayers}–${RULES.maxPlayers} খেলোয়াড়` },
  { icon: <Swords className="size-4" aria-hidden />, text: `${RULES.gameLengthMinutes[0]}–${RULES.gameLengthMinutes[1]} মিনিট` },
  { icon: <ScrollText className="size-4" aria-hidden />, text: "১৫ কার্ডের ডেক" },
];

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-app">
      {/* ── Hero ─────────────────────────────────────────── */}
      <section className="relative overflow-hidden">
        <div
          className="pointer-events-none absolute inset-0"
          aria-hidden
          style={{
            background:
              "radial-gradient(ellipse 75% 60% at 50% -12%, rgb(46 110 82 / 0.35), transparent 60%)," +
              "radial-gradient(ellipse 50% 40% at 85% 20%, rgb(122 31 43 / 0.16), transparent 60%)",
          }}
        />
        <div className="relative mx-auto flex max-w-7xl flex-col items-center px-4 py-20 text-center sm:px-6 sm:py-28">
          <Badge tone="gold" className="mb-6 animate-fade-up">
            <Crown className="size-3.5" aria-hidden />
            ক্ষমতার খেলা · Bluff Strategy Game
          </Badge>

          <h1
            className="font-bengali text-6xl font-bold leading-none text-ivory sm:text-8xl animate-fade-up"
            style={{ animationDelay: "80ms" }}
          >
            রাজনীতি
          </h1>
          <p className="mt-4 text-xl font-semibold uppercase tracking-[0.5em] text-gold-gradient sm:text-2xl animate-fade-up" style={{ animationDelay: "140ms" }}>
            RAJNEETI
          </p>
          <p className="mt-2 text-sm tracking-[0.2em] text-muted uppercase animate-fade-up" style={{ animationDelay: "200ms" }}>
            The Game of Power
          </p>

          <p className="mt-8 max-w-xl font-bengali text-lg leading-relaxed text-parchment-300 animate-fade-up" style={{ animationDelay: "260ms" }}>
            ক্ষমতার খেলায় সত্য নয়, বুদ্ধিই শেষ কথা।
          </p>

          <div className="mt-10 flex flex-wrap items-center justify-center gap-4 animate-fade-up" style={{ animationDelay: "320ms" }}>
            <Link href="/register">
              <Button variant="premium" size="xl">
                খেলা শুরু করো
                <ArrowRight className="size-5" aria-hidden />
              </Button>
            </Link>
            <Link href="/how-to-play">
              <Button variant="outline" size="xl">
                নিয়ম জানো
              </Button>
            </Link>
          </div>

          <div className="mt-12 flex flex-wrap items-center justify-center gap-3 animate-fade-up" style={{ animationDelay: "380ms" }}>
            {HERO_FEATURES.map((f) => (
              <span
                key={f.text}
                className="inline-flex items-center gap-2 rounded-full border border-forest-500/25 bg-deep-800/60 px-4 py-1.5 text-sm text-muted"
              >
                <span className="text-gold-400">{f.icon}</span>
                {f.text}
              </span>
            ))}
          </div>
        </div>
      </section>

      <div className="mx-auto max-w-5xl px-4 sm:px-6">
        <div className="divider-gold" />
      </div>

      {/* ── Character showcase ───────────────────────────── */}
      <section className="mx-auto max-w-7xl px-4 py-16 sm:px-6 sm:py-20">
        <div className="mb-10 text-center">
          <p className="mb-1 text-xs font-semibold uppercase tracking-[0.25em] text-gold-400">ছদ্মবেশ · The Deck</p>
          <h2 className="font-bengali text-3xl font-bold text-ivory sm:text-4xl">পাঁচ চরিত্র, একটি খেলা</h2>
          <p className="mx-auto mt-3 max-w-2xl text-sm text-muted sm:text-base">
            প্রত্যেকে গোপনে ২টি ইনফ্লুয়েন্স কার্ড বাছাই করে। মিথ্যা বলো, ব্লক করো,
            চ্যালেঞ্জ করো — শেষ কথা বলবে বুদ্ধি।
          </p>
        </div>

        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {CHARACTERS.slice(0, 3).map((character, index) => (
            <div key={character.id} className="animate-fade-up" style={{ animationDelay: `${index * 90}ms` }}>
              <CharacterCard character={character} />
            </div>
          ))}
          <div className="flex flex-col items-center justify-center gap-3 rounded-2xl border border-dashed border-gold-500/30 bg-deep-900/40 p-6 text-center sm:col-span-2 lg:col-span-1">
            <Landmark className="size-8 text-gold-400" aria-hidden />
            <p className="font-bengali text-lg font-medium text-ivory">বাকি দুজন কে?</p>
            <p className="text-sm text-muted">ঘাতক আর গোয়েন্দা — এই দুই প্রোফাইলও ডেকের ভেতরে লুকিয়ে আছে।</p>
            <Link href="/characters" className="mt-2">
              <Button variant="outline" size="sm">
                সব চরিত্র দেখো
                <ArrowRight className="size-4" aria-hidden />
              </Button>
            </Link>
          </div>
        </div>
      </section>

      {/* ── How it works ─────────────────────────────────── */}
      <section className="mx-auto max-w-7xl px-4 pb-20 sm:px-6">
        <div className="rounded-3xl border border-forest-500/25 bg-surface panel-emboss panel-texture p-8 sm:p-12">
          <div className="mb-8 text-center">
            <p className="mb-1 text-xs font-semibold uppercase tracking-[0.25em] text-gold-400">মুখস্থ নিয়ম</p>
            <h2 className="font-bengali text-3xl font-bold text-ivory">তিন ধাপের ক্ষমতার খেলা</h2>
          </div>
          <div className="grid gap-8 md:grid-cols-3">
            {[
              {
                step: "০১",
                title: "দাবি করো",
                text: "আয়, বিদেশি অনুদান, কর আদায় — যেকোনো একটা অ্যাকশন দাবি করো। চাইলে মন্ত্রী বা দালাল হওয়ার ভান করো।",
              },
              {
                step: "০২",
                title: "ব্লক আর চ্যালেঞ্জ",
                text: "অন্যদের চরিত্র অনুমান করে ব্লক করো। নিশ্চিত হলে চ্যালেঞ্জ দাও — মিথ্যা প্রমাণিত হলে দাম দিতে হবে।",
              },
              {
                step: "০৩",
                title: "ক্ষমতা দখল",
                text: "৭ কয়েন জমা করে কোপ দাও, প্রতিপক্ষের ইনফ্লুয়েন্স শেষ করো — শেষ জীবিত খেলোয়াড়ই বিজয়ী।",
              },
            ].map((item) => (
              <div key={item.step} className="relative rounded-2xl border border-forest-500/20 bg-deep-900/50 p-6">
                <span className="font-bengali text-4xl font-bold text-gold-500/30">{item.step}</span>
                <h3 className="mt-3 font-bengali text-xl font-semibold text-ivory">{item.title}</h3>
                <p className="mt-2 text-sm leading-relaxed text-muted">{item.text}</p>
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}