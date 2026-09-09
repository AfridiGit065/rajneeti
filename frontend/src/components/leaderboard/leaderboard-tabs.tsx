"use client";

import { cn } from "@/lib/cn";

export type LeaderboardTab = "global" | "weekly" | "friends";

const TABS: { value: LeaderboardTab; label: string }[] = [
  { value: "global", label: "Global" },
  { value: "weekly", label: "Weekly" },
  { value: "friends", label: "Friends" },
];

export function LeaderboardTabs({
  value,
  onChange,
}: {
  value: LeaderboardTab;
  onChange: (tab: LeaderboardTab) => void;
}) {
  return (
    <div
      role="tablist"
      aria-label="লিডারবোর্ড ভিউ"
      className="flex flex-wrap sm:inline-flex rounded-xl border border-forest-500/25 bg-deep-900/80 p-1 gap-1"
    >
      {TABS.map((tab) => {
        const active = tab.value === value;
        return (
          <button
            key={tab.value}
            type="button"
            role="tab"
            aria-selected={active}
            onClick={() => onChange(tab.value)}
            className={cn(
              "rounded-lg px-4 py-2 text-sm font-semibold transition-colors",
              "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-400",
              active
                ? "bg-gold-500/15 text-gold-300 ring-1 ring-inset ring-gold-500/30"
                : "text-muted hover:bg-deep-700/50 hover:text-ivory",
            )}
          >
            {tab.label}
          </button>
        );
      })}
    </div>
  );
}