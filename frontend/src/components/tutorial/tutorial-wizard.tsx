"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { cn } from "@/lib/cn";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { TUTORIAL_STEPS } from "./tutorial-data";
import { useAuthStore } from "@/store/auth-store";
import { CheckCircle2, ArrowRight, ArrowLeft, X } from "@/components/ui/icons";

const TOTAL = TUTORIAL_STEPS.length;

export function TutorialWizard() {
  const router = useRouter();
  const user = useAuthStore((s) => s.user);
  const [step, setStep] = useState(0);
  const [finished, setFinished] = useState(false);
  const [enterIndex, setEnterIndex] = useState(0);

  const current = TUTORIAL_STEPS[step];
  const isLast = step === TOTAL - 1;
  const progress = finished ? 1 : (step + 1) / TOTAL;

  // Re-mount step body so it re-animates on navigation.
  const body = useMemo(() => current.body(), [current]);

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (finished) return;
      if (event.key === "ArrowRight") {
        event.preventDefault();
        if (isLast) setFinished(true);
        else setStep((s) => Math.min(s + 1, TOTAL - 1));
      } else if (event.key === "ArrowLeft") {
        event.preventDefault();
        setStep((s) => Math.max(s - 1, 0));
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [finished, isLast]);

  const go = (next: number) => {
    setEnterIndex((i) => i + 1);
    setStep(next);
  };

  if (finished) {
    return (
      <Card className="mx-auto w-full max-w-3xl animate-fade-up">
        <div className="flex flex-col items-center gap-6 py-10 text-center">
          <div className="flex size-16 items-center justify-center rounded-2xl border border-gold-500/30 bg-gold-500/10 text-gold-300">
            <CheckCircle2 className="size-8" aria-hidden />
          </div>
          <div>
            <h2 className="font-bengali text-2xl font-bold text-ivory">
              টিউটোরিয়াল শেষ!
            </h2>
            <p className="mx-auto mt-2 max-w-md text-sm text-muted">
              নিয়মগুলো হাতে-কলমে শেখো — খেলা আমন্ত্রণ জানাচ্ছে!{" "}
              <span className="text-parchment-300">
                (You now know the basics. Ready to play?)
              </span>
            </p>
          </div>
          <div className="flex flex-wrap items-center justify-center gap-3">
            <Button variant="outline" onClick={() => go(0)}>
              <ArrowRight className="size-4 rotate-180" aria-hidden />
              আবার দেখো
            </Button>
            <Button
              variant="premium"
              onClick={() => router.push(user ? "/lobby" : "/register")}
            >
              {user ? "লবিতে যাও" : "অ্যাকাউন্ট খুলো"}
              <ArrowRight className="size-4" aria-hidden />
            </Button>
          </div>
        </div>
      </Card>
    );
  }

  return (
    <div className="mx-auto w-full max-w-3xl">
      {/* Progress header */}
      <div className="mb-5">
        <div className="mb-2 flex items-center justify-between gap-4 text-xs">
          <p className="font-semibold uppercase tracking-[0.2em] text-gold-400">
            পাঠ {step + 1} / {TOTAL}
            <span className="ml-2 normal-case text-muted">How to Play</span>
          </p>
          <p className="text-muted">{Math.round(progress * 100)}%</p>
        </div>
        <div className="h-1.5 w-full overflow-hidden rounded-full bg-deep-750">
          <div
            className="h-full rounded-full bg-gradient-to-r from-gold-600 to-gold-400 transition-all duration-300"
            style={{ width: `${progress * 100}%` }}
          />
        </div>
        <div className="mt-3 flex items-center justify-center gap-1.5">
          {TUTORIAL_STEPS.map((s, i) => (
            <button
              key={s.key}
              type="button"
              aria-label={`ধাপ ${i + 1}: ${s.labelBn}`}
              title={`${s.labelBn} · ${s.labelEn}`}
              onClick={() => go(i)}
              className={cn(
                "h-2 rounded-full transition-all duration-200",
                i === step
                  ? "w-6 bg-gold-400"
                  : "w-2 cursor-pointer bg-deep-650 hover:bg-forest-400/60",
              )}
            />
          ))}
        </div>
      </div>

      {/* Step card */}
      <Card className="animate-fade-up" key={`${step}-${enterIndex}`}>
        <div className="mb-5 flex items-start gap-4">
          <span className="flex size-12 shrink-0 items-center justify-center rounded-xl border border-gold-500/30 bg-gold-500/10 text-gold-400">
            <current.icon className="size-6" aria-hidden />
          </span>
          <div className="min-w-0">
            <p className="font-bengali text-xs text-gold-500">{current.indexBn}</p>
            <h2 className="font-bengali text-2xl font-bold text-ivory">
              {current.labelBn}
            </h2>
            <p className="text-xs uppercase tracking-[0.22em] text-muted">
              {current.labelEn}
            </p>
            <p className="mt-1 text-sm text-parchment-300">{current.summary}</p>
          </div>
        </div>
        <div className="border-t border-forest-500/15 pt-5">{body}</div>
      </Card>

      {/* Footer controls */}
      <div className="mt-6 flex flex-wrap items-center justify-between gap-3">
        <Button variant="ghost" size="md" onClick={() => setFinished(true)}>
          <X className="size-4" aria-hidden />
          এড়িয়ে যাও
          <span className="text-xs text-muted">Skip</span>
        </Button>
        <div className="flex items-center gap-2.5">
          <Button
            variant="secondary"
            size="md"
            disabled={step === 0}
            onClick={() => go(step - 1)}
          >
            <ArrowLeft className="size-4" aria-hidden />
            আগের
            <span className="text-xs text-muted">Prev</span>
          </Button>
          {isLast ? (
            <Button variant="premium" size="md" onClick={() => setFinished(true)}>
              শেষ করো
              <span className="text-xs text-gold-700/80">Finish</span>
            </Button>
          ) : (
            <Button variant="primary" size="md" onClick={() => go(step + 1)}>
              পরের
              <span className="text-xs text-deep-700/70">Next</span>
              <ArrowRight className="size-4" aria-hidden />
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}