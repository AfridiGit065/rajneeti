"use client";

import { cn } from "@/lib/cn";
import { useCountdown } from "@/hooks/use-countdown";
import { TimerReset } from "@/components/ui/icons";

interface TimerProps {
  seconds: number;
  label?: string;
  className?: string;
  compact?: boolean;
}

/**
 * Restart a turn timer by remounting with a changing key, e.g.
 * `<Timer key={turnNumber} seconds={30} />`.
 */
export function Timer({ seconds, label, className, compact = false }: TimerProps) {
  const { display, expired } = useCountdown(seconds);

  return (
    <div
      className={cn(
        "inline-flex items-center gap-2 rounded-lg border px-3 py-1.5",
        expired
          ? "border-crimson-500/50 bg-crimson-600/15 text-crimson-300"
          : "border-gold-500/35 bg-gold-500/8 text-gold-300",
        compact && "px-2 py-1 text-xs",
        className,
      )}
      role="timer"
      aria-label={label ? `${label}: ${display}` : `অবশিষ্ট সময়: ${display}`}
    >
      <TimerReset className={cn("shrink-0", compact ? "size-3.5" : "size-4")} aria-hidden />
      <span className="font-mono font-semibold tabular-nums">{display}</span>
      {label ? <span className="text-xs text-muted">{label}</span> : null}
    </div>
  );
}