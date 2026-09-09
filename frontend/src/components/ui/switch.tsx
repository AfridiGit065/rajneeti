"use client";

import { cn } from "@/lib/cn";

interface SwitchProps {
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
  disabled?: boolean;
  size?: "sm" | "md";
  id?: string;
  className?: string;
  "aria-label"?: string;
}

const TRACK: Record<NonNullable<SwitchProps["size"]>, string> = {
  sm: "h-5 w-9",
  md: "h-6 w-11",
};

const KNOB: Record<NonNullable<SwitchProps["size"]>, string> = {
  sm: "size-4",
  md: "size-5",
};

const KNOB_POSITION: Record<NonNullable<SwitchProps["size"]>, string> = {
  sm: "translate-x-4",
  md: "translate-x-5",
};

/** Accessible toggle switch. */
export function Switch({
  checked,
  onCheckedChange,
  disabled = false,
  size = "md",
  id,
  className,
  "aria-label": ariaLabel,
}: SwitchProps) {
  return (
    <button
      type="button"
      id={id}
      role="switch"
      aria-checked={checked}
      aria-label={ariaLabel}
      disabled={disabled}
      onClick={() => onCheckedChange(!checked)}
      className={cn(
        "relative inline-flex shrink-0 cursor-pointer items-center rounded-full border transition-colors duration-200",
        "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-400",
        "disabled:cursor-not-allowed disabled:opacity-45",
        TRACK[size],
        className,
        checked
          ? "border-gold-400/60 bg-gold-500/25 shadow-[0_1px_0_rgb(255_255_255/0.15)_inset,0_0_16px_-4px_rgb(201_165_60/0.5)]"
          : "border-forest-500/30 bg-deep-750/80",
      )}
    >
      <span
        aria-hidden
        className={cn(
          "block rounded-full shadow transition-transform duration-200",
          KNOB[size],
          checked
            ? cn("translate-x-0 bg-gold-400", KNOB_POSITION[size])
            : "translate-x-0.5 bg-forest-100/80",
        )}
      />
    </button>
  );
}