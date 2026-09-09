"use client";

import { cn } from "@/lib/cn";
import { Coins } from "@/components/ui/icons";

export function CoinDisplay({
  coins,
  size = "md",
  animate = false,
  className,
}: {
  coins: number;
  size?: "sm" | "md" | "lg";
  animate?: boolean;
  className?: string;
}) {
  const sizes = {
    sm: "gap-1 text-sm",
    md: "gap-1.5 text-base",
    lg: "gap-2 text-xl",
  };
  const iconSizes = { sm: "size-3.5", md: "size-4.5", lg: "size-6" };

  return (
    <span
      className={cn(
        "inline-flex items-center font-semibold tabular-nums",
        sizes[size],
        className,
      )}
      aria-label={`${coins} কয়েন`}
    >
      <Coins
        className={cn("text-gold-400", iconSizes[size], animate && "animate-coin-pop")}
        aria-hidden
      />
      {coins}
    </span>
  );
}