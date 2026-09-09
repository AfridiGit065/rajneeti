"use client";

import { Timer } from "./timer";
import { Flame } from "@/components/ui/icons";
import { cn } from "@/lib/cn";

export function TurnTimer({
  playerName,
  turnNumber,
  seconds = 30,
  isSelf = false,
  className,
}: {
  playerName: string;
  turnNumber: number;
  seconds?: number;
  isSelf?: boolean;
  className?: string;
}) {
  return (
    <div className={cn("flex items-center gap-3", className)}>
      <span
        className={cn(
          "flex items-center gap-1.5 text-sm font-medium",
          isSelf ? "text-gold-300" : "text-muted",
        )}
      >
        <Flame className="size-4 text-gold-400" aria-hidden />
        {isSelf ? "আপনার পালা" : `${playerName} এর পালা`}
      </span>
      <Timer key={turnNumber} seconds={seconds} compact label="s" />
    </div>
  );
}