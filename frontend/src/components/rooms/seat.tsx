"use client";

import type { ReactNode } from "react";
import { cn } from "@/lib/cn";
import { MapPin } from "@/components/ui/icons";

interface SeatProps {
  seatIndex: number;
  empty?: boolean;
  delayMs?: number;
  children?: ReactNode;
}

function EmptySeat({ seatIndex }: { seatIndex: number }) {
  return (
    <div className="flex min-h-[88px] flex-col items-center justify-center gap-1.5 text-center">
      <span className="flex size-9 items-center justify-center rounded-full border border-dashed border-forest-500/30 text-muted/50">
        <MapPin className="size-4" aria-hidden />
      </span>
      <p className="text-xs text-muted/60">অপেক্ষমাণ আসন</p>
      {seatIndex === 0 ? <p className="text-[10px] text-muted/40">প্রথম খেলোয়াড় হোস্ট হয়</p> : null}
    </div>
  );
}

/** A positional seat slot in the room lobby (occupied by a RoomPlayerCard or empty). */
export function Seat({ seatIndex, empty = false, delayMs = 0, children }: SeatProps) {
  return (
    <div
      className={cn(
        "relative rounded-2xl border p-4 pt-7 animate-fade-up",
        empty
          ? "border-dashed border-forest-500/25 bg-deep-900/30"
          : "border-forest-500/30 bg-surface panel-emboss",
      )}
      style={{ animationDelay: `${delayMs}ms` }}
    >
      <span className="absolute right-3 top-2.5 text-[10px] font-semibold uppercase tracking-widest text-muted">
        আসন {seatIndex + 1}
      </span>
      {children ?? <EmptySeat seatIndex={seatIndex} />}
    </div>
  );
}