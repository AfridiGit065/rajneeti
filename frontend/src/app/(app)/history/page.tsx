"use client";

import { useCallback, useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/ui/empty-state";
import { ErrorState } from "@/components/ui/error-state";
import { LoadingState } from "@/components/ui/loading-state";
import { FileQuestion } from "@/components/ui/icons";
import {
  MatchDetailsModal,
  MatchHistoryCard,
} from "@/components/history";
import { MOCK_CURRENT_USER } from "@/mocks/users";
import { MetaService } from "@/services/meta-service";
import { cn } from "@/lib/cn";
import type { MatchHistoryEntry } from "@/types/user";

const FILTERS = [
  { value: "all", label: "সব" },
  { value: "win", label: "জয়" },
  { value: "loss", label: "পরাজয়" },
] as const;

type HistoryFilter = (typeof FILTERS)[number]["value"];

function FilterPills({
  value,
  counts,
  onChange,
}: {
  value: HistoryFilter;
  counts: Record<"all" | "win" | "loss", number>;
  onChange: (filter: HistoryFilter) => void;
}) {
  return (
    <div
      role="group"
      aria-label="ফলাফল অনুযায়ী ফিল্টার"
      className="inline-flex rounded-xl border border-forest-500/25 bg-deep-900/80 p-1"
    >
      {FILTERS.map((filter) => {
        const active = filter.value === value;
        return (
          <button
            key={filter.value}
            type="button"
            onClick={() => onChange(filter.value)}
            aria-pressed={active}
            className={cn(
              "flex items-center gap-1.5 rounded-lg px-3.5 py-2 text-sm font-semibold transition-colors",
              "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-400",
              active
                ? "bg-gold-500/15 text-gold-300 ring-1 ring-inset ring-gold-500/30"
                : "text-muted hover:bg-deep-700/50 hover:text-ivory",
            )}
          >
            {filter.label}
            <span
              className={cn(
                "rounded-full px-1.5 py-0.5 font-mono text-[0.65rem] leading-none",
                active ? "bg-gold-500/15 text-gold-300" : "bg-deep-700/60 text-muted",
              )}
            >
              {counts[filter.value]}
            </span>
          </button>
        );
      })}
    </div>
  );
}

export default function HistoryPage() {
  const [matches, setMatches] = useState<MatchHistoryEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<HistoryFilter>("all");
  const [selected, setSelected] = useState<MatchHistoryEntry | null>(null);

  const fetchHistory = useCallback(
    () => MetaService.getMatchHistory(MOCK_CURRENT_USER.id),
    [],
  );

  useEffect(() => {
    let cancelled = false;
    fetchHistory().then((result) => {
      if (cancelled) return;
      if (result.ok) {
        setMatches(result.data);
      } else {
        setError(result.error.message);
      }
      setLoading(false);
    });
    return () => {
      cancelled = true;
    };
  }, [fetchHistory]);

  async function retry() {
    setLoading(true);
    setError(null);
    const result = await fetchHistory();
    if (result.ok) {
      setMatches(result.data);
    } else {
      setError(result.error.message);
    }
    setLoading(false);
  }

  const counts = {
    all: matches.length,
    win: matches.filter((m) => m.result === "win").length,
    loss: matches.filter((m) => m.result === "loss").length,
  };
  const shown = matches.filter((m) => filter === "all" || m.result === filter);

  return (
    <div className="space-y-6">
      <header>
        <p className="text-xs font-semibold uppercase tracking-[0.25em] text-gold-400">
          পূর্বের খেলা
        </p>
        <h1 className="mt-1 font-bengali text-2xl font-bold text-ivory">ম্যাচ ইতিহাস</h1>
        <p className="mt-1 text-sm text-muted">আপনার প্রতিটি ম্যাচের ফলাফল ও রেটিং পরিবর্তন।</p>
      </header>

      <FilterPills value={filter} counts={counts} onChange={setFilter} />

      {loading ? (
        <LoadingState label="ম্যাচের ইতিহাস লোড হচ্ছে…" />
      ) : error !== null ? (
        <ErrorState
          title="ইতিহাস লোড করা যায়নি"
          message={error}
          action={
            <Button variant="premium" onClick={retry}>
              আবার চেষ্টা করুন
            </Button>
          }
        />
      ) : shown.length === 0 ? (
        <EmptyState
          icon={<FileQuestion className="size-6" aria-hidden />}
          title={filter === "all" ? "কোনো ম্যাচ এখনো খেলা হয়নি" : "এই ফিল্টারে কোনো ম্যাচ নেই"}
          description={
            filter === "all"
              ? "প্রথম ম্যাচ শেষ হলে আপনার ফলাফল এখানে দেখা যাবে।"
              : "ফিল্টার বদলে সব ম্যাচ দেখুন।"
          }
        />
      ) : (
        <div className="grid gap-4 lg:grid-cols-2">
          {shown.map((match) => (
            <MatchHistoryCard key={match.matchId} match={match} onSelect={setSelected} />
          ))}
        </div>
      )}

      <MatchDetailsModal match={selected} onClose={() => setSelected(null)} />
    </div>
  );
}