"use client";

import { useState } from "react";
import { cn } from "@/lib/cn";
import { Check, Copy } from "@/components/ui/icons";
import { useToast } from "@/hooks/use-toast";

export function RoomCode({
  code,
  size = "md",
  className,
}: {
  code: string;
  size?: "sm" | "md" | "lg";
  className?: string;
}) {
  const [copied, setCopied] = useState(false);
  const { success } = useToast();

  const sizes = {
    sm: "text-sm px-2.5 py-1 gap-1.5",
    md: "text-lg px-3.5 py-1.5 gap-2",
    lg: "text-2xl px-5 py-2.5 gap-2.5",
  };
  const badgeSizes = {
    sm: "size-3",
    md: "size-4",
    lg: "size-5",
  };

  async function copy() {
    try {
      await navigator.clipboard.writeText(code);
    } catch {
      // Clipboard may be unavailable; fall back to nothing.
    }
    setCopied(true);
    success("রুম কোড কপি হয়েছে", code);
    window.setTimeout(() => setCopied(false), 1600);
  }

  return (
    <button
      type="button"
      onClick={copy}
      className={cn(
        "inline-flex items-center rounded-lg border border-gold-500/35 bg-gold-500/8 text-gold-300",
        "font-mono font-semibold tracking-[0.2em] transition-colors",
        "hover:border-gold-400/60 hover:bg-gold-500/15",
        "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-400",
        sizes[size],
        className,
      )}
      aria-label={`রুম কোড ${code} কপি করুন`}
    >
      {code}
      {copied ? (
        <Check className={cn("text-forest-300", badgeSizes[size])} aria-hidden />
      ) : (
        <Copy className={cn("opacity-60", badgeSizes[size])} aria-hidden />
      )}
    </button>
  );
}