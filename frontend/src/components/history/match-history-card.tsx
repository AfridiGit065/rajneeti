import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { ChevronRight, Clock3, TrendingDown, TrendingUp } from "@/components/ui/icons";
import { formatDate, formatRatingChange } from "@/lib/format";
import { ResultBadge } from "./result-badge";
import type { MatchHistoryEntry } from "@/types/user";

function RatingChange({ change }: { change: number }) {
  if (change > 0) {
    return (
      <span className="text-forest-300">
        <TrendingUp className="mr-1 inline size-3.5" aria-hidden />
        {formatRatingChange(change)}
      </span>
    );
  }
  if (change < 0) {
    return (
      <span className="text-crimson-300">
        <TrendingDown className="mr-1 inline size-3.5" aria-hidden />
        {formatRatingChange(change)}
      </span>
    );
  }
  return <span className="text-muted">{formatRatingChange(0)}</span>;
}

export function MatchHistoryCard({
  match,
  onSelect,
}: {
  match: MatchHistoryEntry;
  onSelect: (match: MatchHistoryEntry) => void;
}) {
  const extraPlayers = match.participants.length - 3;

  return (
    <Card interactive className="flex flex-col gap-4 animate-fade-up">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="font-mono text-xs font-medium tracking-wide text-gold-300">
            {match.matchId}
          </p>
          <p className="mt-1 text-sm text-muted">
            {formatDate(match.playedAt)}
            {match.modeName ? <span className="text-muted/70"> · {match.modeName}</span> : null}
          </p>
        </div>
        <ResultBadge result={match.result} />
      </div>

      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-1.5">
          <div className="flex -space-x-2">
            {match.participants.slice(0, 3).map((p, index) => (
              <span
                key={`${match.matchId}-${p.displayName}-${index}`}
                className="flex size-8 items-center justify-center rounded-full border-2 border-deep-900 bg-gradient-to-b from-forest-500 to-forest-600 text-xs font-bold text-deep-950"
                title={p.displayName}
              >
                {p.avatarInitial}
              </span>
            ))}
          </div>
          <span className="ml-2 text-sm text-muted">{match.playerCount} জন</span>
        </div>
        <div className="text-right">
          <p className="text-[0.68rem] font-semibold uppercase tracking-wider text-muted">
            ফাইনাল র‍্যাংক
          </p>
          <p className="font-mono text-sm font-bold text-ivory">
            #{match.position} <span className="font-normal text-muted">/ {match.playerCount}</span>
          </p>
        </div>
        <div className="text-right">
          <p className="text-[0.68rem] font-semibold uppercase tracking-wider text-muted">
            রেটিং
          </p>
          <p className="text-sm font-semibold">
            <RatingChange change={match.ratingChange} />
          </p>
        </div>
      </div>

      <div className="mt-auto flex items-center justify-between gap-3 border-t border-forest-500/15 pt-3">
        <span className="flex items-center gap-1.5 text-xs text-muted">
          <Clock3 className="size-3.5" aria-hidden />
          {match.durationMinutes} মিনিট
        </span>
        {extraPlayers > 0 ? (
          <span className="text-xs text-muted">+{extraPlayers} জন অন্যান্য</span>
        ) : null}
        <Button variant="outline" size="sm" onClick={() => onSelect(match)}>
          বিস্তারিত
          <ChevronRight className="size-4" aria-hidden />
        </Button>
      </div>
    </Card>
  );
}