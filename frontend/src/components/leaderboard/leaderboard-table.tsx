import { cn } from "@/lib/cn";
import { Badge } from "@/components/ui/badge";
import { Crown, Medal } from "@/components/ui/icons";
import type { LeaderboardEntry } from "@/types/user";

const PODIUM: Record<number, string> = {
  1: "bg-gradient-to-r from-gold-500/15 via-deep-800/60 to-transparent",
  2: "bg-gradient-to-r from-parchment-500/10 via-deep-800/60 to-transparent",
  3: "bg-gradient-to-r from-parchment-700/10 via-deep-800/60 to-transparent",
};

function RankCell({ rank }: { rank: number }) {
  if (rank === 1) {
    return (
      <span className="flex size-8 items-center justify-center rounded-full bg-gradient-to-b from-gold-400 to-gold-600 text-deep-950 shadow-gold">
        <Crown className="size-4" aria-hidden />
      </span>
    );
  }
  if (rank === 2 || rank === 3) {
    return (
      <span className="flex size-8 items-center justify-center rounded-full bg-gradient-to-b from-parchment-300 to-parchment-600 text-deep-950">
        <Medal className="size-4" aria-hidden />
      </span>
    );
  }
  return <span className="font-mono text-lg font-bold text-muted">{rank}</span>;
}

function winRateClass(rate: number) {
  if (rate >= 0.6) return "text-forest-300";
  if (rate >= 0.5) return "text-gold-300";
  return "text-muted";
}

export function LeaderboardTable({
  entries,
  currentUserId,
}: {
  entries: LeaderboardEntry[];
  currentUserId: string;
}) {
  return (
    <div className="overflow-x-auto rounded-2xl border border-forest-500/25 bg-deep-900/60 panel-emboss">
      <table className="w-full min-w-[680px] border-collapse text-left">
        <caption className="sr-only">গ্লোবাল লিডারবোর্ড</caption>
        <thead>
          <tr className="border-b border-forest-500/20 text-[0.68rem] font-semibold uppercase tracking-[0.2em] text-muted">
            <th scope="col" className="px-4 py-3.5">
              র‍্যাংক
            </th>
            <th scope="col" className="px-4 py-3.5">
              খেলোয়াড়
            </th>
            <th scope="col" className="px-4 py-3.5 text-right">
              রেটিং
            </th>
            <th scope="col" className="px-4 py-3.5 text-right">
              জয়
            </th>
            <th scope="col" className="px-4 py-3.5 text-right">
              ম্যাচ
            </th>
            <th scope="col" className="px-4 py-3.5 text-right">
              জয়ের হার
            </th>
          </tr>
        </thead>
        <tbody>
          {entries.map((entry) => {
            const isCurrent = entry.user.id === currentUserId;
            const rating = entry.rating ?? entry.points;
            return (
              <tr
                key={entry.rank}
                className={cn(
                  "border-b border-forest-500/10 transition-colors last:border-b-0",
                  "hover:bg-deep-800/50",
                  PODIUM[entry.rank],
                  isCurrent && "bg-gold-500/5 ring-1 ring-inset ring-gold-500/40",
                )}
              >
                <td className="px-4 py-3.5">
                  <RankCell rank={entry.rank} />
                </td>
                <td className="px-4 py-3.5">
                  <div className="flex items-center gap-3">
                    <span
                      className={cn(
                        "flex size-9 shrink-0 items-center justify-center rounded-full bg-gradient-to-b from-forest-500 to-forest-600 text-sm font-bold text-deep-950",
                        entry.user.titled && "ring-1 ring-gold-400/70",
                      )}
                      aria-hidden
                    >
                      {entry.user.avatarInitial}
                    </span>
                    <span className="flex min-w-0 flex-col leading-tight">
                      <span
                        className={cn(
                          "truncate font-semibold",
                          entry.rank === 1 ? "text-gold-300" : "text-ivory",
                        )}
                      >
                        {entry.user.displayName}
                      </span>
                      <span className="truncate text-xs text-muted">@{entry.user.username}</span>
                    </span>
                    {isCurrent ? <Badge tone="gold">আপনি</Badge> : null}
                  </div>
                </td>
                <td className="px-4 py-3.5 text-right font-mono text-sm font-semibold text-ivory">
                  {rating}
                </td>
                <td className="px-4 py-3.5 text-right font-mono text-sm text-ivory">
                  {entry.wins}
                </td>
                <td className="px-4 py-3.5 text-right font-mono text-sm text-ivory">
                  {entry.gamesPlayed}
                </td>
                <td
                  className={cn(
                    "px-4 py-3.5 text-right font-mono text-sm font-semibold",
                    winRateClass(entry.winRate),
                  )}
                >
                  {Math.round(entry.winRate * 100)}%
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}