import { TutorialWizard } from "@/components/tutorial/tutorial-wizard";

export default function HowToPlayPage() {
  return (
    <div className="mx-auto w-full max-w-4xl px-4 py-10 sm:px-6">
      <div className="mb-8 text-center">
        <p className="text-xs font-semibold uppercase tracking-[0.25em] text-gold-400">
          নিয়মাবলী · Rules
        </p>
        <h1 className="mt-2 font-bengali text-3xl font-bold text-ivory sm:text-4xl">
          কিভাবে খেলবেন
        </h1>
        <p className="mx-auto mt-3 max-w-xl text-sm text-muted sm:text-base">
          রজনীতি — কৌশল, ব্লাফ আর বিশ্বাসের খেলা। ধাপে ধাপে শিখে নাও।{" "}
          <span className="text-parchment-300">
            (A game of strategy, bluff and trust — learn it step by step.)
          </span>
        </p>
      </div>
      <TutorialWizard />
    </div>
  );
}