"use client";

import { useRouter } from "next/navigation";
import type { CSSProperties } from "react";
import { cn } from "@/lib/cn";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Crown,
  History,
  Home,
  Medal,
  Play,
  Sparkles,
  Trophy,
} from "@/components/ui/icons";
import { CoinDisplay } from "./coin-display";
import { InfluenceCard } from "./influence-card";
import type { GamePlayer, GameState } from "@/types/game";

interface RankingEntry {
  player: GamePlayer;
  rank: number;
}

const BN_DIGITS = ["০", "১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯"];

function bn(value: number): string {
  return String(value)
    .split("")
    .map((char) => BN_DIGITS[Number(char)] ?? char)
    .join("");
}

/**
 * Deterministic final standings: survivors first, then by coins, then seat.
 * Mirrors elimination order for the finished mock without revealing cards.
 */
function buildStandings(game: GameState): RankingEntry[] {
  return [...game.players]
    .sort((a, b) => {
      if (a.isAlive !== b.isAlive) return a.isAlive ? -1 : 1;
      if (b.coins !== a.coins) return b.coins - a.coins;
      return a.seatIndex - b.seatIndex;
    })
    .map((player, index) => ({ player, rank: index + 1 }));
}

const PARTICLES = Array.from({ length: 14 }, (_, index) => ({
  left: ((index * 61 + 17) % 100) / 100,
  delay: (index % 7) * 1.4,
  duration: 9 + (index % 5) * 1.3,
  size: index % 4 === 0 ? 5 : 3,
  tone: index % 4 === 0 ? "bg-parchment-200/80" : "bg-gold-400/70",
}));

const RANK_TONE: Record<number, string> = {
  1: "border-gold-500/50 bg-gold-500/10 text-gold-200",
  2: "border-forest-500/40 bg-forest-500/10 text-forest-200",
  3: "border-parchment-500/35 bg-parchment-500/10 text-parchment-200",
};

function PlayerChip({
  player,
  highlighted = false,
  showYou = false,
}: {
  player: GamePlayer;
  highlighted?: boolean;
  showYou?: boolean;
}) {
  return (
    <span className="flex min-w-0 items-center gap-2.5">
      <span
        className={cn(
          "flex size-8 shrink-0 items-center justify-center rounded-full text-sm font-bold text-deep-950",
          "bg-gradient-to-b from-forest-500 to-forest-600",
          highlighted && "from-gold-400 via-gold-500 to-gold-600 ring-2 ring-gold-300/70",
        )}
        aria-hidden
      >
        {player.displayName?.[0] ?? player.username[0]}
      </span>
      <span className="flex min-w-0 flex-col leading-tight">
        <span className="truncate font-bold text-ivory">
          {player.displayName ?? player.username}
        </span>
        {showYou ? <Badge tone="gold" className="mt-0.5 w-fit">আপনি</Badge> : null}
      </span>
    </span>
  );
}

