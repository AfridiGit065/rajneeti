import { TutorialWizard } from "@/components/tutorial/tutorial-wizard";

export default function HowToPlayPage() {
  return (
    <div className="mx-auto w-full max-w-4xl px-4 py-10 sm:px-6">
      <div className="mb-8 text-center">
        <p className="text-xs font-semibold uppercase tracking-[0.25em] text-gold-400 font-cinzel">
          Rules &amp; Tutorial
        </p>
        <h1 className="mt-2 font-display text-3xl font-bold text-ivory sm:text-4xl">
          How to Play
        </h1>
        <p className="mx-auto mt-3 max-w-xl text-sm text-muted sm:text-base">
          RAJNEETI — A game of strategy, bluffing, and trust. Master the court mechanics step by step.
        </p>
      </div>
      <TutorialWizard />
    </div>
  );
}