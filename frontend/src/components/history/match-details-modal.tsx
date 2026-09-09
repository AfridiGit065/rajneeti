import { Badge } from "@/components/ui/badge";
import { Modal } from "@/components/ui/modal";
import { Clock3, TrendingDown, TrendingUp, Users } from "@/components/ui/icons";
import { formatDate, formatRatingChange } from "@/lib/format";
import { cn } from "@/lib/cn";
import { ResultBadge } from "./result-badge";
import type { MatchHistoryEntry } from "@/types/user";

function SummaryBox({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="rounded-xl border border-forest-500/15 bg-deep-800/50 p-3">
      <p className="text-[0.68rem] font-semibold uppercase tracking-wider text-muted">{label}</p>
      <div className="mt-1 text-sm font-semibold text-ivory">{children}</div>
    </div>
  );
}

export function MatchDetailsModal({
  match,
  onClose,
}: {
  match: MatchHistoryEntry | null;
  onClose: () => void;
}) {
  return (
    <Modal
      open={match !== null}
      onClose={onClose}
      title={match ? `ম্যাচ ${match.matchId}` : undefined}
      subtitle={
        match
          ? `${formatDate(match.playedAt)} · ${match.durationMinutes} মিনিট · ${match.playerCount} জন`
          : undefined
      }
      size="md"
    >
      {match ? (
        <div className="space-y-5">
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
            <SummaryBox label="ফলাফল">
              <ResultBadge result={match.result} />
            </SummaryBox>
            <SummaryBox label="রেটিং পরিবর্তন">
              <span
                className={cn(
                  match.ratingChange > 0 && "text-forest-300",
                  match.ratingChange < 0 && "text-crimson-300",
                  match.ratingChange === 0 && "text-muted",
                )}
              >
                {match.ratingChange > 0 ? (
                  <TrendingUp className="mr-1 inline size-3.5" aria-hidden />
                ) : match.ratingChange < 0 ? (
                  <TrendingDown className="mr-1 inline size-3.5" aria-hidden />
                ) : null}
                {formatRatingChange(match.ratingChange)}
              </span>
            </SummaryBox>
            <SummaryBox label="ফাইনাল র‍্যাংক">
              <span className="font-mono">
                #{match.position} <span className="font-normal text-muted">/ {match.playerCount}</span>
              </span>
            </SummaryBox>
            <SummaryBox label="সময়কাল">
              <span className="flex items-center gap-1.5">
                <Clock3 className="size-3.5 text-gold-300" aria-hidden />
                {match.durationMinutes} মিনিট
              </span>
            </SummaryBox>
            <SummaryBox label="খেলোয়াড়">
              <span className="flex items-center gap-1.5">
                <Users className="size-3.5 text-gold-300" aria-hidden />
                {match.playerCount} জন
              </span>
            </SummaryBox>
            <SummaryBox label="মোড">{match.modeName ?? "ক্লাসিক"}</SummaryBox>
          </div>

          <div>
            <h3 className="mb-2 text-sm font-semibold uppercase tracking-[0.15em] text-muted">
              অংশগ্রহণকারী
            </h3>
            <ol className="space-y-2">
              {match.participants.map((p, index) => (
                <li
                  key={`${p.displayName}-${index}`}
                  className={cn(
                    "flex items-center gap-3 rounded-xl border px-3 py-2.5",
                    p.isCurrentUser
                      ? "border-gold-500/40 bg-gold-500/8"
                      : "border-forest-500/15 bg-deep-800/50",
                  )}
                >
                  <span className="w-9 shrink-0 text-center font-mono text-sm font-bold text-gold-300">
                    #{p.position}
                  </span>
                  <span
                    className="flex size-9 items-center justify-center rounded-full bg-gradient-to-b from-forest-500 to-forest-600 text-xs font-bold text-deep-950"
                    aria-hidden
                  >
                    {p.avatarInitial}
                  </span>
                  <span className="truncate font-medium text-ivory">{p.displayName}</span>
                  <span className="ml-auto shrink-0">
                    {p.isCurrentUser ? <Badge tone="gold">আপনি</Badge> : null}
                  </span>
                </li>
              ))}
            </ol>
          </div>
        </div>
      ) : null}
    </Modal>
  );
}