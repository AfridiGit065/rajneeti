import Link from "next/link";
import { CHARACTERS } from "@/lib/game/characters";
import { RULES } from "@/lib/game/rules";
import { CharacterCard } from "@/components/game/character-card";
import { TopNav } from "@/components/layout/top-nav";
import { Footer } from "@/components/layout/footer";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { ArrowRight, Crown, Swords, ScrollText, Users, Landmark, BookOpen, ShieldAlert, Sparkles, EyeOff } from "@/components/ui/icons";

const HERO_FEATURES = [
  { icon: <Users className="size-4" aria-hidden />, text: `${RULES.minPlayers}–${RULES.maxPlayers} খেলোয়াড়` },
  { icon: <Swords className="size-4" aria-hidden />, text: `${RULES.gameLengthMinutes[0]}–${RULES.gameLengthMinutes[1]} মিনিট` },
  { icon: <ScrollText className="size-4" aria-hidden />, text: "১৫ কার্ডের ডেক" },
];

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-app flex flex-col selection:bg-gold-500/30 selection:text-gold-200">
      {/* ── Fixed Navigation Bar ─────────────────────────── */}
      <TopNav mode="public" />

      {/* ── Hero Section ─────────────────────────────────── */}
      <section className="relative overflow-hidden pt-12 pb-20 sm:pt-16 sm:pb-28">
        {/* Subtle Bangladesh-inspired abstract texture & ambient emerald-crimson lighting */}
        <div
          className="pointer-events-none absolute inset-0"
          aria-hidden
          style={{
            backgroundImage:
              "radial-gradient(ellipse 75% 55% at 50% -10%, rgb(44 110 82 / 0.38), transparent 65%)," +
              "radial-gradient(ellipse 50% 40% at 85% 25%, rgb(147 41 58 / 0.18), transparent 60%)," +
              "radial-gradient(ellipse 45% 35% at 15% 75%, rgb(201 165 60 / 0.12), transparent 55%)," +
              "url(\"data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='%23c9a53c' fill-opacity='0.035' fill-rule='evenodd'%3E%3Cpath d='M30 0l30 30-30 30L0 30 30 0zm0 10.606L10.606 30 30 49.394 49.394 30 30 10.606z'/%3E%3Ccircle cx='30' cy='30' r='3'/%3E%3C/g%3E%3C/svg%3E\")",
          }}
        />

        <div className="relative mx-auto flex max-w-7xl flex-col items-center px-4 text-center sm:px-6">
          {/* Top Badge */}
          <Badge tone="gold" className="mb-6 animate-fade-up">
            <Crown className="size-3.5" aria-hidden />
            ক্ষমতার খেলা · Bluff Strategy Game
          </Badge>

          {/* Hero Titles: Bengali & English */}
          <div className="space-y-2">
            <h1
              className="font-bengali text-6xl font-bold leading-none text-ivory sm:text-8xl md:text-9xl animate-fade-up drop-shadow-[0_10px_35px_rgba(0,0,0,0.8)]"
              style={{ animationDelay: "80ms" }}
            >
              রাজনীতি
            </h1>
            <p
              className="mt-4 text-2xl font-bold uppercase tracking-[0.45em] text-gold-gradient sm:text-3xl md:text-4xl animate-fade-up"
              style={{ animationDelay: "140ms" }}
            >
              RAJNEETI
            </p>
            <p
              className="mt-2 text-sm tracking-[0.3em] text-muted uppercase animate-fade-up font-semibold"
              style={{ animationDelay: "200ms" }}
            >
              The Game of Power
            </p>
          </div>

          {/* Required Tagline */}
          <div
            className="mt-8 max-w-2xl rounded-2xl border border-gold-500/30 bg-deep-900/70 p-4 sm:p-5 backdrop-blur-md animate-fade-up shadow-gold"
            style={{ animationDelay: "260ms" }}
          >
            <p className="font-bengali text-xl font-medium leading-relaxed text-parchment-200 sm:text-2xl">
              &ldquo;ক্ষমতার খেলায় সত্য নয়, বুদ্ধিই শেষ কথা।&rdquo;
            </p>
            <p className="mt-1 text-xs text-muted">
              In the game of power, truth is not final—intellect decides the realm.
            </p>
          </div>

          {/* Primary & Secondary CTAs */}
          <div
            className="mt-10 flex flex-wrap items-center justify-center gap-4 animate-fade-up"
            style={{ animationDelay: "320ms" }}
          >
            {/* Primary CTA: খেলা শুরু করুন → /login */}
            <Link href="/login">
              <Button variant="premium" size="xl" className="font-bengali text-lg px-8 py-3.5 shadow-gold">
                খেলা শুরু করুন
                <ArrowRight className="size-5" aria-hidden />
              </Button>
            </Link>

            {/* Secondary CTA: কীভাবে খেলবেন → /how-to-play */}
            <Link href="/how-to-play">
              <Button variant="outline" size="xl" className="font-bengali text-lg px-7 py-3.5">
                <BookOpen className="size-5 text-gold-400" aria-hidden />
                কীভাবে খেলবেন
              </Button>
            </Link>
          </div>

          {/* Quick Metrics Bar */}
          <div
            className="mt-12 flex flex-wrap items-center justify-center gap-3 animate-fade-up"
            style={{ animationDelay: "380ms" }}
          >
            {HERO_FEATURES.map((f) => (
              <span
                key={f.text}
                className="inline-flex items-center gap-2 rounded-full border border-forest-500/30 bg-deep-800/80 px-4 py-1.5 text-sm text-muted shadow-sm"
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

      {/* ── Character Showcase ────────────────────────────── */}
      <section className="mx-auto max-w-7xl px-4 py-16 sm:px-6 sm:py-20">
        <div className="mb-12 text-center">
          <p className="mb-1 text-xs font-semibold uppercase tracking-[0.25em] text-gold-400">
            ছদ্মবেশ · The Deck
          </p>
          <h2 className="font-bengali text-3xl font-bold text-ivory sm:text-4xl">
            পাঁচ কাল্পনিক চরিত্র, একটি খেলা
          </h2>
          <p className="mx-auto mt-3 max-w-2xl text-sm text-muted sm:text-base">
            কোনো বাস্তব রাজনৈতিক চরিত্র নয়—বিশুদ্ধ মনস্তাত্ত্বিক কৌশলের উপর ভিত্তি করে তৈরি।
            প্রত্যেক খেলোয়াড় গোপনে ২টি কার্ড নিয়ে চাল দেয়। মিথ্যা বলো, ব্লক করো,
            চ্যালেঞ্জ করো — শেষ কথা বলবে বুদ্ধি।
          </p>
        </div>

        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {CHARACTERS.slice(0, 3).map((character, index) => (
            <div
              key={character.id}
              className="animate-fade-up transition-transform duration-300 hover:-translate-y-1.5"
              style={{ animationDelay: `${index * 90}ms` }}
            >
              <CharacterCard character={character} />
            </div>
          ))}

          <div className="flex flex-col items-center justify-center gap-4 rounded-2xl border border-dashed border-gold-500/35 bg-deep-900/50 p-8 text-center sm:col-span-2 lg:col-span-1 shadow-panel">
            <Landmark className="size-10 text-gold-400 animate-glow-pulse" aria-hidden />
            <div className="space-y-1">
              <p className="font-bengali text-xl font-bold text-ivory">বাকি চরিত্রসমূহ</p>
              <p className="text-sm text-muted">
                আমলা ও গোয়েন্দা — পুরো ডেকের সব কার্ড ও ক্ষমতা এক্সপ্লোর করুন।
              </p>
            </div>
            <Link href="/characters" className="mt-2">
              <Button variant="outline" size="md">
                সব চরিত্র দেখুন
                <ArrowRight className="size-4" aria-hidden />
              </Button>
            </Link>
          </div>
        </div>
      </section>

      {/* ── How It Works & Core Rules ──────────────────────── */}
      <section className="mx-auto max-w-7xl px-4 pb-20 sm:px-6">
        <div className="rounded-3xl border border-forest-500/25 bg-surface panel-emboss panel-texture p-8 sm:p-12">
          <div className="mb-10 text-center">
            <p className="mb-1 text-xs font-semibold uppercase tracking-[0.25em] text-gold-400">
              খেলার ধাপসমূহ
            </p>
            <h2 className="font-bengali text-3xl font-bold text-ivory sm:text-4xl">
              তিন ধাপে ক্ষমতার লড়াই
            </h2>
            <p className="mt-2 text-sm text-muted">
              সরাসরি ব্ল্যাফ করুন অথবা কৌশলী চালে প্রতিপক্ষকে ফাঁদে ফেলুন।
            </p>
          </div>

          <div className="grid gap-8 md:grid-cols-3">
            {[
              {
                step: "০১",
                icon: <Sparkles className="size-5 text-gold-400" />,
                title: "দাবি করুন",
                text: "আয়, ট্যাক্স আদায় বা বিদেশি অনুদান—যেকোনো ক্ষমতা দাবি করে চাল দিন। হাতে কার্ড না থাকলেও নিখুঁত ব্ল্যাফ করুন।",
              },
              {
                step: "০২",
                icon: <ShieldAlert className="size-5 text-crimson-400" />,
                title: "ব্লক ও চ্যালেঞ্জ",
                text: "প্রতিপক্ষের ভান ধরতে পারলে প্রকাশ্যে চ্যালেঞ্জ দিন। মিথ্যা প্রমাণ হলে তার কার্ড কাটা যাবে, ভুল সন্দেহ করলে আপনার ক্ষতি।",
              },
              {
                step: "০৩",
                icon: <EyeOff className="size-5 text-forest-300" />,
                title: "চূড়ান্ত আধিপত্য",
                text: "৭ কয়েন জমা করে আঘাত হানুন। সব প্রতিপক্ষের ইনফ্লুয়েন্স ধ্বংস করে টেবিলে শেষ জীবিত খেলোয়াড় হিসেবে বিজয়ী হোন।",
              },
            ].map((item) => (
              <div
                key={item.step}
                className="relative rounded-2xl border border-forest-500/20 bg-deep-900/60 p-6 transition-all hover:border-gold-500/40"
              >
                <div className="flex items-center justify-between mb-3">
                  <span className="font-bengali text-3xl font-bold text-gold-500/40">
                    {item.step}
                  </span>
                  <div className="p-2 rounded-lg bg-deep-800/80 border border-white/5">
                    {item.icon}
                  </div>
                </div>
                <h3 className="font-bengali text-xl font-semibold text-ivory">
                  {item.title}
                </h3>
                <p className="mt-2 text-sm leading-relaxed text-muted">
                  {item.text}
                </p>
              </div>
            ))}
          </div>

          <div className="mt-10 text-center">
            <Link href="/how-to-play">
              <Button variant="outline" size="md">
                সম্পূর্ণ গেম নির্দেশিকা পড়ুন
                <ArrowRight className="size-4" aria-hidden />
              </Button>
            </Link>
          </div>
        </div>
      </section>

      {/* ── Footer ───────────────────────────────────────── */}
      <Footer />
    </div>
  );
}