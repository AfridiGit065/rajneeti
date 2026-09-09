"use client";

import { cn } from "@/lib/cn";
import { Skull, ShieldAlert } from "@/components/ui/icons";
import type { ChallengeVerdict } from "@/lib/game/challenge";

interface ChallengeResultProps {
  verdict: ChallengeVerdict;
  challengerName: string;
  claimantName: string;
  className?: string;
}

const CONFIG = {
  true_claim: {
    chip: "চ্যালেঞ্জ ব্যর্থ",
    chipEn: "CHALLENGE FAILED",
    icon: Skull,
    headline: "দাবিটি সত্য ছিল!",
    loss: (name: string) => `চ্যালেঞ্জকারী ${name} ১ ইনফ্লুয়েন্স হারাল`,
    panel: "border-crimson-500/55 bg-crimson-600/12 text-crimson-200",
    glow: "from-crimson-500/30",
    iconColor: "text-crimson-300",
  },
  bluff: {
    chip: "ব্লাফ ধরা পড়ল!",
    chipEn: "BLUFF CAUGHT",
    icon: ShieldAlert,
    headline: "মিথ্যা দাবি উন্মোচিত!",
    loss: (name: string) => `দাবিকারী ${name} ১ ইনফ্লুয়েন্স হারাল`,
    panel: "border-forest-400/55 bg-forest-500/12 text-forest-200",
    glow: "from-forest-400/30",
    iconColor: "text-forest-300",
  },
} as const;

export function ChallengeResult({
  verdict,
  challengerName,
  claimantName,
  className,
}: ChallengeResultProps) {
  const cfg = CONFIG[verdict];
  const Icon = cfg.icon;

  return (
    <div
      className={cn(
        "fixed inset-0 z-[60] flex items-center justify-center bg-deep-950/90 px-4 backdrop-blur-md animate-fade-in",
        className,
      )}
      role="status"
      aria-label={cfg.chip}
    >
      <div
        className={cn(
          "relative w-full max-w-md overflow-hidden rounded-3xl border bg-deep-900/95 panel-emboss panel-texture",
          "px-6 py-8 text-center animate-zoom-in",
        )}
      >
        <div
          className={cn(
            "pointer-events-none absolute inset-x-0 -top-24 h-48 bg-gradient-to-b to-transparent blur-2xl animate-glow-pulse",
            cfg.glow,
          )}
          aria-hidden
        />
        <div className="relative">
          <div
            className={cn(
              "mx-auto flex size-16 items-center justify-center rounded-full border bg-deep-950/80",
              cfg.panel,
            )}
          >
            <Icon className={cn("size-8", cfg.iconColor)} aria-hidden />
          </div>

          <span
            className={cn(
              "mt-4 inline-flex items-center gap-2 rounded-full border px-4 py-1.5 text-sm font-bold",
              cfg.panel,
            )}
          >
            {cfg.chip}
          </span>

          <h3 className="mt-3 font-bengali text-2xl font-bold text-ivory sm:text-3xl">
            {cfg.headline}
          </h3>
          <p className="mt-1 font-cinzel text-[0.65rem] font-semibold uppercase tracking-[0.3em] text-muted">
            {cfg.chipEn}
          </p>

          <p className="mt-4 text-base font-medium text-parchment-300 sm:text-lg">
            {cfg.loss(verdict === "true_claim" ? challengerName : claimantName)}
          </p>
          <p className="mt-1 text-sm text-muted">
            উন্মোচন প্রস্তুত হচ্ছে…
          </p>
        </div>
      </div>
    </div>
  );
}