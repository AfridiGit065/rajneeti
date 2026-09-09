"use client";

import { cn } from "@/lib/cn";
import { Shield } from "@/components/ui/icons";

export function InfluenceDisplay({
  count,
  revealed = 0,
  size = "md",
  className,
}: {
  count: number;
  revealed?: number;
  size?: "sm" | "md" | "lg";
  className?: string;
}) {
  const sizes = {
    sm: "gap-1 text-sm",
    md: "gap-1.5 text-base",
    lg: "gap-2 text-xl",
  };
  const iconSizes = { sm: "size-3.5", md: "size-4.5", lg: "size-6" };

  const lostColor = count === 0 ? "text-crimson-300" : "text-forest-300";

  return (
    <span
      className={cn(
        "inline-flex items-center font-semibold tabular-nums",
        sizes[size],
        className,
      )}
      aria-label={`${count} ইনফ্লুয়েন্স${revealed > 0 ? `, ${revealed} উন্মোচিত` : ""}`}
    >
      <Shield className={cn(iconSizes[size], lostColor)} aria-hidden />
      {count}
      {revealed > 0 ? (
        <span className="text-[0.7em] text-muted">({revealed})</span>
      ) : null}
    </span>
  );
}