"use client";

import type { ButtonHTMLAttributes } from "react";
import { forwardRef } from "react";
import { cn } from "@/lib/cn";
import { X } from "@/components/ui/icons";

interface IconButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  label: string;
  variant?: "default" | "gold" | "danger";
  size?: "sm" | "md" | "lg";
  active?: boolean;
}

const VARIANT: Record<NonNullable<IconButtonProps["variant"]>, string> = {
  default:
    "text-muted hover:text-ivory hover:bg-deep-700/60 border border-deep-700/60",
  gold: "text-gold-300 hover:text-gold-200 hover:bg-gold-500/10 border border-gold-500/30",
  danger: "text-crimson-300 hover:text-crimson-200 hover:bg-crimson-600/15 border border-crimson-500/30",
};

const SIZE: Record<NonNullable<IconButtonProps["size"]>, string> = {
  sm: "size-8 rounded-lg",
  md: "size-10 rounded-lg",
  lg: "size-12 rounded-xl",
};

export const IconButton = forwardRef<HTMLButtonElement, IconButtonProps>(
  ({ label, variant = "default", size = "md", active, className, children, ...rest }, ref) => {
    return (
      <button
        ref={ref}
        aria-label={label}
        title={label}
        className={cn(
          "inline-flex items-center justify-center shrink-0",
          "transition-all duration-150 select-none",
          "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-400",
          "disabled:cursor-not-allowed disabled:opacity-45",
          VARIANT[variant],
          SIZE[size],
          active && "text-gold-300 border-gold-500/50 bg-gold-500/10",
          className,
        )}
        {...rest}
      >
        {children ?? <X className="size-5" aria-hidden />}
      </button>
    );
  },
);
IconButton.displayName = "IconButton";