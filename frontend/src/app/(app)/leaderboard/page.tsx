"use client";

import { useCallback, useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/ui/empty-state";
import { ErrorState } from "@/components/ui/error-state";
import { Input } from "@/components/ui/input";
import { LoadingState } from "@/components/ui/loading-state";
import { Search, Trophy } from "@/components/ui/icons";
import {
  LeaderboardTable,
  LeaderboardTabs,
  type LeaderboardTab,
} from "@/components/leaderboard";
import { MOCK_CURRENT_USER } from "@/mocks/users";
import { MetaService } from "@/services/meta-service";
import { useAuthStore } from "@/store/auth-store";
import type { LeaderboardEntry } from "@/types/user";

export default function LeaderboardPage() {
  const [tab, setTab] = useState<LeaderboardTab>("global");
  const [entries, setEntries] = useState<LeaderboardEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const currentUserId = useAuthStore((s) => s.user)?.id ?? MOCK_CURRENT_USER.id;

  const fetchBoard = useCallback(async () => MetaService.getLeaderboard(), []);

  useEffect(() => {
    let cancelled = false;
    fetchBoard().then((result) => {
      if (cancelled) return;
      if (result.ok) {
        setEntries(result.data);
      } else {
        setError(result.error.message);
      }
      setLoading(false);
    });
    return () => {
      cancelled = true;
    };
  }, [fetchBoard]);

  async function retry() {
    setLoading(true);
    setError(null);
    const result = await fetchBoard();
    if (result.ok) {
      setEntries(result.data);
    } else {
      setError(result.error.message);
    }
    setLoading(false);
  }

  const q = query.trim().toLowerCase();
  const shown = q
    ? entries.filter(
        (entry) =>
          entry.user.displayName.toLowerCase().includes(q) ||
          entry.user.username.toLowerCase().includes(q),
      )
    : entries;

  const isGlobal = tab === "global";
  const emptyTitle =
    isGlobal
      ? "কোনো খেলোয়াড় পাওয়া যায়নি"
      : tab === "weekly"
        ? "সাপ্তাহিক ডেটা এখনো নেই"
        : "বন্ধু তালিকা এখনো খালি";
  const emptyDescription =
    isGlobal
      ? "অন্য নাম দিয়ে খুঁজে দেখুন।"
      : tab === "weekly"
        ? "শীঘ্রই সাপ্তাহিক র‍্যাংকিং চালু হবে।"
        : "বন্ধুদের আমন্ত্রণ জানিয়ে প্রতিযোগিতা শুরু করুন।";

  return (
    <div className="space-y-6">
      <header>
        <p className="text-xs font-semibold uppercase tracking-[0.25em] text-gold-400">
          র‍্যাংকিং
        </p>
        <h1 className="mt-1 font-bengali text-2xl font-bold text-ivory">লিডারবোর্ড</h1>
        <p className="mt-1 text-sm text-muted">সেরা খেলোয়াড়দের রেটিং ও পরিসংখ্যান।</p>
      </header>

      <div className="flex flex-wrap items-center justify-between gap-4">
        <LeaderboardTabs value={tab} onChange={setTab} />
        <div className="w-full sm:w-72">
          <Input
            aria-label="খেলোয়াড় খুঁজুন"
            placeholder="খেলোয়াড় খুঁজুন…"
            leadingIcon={<Search className="size-4" aria-hidden />}
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
        </div>
      </div>

      {loading ? (
        <LoadingState label="লিডারবোর্ড লোড হচ্ছে…" />
      ) : error !== null ? (
        <ErrorState
          title="লিডারবোর্ড লোড করা যায়নি"
          message={error}
          action={
            <Button variant="premium" onClick={retry}>
              আবার চেষ্টা করুন
            </Button>
          }
        />
      ) : isGlobal && shown.length > 0 ? (
        <LeaderboardTable entries={shown} currentUserId={currentUserId} />
      ) : (
        <EmptyState
          icon={<Trophy className="size-6" aria-hidden />}
          title={emptyTitle}
          description={emptyDescription}
        />
      )}
    </div>
  );
}