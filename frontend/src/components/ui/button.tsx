"use client";

import type { ButtonHTMLAttributes, ReactNode } from "react";
import { forwardRef } from "react";
import { cn } from "@/lib/cn";
import { Loader2 } from "@/components/ui/icons";

export type ButtonVariant =
  | "primary"
  | "secondary"
  | "outline"
  | "ghost"
  | "danger"
  | "premium";

export type ButtonSize = "sm" | "md" | "lg" | "xl";

const VARIANT_CLASSES: Record<ButtonVariant, string> = {
  primary:
    "bg-gold-500 text-deep-950 hover:bg-gold-400 active:bg-gold-600 " +
    "shadow-[0_1px_0_rgb(255_255_255/0.18)_inset,0_10px_24px_-10px_rgb(201_165_60/0.6)] " +
    "hover:shadow-[0_1px_0_rgb(255_255_255/0.2)_inset,0_14px_30px_-10px_rgb(201_165_60/0.75)]",
  secondary:
    "bg-deep-700/80 text-ivory hover:bg-deep-650/90 border border-forest-500/25 " +
    "shadow-panel",
  outline:
    "bg-transparent text-ivory border border-gold-500/45 hover:border-gold-400 " +
    "hover:bg-gold-500/10 text-parchment-300",
  ghost:
    "bg-transparent text-muted hover:text-ivory hover:bg-deep-700/50 border border-transparent",
  danger:
    "bg-crimson-600 text-ivory hover:bg-crimson-500 border border-crimson-400/30 " +
    "shadow-[0_1px_0_rgb(255_255_255/0.12)_inset,0_10px_24px_-12px_rgb(176_58_76/0.7)]",
  premium:
    "bg-gradient-to-b from-gold-400 via-gold-500 to-gold-600 text-deep-950 " +
    "shadow-[0_1px_0_rgb(255_255_255/0.3)_inset,0_14px_34px_-12px_rgb(201_165_60/0.8)] " +
    "hover:brightness-110 active:brightness-95 border border-gold-300/60",
};

const SIZE_CLASSES: Record<ButtonSize, string> = {
  sm: "h-8 px-3 text-xs rounded-md gap-1.5",
  md: "h-10 px-4 text-sm rounded-lg gap-2",
  lg: "h-12 px-6 text-base rounded-xl gap-2.5",
  xl: "h-14 px-8 text-lg rounded-xl gap-3",
};

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  loading?: boolean;
  fullWidth?: boolean;
  children: ReactNode;
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      variant = "primary",
      size = "md",
      loading = false,
      fullWidth = false,
      className,
      disabled,
      children,
      ...rest
    },
    ref,
  ) => {
    return (
      <button
        ref={ref}
        disabled={disabled || loading}
        className={cn(
          "inline-flex items-center justify-center font-semibold tracking-wide",
          "transition-all duration-150 select-none",
          "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-400",
          "disabled:cursor-not-allowed disabled:opacity-45",
          VARIANT_CLASSES[variant],
          SIZE_CLASSES[size],
          fullWidth && "w-full",
          className,
        )}
        {...rest}
      >
        {loading ? <Loader2 className="size-4 animate-spin" /> : null}
        {children}
      </button>
    );
  },
);
Button.displayName = "Button";