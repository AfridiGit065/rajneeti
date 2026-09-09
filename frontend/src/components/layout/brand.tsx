"use client";

import Link from "next/link";
import { cn } from "@/lib/cn";
import { Landmark } from "@/components/ui/icons";

export function Brand({
  size = "md",
  subtitle = true,
  className,
}: {
  size?: "sm" | "md" | "lg";
  subtitle?: boolean;
  className?: string;
}) {
  const mark = { sm: "size-8", md: "size-10", lg: "size-12" };
  const name = { sm: "text-lg", md: "text-xl", lg: "text-2xl" };
  const sub = { sm: "text-[0.5rem]", md: "text-[0.55rem]", lg: "text-xs" };

  return (
    <Link
      href="/"
      className={cn(
        "inline-flex items-center gap-3 rounded-lg focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-400",
        className,
      )}
      aria-label="রাজনীতি — হোম"
    >
      <span
        className={cn(
          mark[size],
          "relative flex items-center justify-center rounded-xl border border-gold-500/50 bg-gradient-to-b from-deep-700 to-deep-900 text-gold-300 shadow-gold animate-glow-pulse",
        )}
      >
        <Landmark className="size-[60%]" aria-hidden />
      </span>
      <span className="flex flex-col leading-none">
        <span className={cn(name[size], "font-bengali font-bold text-ivory")}>
          রাজনীতি
        </span>
        <span className={cn(sub[size], "font-semibold uppercase tracking-[0.3em] text-gold-400")}>
          RAJNEETI
        </span>
        {subtitle ? (
          <span className={cn(sub[size], "mt-0.5 tracking-wider text-muted")}>
            The Game of Power
          </span>
        ) : null}
      </span>
    </Link>
  );
}