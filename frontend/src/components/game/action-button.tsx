"use client";

import type { ButtonHTMLAttributes, ReactNode } from "react";
import { forwardRef } from "react";
import { cn } from "@/lib/cn";
import { Loader2 } from "@/components/ui/icons";

export type ActionKind = "primary" | "secondary" | "danger" | "gold" | "ghost";
export type ActionSize = "sm" | "md" | "lg";

const KIND_CLASSES: Record<ActionKind, string> = {
  primary:
    "bg-gold-500 text-deep-950 hover:bg-gold-400 active:bg-gold-600 " +
    "shadow-[0_1px_0_rgb(255_255_255/0.18)_inset,0_10px_24px_-10px_rgb(201_165_60/0.65)] " +
    "hover:shadow-[0_1px_0_rgb(255_255_255/0.22)_inset,0_14px_30px_-10px_rgb(201_165_60/0.8)]",
  secondary:
    "bg-deep-700/80 text-ivory hover:bg-deep-650/90 border border-forest-500/25 shadow-panel",
  danger:
    "bg-crimson-600 text-ivory hover:bg-crimson-500 active:bg-crimson-700 " +
    "border border-crimson-400/30 " +
    "shadow-[0_1px_0_rgb(255_255_255/0.12)_inset,0_10px_24px_-12px_rgb(176_58_76/0.7)]",
  gold:
    "bg-gradient-to-b from-gold-300 via-gold-400 to-gold-600 text-deep-950 " +
    "shadow-[0_1px_0_rgb(255_255_255/0.3)_inset,0_14px_34px_-12px_rgb(201_165_60/0.85)] " +
    "hover:brightness-110 active:brightness-95 border border-gold-300/60",
  ghost:
    "bg-transparent text-parchment-300 hover:bg-gold-500/10 hover:text-gold-300 " +
    "border border-transparent hover:border-gold-500/30",
};

const SIZE_CLASSES: Record<ActionSize, string> = {
  sm: "h-8 px-3 text-xs rounded-md gap-1.5",
  md: "h-10 px-4 text-sm rounded-lg gap-2",
  lg: "h-12 px-6 text-base rounded-xl gap-2.5",
};

interface ActionButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  kind?: ActionKind;
  size?: ActionSize;
  icon?: ReactNode;
  label: string;
  active?: boolean;
  loading?: boolean;
  fullWidth?: boolean;
}

/** High-emphasis gameplay control: distinct from the generic Button. */
export const ActionButton = forwardRef<HTMLButtonElement, ActionButtonProps>(
  (
    {
      kind = "primary",
      size = "md",
      icon,
      label,
      active = false,
      loading = false,
      fullWidth = false,
      className,
      children,
      disabled,
      ...rest
    },
    ref,
  ) => {
    const isDisabled = disabled || loading;
    return (
      <button
        ref={ref}
        disabled={isDisabled}
        aria-pressed={active || undefined}
        aria-busy={loading || undefined}
        className={cn(
          "inline-flex items-center justify-center gap-2 font-semibold uppercase tracking-[0.08em] select-none",
          "transition-all duration-150",
          "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-400",
          "disabled:cursor-not-allowed disabled:opacity-45",
          active && "ring-2 ring-gold-400/70 shadow-gold",
          KIND_CLASSES[kind],
          SIZE_CLASSES[size],
          fullWidth && "w-full",
          className,
        )}
        {...rest}
      >
        {loading ? <Loader2 className="size-4 animate-spin" aria-hidden /> : icon}
        <span>{children ?? label}</span>
      </button>
    );
  },
);
ActionButton.displayName = "ActionButton";