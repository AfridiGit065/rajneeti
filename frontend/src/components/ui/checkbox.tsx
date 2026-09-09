"use client";

import type { InputHTMLAttributes, ReactNode } from "react";
import { forwardRef } from "react";
import { cn } from "@/lib/cn";
import { Check } from "@/components/ui/icons";

interface CheckboxProps extends Omit<InputHTMLAttributes<HTMLInputElement>, "type"> {
  label?: ReactNode;
}

export const Checkbox = forwardRef<HTMLInputElement, CheckboxProps>(
  ({ label, className, checked, ...rest }, ref) => {
    return (
      <label
        className={cn(
          "inline-flex cursor-pointer items-center gap-2.5 select-none text-sm text-parchment-300",
          className,
        )}
      >
        <span className="relative flex size-[18px] shrink-0">
          <input ref={ref} type="checkbox" checked={checked} className="peer sr-only" {...rest} />
          <span
            aria-hidden
            className={cn(
              "flex size-[18px] items-center justify-center rounded border bg-deep-900 transition-colors duration-150",
              "peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-gold-400",
              checked
                ? "border-gold-500/70 bg-gold-500/20"
                : "border-forest-500/40 hover:border-forest-400/60",
            )}
          >
            <Check className={cn("size-3", checked ? "text-gold-300" : "text-transparent")} />
          </span>
        </span>
        {label ?? null}
      </label>
    );
  },
);
Checkbox.displayName = "Checkbox";