export function GameOverScreen({
  game,
  selfId,
}: {
  game: GameState;
  selfId?: string;
}) {
  const router = useRouter();

  const winner =
    game.players.find((player) => player.id === game.winnerPlayerId) ??
    game.players.find((player) => player.isAlive) ??
    game.players[0];

  const standings = buildStandings(game);

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-app">
      <div className="relative flex min-h-dvh flex-col items-center justify-center px-4 py-12">
        {/* Cinematic backdrop */}
        <div className="pointer-events-none absolute inset-0 animate-victory-flare" aria-hidden>
          <span className="absolute left-1/2 top-[-18%] h-[46vh] w-[80vw] -translate-x-1/2 rounded-full bg-gold-500/20 blur-3xl" />
          <span className="absolute bottom-[-22%] left-[-14%] h-[44vh] w-[44vw] rounded-full bg-forest-500/14 blur-3xl" />
          <span className="absolute right-[-12%] top-[6%] h-[40vh] w-[36vw] rounded-full bg-crimson-500/12 blur-3xl" />
        </div>

        {/* Sparse celebratory particles (premium, not confetti) */}
        <div className="pointer-events-none absolute inset-0 overflow-hidden" aria-hidden>
          {PARTICLES.map((particle, index) => (
            <span
              key={index}
              className={cn(
                "animate-particle-rise absolute -bottom-4 rounded-full",
                particle.tone,
              )}
              style={
                {
                  left: `${particle.left * 100}%`,
                  width: particle.size,
                  height: particle.size,
                  animationDelay: `${particle.delay}s`,
                  animationDuration: `${particle.duration}s`,
                } as CSSProperties
              }
            />
          ))}
        </div>

        <div className="relative flex w-full max-w-md flex-col items-center text-center animate-fade-up">
          {/* Trophy hero */}
          <div className="relative mb-5">
            <span
              className="animate-ring-orbit pointer-events-none absolute -inset-5 rounded-full border border-dashed border-gold-500/35"
              aria-hidden
            />
            <div className="animate-trophy-float relative flex size-24 items-center justify-center rounded-full border border-gold-500/45 bg-deep-900/90 shadow-gold">
              <Trophy className="size-11 text-gold-300" aria-hidden />
              <span
                className={cn(
                  "absolute -right-1.5 -top-1.5 flex size-8 items-center justify-center rounded-full",
                  "border border-gold-400/60 bg-deep-950 shadow-gold animate-glow-pulse",
                )}
              >
                <Crown className="size-4 text-gold-200" aria-hidden />
              </span>
            </div>
          </div>

          {/* Title */}
          <p className="mb-2 flex items-center justify-center gap-1.5 text-xs font-semibold uppercase tracking-[0.35em] text-gold-400">
            <Sparkles className="size-3.5" aria-hidden />
            বিজয়ের মুহূর্ত
            <Sparkles className="size-3.5" aria-hidden />
          </p>
          <h1 className="text-gold-shimmer font-bengali text-4xl font-black tracking-wide sm:text-5xl">
            খেলা শেষ
          </h1>

          {/* Winner reveal */}
          {winner ? (
            <div className="animate-podium-in mt-6 w-full rounded-2xl border border-gold-500/35 bg-deep-900/85 p-6 panel-emboss shadow-gold">
              <div className="flex flex-col items-center gap-3">
                <span
                  className={cn(
                    "flex size-16 items-center justify-center rounded-full text-2xl font-black text-deep-950",
                    "bg-gradient-to-b from-gold-400 via-gold-500 to-gold-600 ring-4 ring-gold-300/30",
                  )}
                  aria-hidden
                >
                  {winner.displayName?.[0] ?? winner.username[0]}
                </span>
                <div>
                  <p className="text-[0.68rem] font-semibold uppercase tracking-[0.3em] text-gold-400">
                    বিজয়ী
                  </p>
                  <h2 className="mt-0.5 font-cinzel text-2xl font-black uppercase tracking-[0.16em] text-ivory">
                    {winner.displayName ?? winner.username}
                  </h2>
                  <p className="mt-1.5 text-sm text-muted">
                    শেষ পর্যন্ত ক্ষমতা ধরে রেখেছে।
                  </p>
                </div>
              </div>

              <div className="mt-5 grid grid-cols-3 gap-2.5">
                <div className="rounded-xl border border-forest-500/25 bg-deep-800/70 px-2 py-3">
                  <p className="text-[0.65rem] font-semibold uppercase tracking-wider text-muted">
                    ইনফ্লুয়েন্স
                  </p>
                  <div className="mt-1.5 flex items-center justify-center gap-1.5">
                    {winner.influenceCards.length > 0 ? (
                      winner.influenceCards.map((card) => (
                        <InfluenceCard
                          key={card.id}
                          characterId={card.characterId}
                          state="revealed"
                          size="xs"
                          className="!w-9 sm:!w-10"
                        />
                      ))
                    ) : (
                      <span className="font-mono text-lg font-bold text-ivory">০</span>
                    )}
                  </div>
                </div>

                <div className="rounded-xl border border-gold-500/25 bg-deep-800/70 px-2 py-3">
                  <p className="text-[0.65rem] font-semibold uppercase tracking-wider text-muted">
                    কয়েন
                  </p>
                  <CoinDisplay coins={winner.coins} size="lg" className="mt-1" />
                </div>

                <div className="rounded-xl border border-parchment-500/25 bg-deep-800/70 px-2 py-3">
                  <p className="text-[0.65rem] font-semibold uppercase tracking-wider text-muted">
                    অবস্থান
                  </p>
                  <span className="mt-1 flex items-center justify-center gap-1 font-mono text-lg font-bold text-ivory">
                    <Medal className="size-4 text-gold-300" aria-hidden />
                    {bn(1)}
                  </span>
                </div>
              </div>
            </div>
          ) : null}

          {/* Final standings */}
          <div className="animate-podium-in mt-5 w-full rounded-2xl border border-forest-500/25 bg-surface p-4 panel-emboss [animation-delay:180ms]">
            <p className="text-xs font-semibold uppercase tracking-[0.25em] text-muted">
              চূড়ান্ত অবস্থান
            </p>
            <ol className="mt-3 space-y-2">
              {standings.map(({ player, rank }, index) => (
                <li
                  key={player.id}
                  className={cn(
                    "flex items-center justify-between gap-3 rounded-xl border px-3 py-2.5 animate-fade-up",
                    rank === 1
                      ? "border-gold-500/40 bg-gold-500/8"
                      : "border-forest-500/15 bg-deep-900/50",
                  )}
                  style={{ animationDelay: `${220 + index * 90}ms` } as CSSProperties}
                >
                  <span className="flex min-w-0 items-center gap-3">
                    <span
                      className={cn(
                        "flex size-7 shrink-0 items-center justify-center rounded-full border font-mono text-sm font-bold",
                        rank === 1 ? RANK_TONE[1] : rank === 2 ? RANK_TONE[2] : rank === 3 ? RANK_TONE[3] : "border-deep-650 bg-deep-800/70 text-muted",
                      )}
                      aria-hidden
                    >
                      {rank === 1 ? (
                        <Crown className="size-3.5" aria-hidden />
                      ) : (
                        bn(rank)
                      )}
                    </span>
                    <PlayerChip
                      player={player}
                      highlighted={rank === 1}
                      showYou={selfId != null && player.userId === selfId}
                    />
                  </span>

                  <span className="flex shrink-0 items-center gap-2.5">
                    <span className="flex items-center gap-1 text-sm font-semibold tabular-nums text-gold-300">
                      <CoinDisplay coins={player.coins} size="sm" />
                    </span>
                    <span className="flex items-center gap-1 text-xs text-muted">
                      <span className="flex items-center gap-0.5">{bn(player.influenceCards.length)} কার্ড</span>
                    </span>
                  </span>
                </li>
              ))}
            </ol>
          </div>

          {/* Actions */}
          <div className="mt-6 flex flex-wrap items-center justify-center gap-3">
            <Button variant="premium" size="lg" onClick={() => router.push("/rooms/create")}>
              <Play className="size-4" aria-hidden />
              আবার খেলুন
            </Button>
            <Button variant="outline" size="lg" onClick={() => router.push("/lobby")}>
              <Home className="size-4" aria-hidden />
              লবিতে ফিরে যান
            </Button>
            <Button variant="ghost" size="lg" onClick={() => router.push("/history")}>
              <History className="size-4" aria-hidden />
              ম্যাচ ইতিহাস
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}