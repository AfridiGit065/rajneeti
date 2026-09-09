"use client";

import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/cn";
import { ScrollText } from "@/components/ui/icons";
import type { GameLogEntry } from "@/types/game";

const KIND_COLOR: Record<GameLogEntry["kind"], string> = {
  info: "text-forest-300",
  action: "text-gold-300",
  challenge: "text-crimson-300",
  block: "text-parchment-300",
  reveal: "text-parchment-300",
  elimination: "text-crimson-300",
};

function formatTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleTimeString("bn-BD", { hour: "2-digit", minute: "2-digit" });
}

export function GameLog({ entries, className }: { entries: GameLogEntry[]; className?: string }) {
  return (
    <div
      className={cn(
        "flex max-h-[min(60vh,540px)] flex-col rounded-2xl border border-forest-500/25 bg-surface panel-emboss",
        className,
      )}
    >
      <div className="flex items-center justify-between gap-3 border-b border-forest-500/20 px-4 py-3">
        <h2 className="flex items-center gap-2 font-bengali text-base font-semibold text-ivory">
          <ScrollText className="size-4 text-gold-400" aria-hidden />
          গেম লগ
        </h2>
        <Badge tone="neutral">{entries.length}</Badge>
      </div>
      <ul className="flex-1 space-y-2.5 overflow-y-auto px-4 py-3">
        {entries.length === 0 ? (
          <li className="text-sm text-muted">এখনো কোনো ঘটনা ঘটেনি।</li>
        ) : (
          entries.map((entry) => (
            <li key={entry.id} className="flex items-start gap-2">
              <span
                className={cn("mt-1.5 size-1.5 shrink-0 rounded-full bg-current", KIND_COLOR[entry.kind])}
                aria-hidden
              />
              <div className="min-w-0">
                <p className="text-sm leading-snug text-ivory/90">
                  {entry.textBn ?? entry.text}
                </p>
                <p className="font-mono text-[0.65rem] text-muted">{formatTime(entry.timestamp)}</p>
              </div>
            </li>
          ))
        )}
      </ul>
    </div>
  );
}