"use client";

import { useEffect, useRef } from "react";
import { cn } from "@/lib/cn";
import { ScrollText } from "@/components/ui/icons";
import type { GameLogEntry } from "@/types/game";

const KIND_COLOR: Record<GameLogEntry["kind"], string> = {
  info: "text-forest-400 bg-forest-400",
  action: "text-gold-400 bg-gold-400",
  challenge: "text-crimson-400 bg-crimson-400",
  block: "text-parchment-300 bg-parchment-300",
  reveal: "text-parchment-300 bg-parchment-300",
  elimination: "text-crimson-400 bg-crimson-400",
};

function formatTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleTimeString("en-US", { hour: "2-digit", minute: "2-digit" });
}

export function GameLog({
  entries,
  className,
}: {
  entries: GameLogEntry[];
  className?: string;
}) {
  const listRef = useRef<HTMLUListElement>(null);

  useEffect(() => {
    if (listRef.current) {
      listRef.current.scrollTop = listRef.current.scrollHeight;
    }
  }, [entries.length]);

  return (
    <div
      className={cn(
        "flex h-full flex-col overflow-hidden select-none",
        className,
      )}
    >
      {/* Header */}
      <div className="flex items-center justify-between border-b border-forest-500/15 px-3 py-2 bg-deep-950/40">
        <div className="flex items-center gap-1.5">
          <ScrollText className="size-3 text-gold-400" aria-hidden />
          <h2 className="font-cinzel text-[10px] font-bold uppercase tracking-widest text-muted">
            Chronicle / Log
          </h2>
        </div>
        <span className="font-mono text-[10px] font-semibold text-gold-400/80 bg-gold-500/10 rounded px-1.5 py-0.2 border border-gold-500/20">
          {entries.length}
        </span>
      </div>

      {/* Log list */}
      <ul ref={listRef} className="flex-1 space-y-2 overflow-y-auto px-3 py-2.5 scrollbar-thin">
        {entries.length === 0 ? (
          <li className="text-center py-6 text-xs text-muted/60">
            খেলা শুরু হয়েছে — ঘটনাগুলো এখানে প্রদর্শিত হবে।
          </li>
        ) : (
          entries.map((entry) => (
            <li key={entry.id} className="flex items-start gap-2 text-left">
              <span
                className={cn(
                  "mt-1.5 size-1.5 shrink-0 rounded-full",
                  KIND_COLOR[entry.kind] ?? "bg-forest-400",
                )}
                aria-hidden
              />
              <div className="min-w-0 flex-1 leading-tight">
                <p className="font-bengali text-xs leading-relaxed text-ivory/90">
                  {entry.textBn ?? entry.text}
                </p>
                <span className="font-mono text-[9px] text-muted/60">
                  {formatTime(entry.timestamp)}
                </span>
              </div>
            </li>
          ))
        )}
      </ul>
    </div>
  );
}