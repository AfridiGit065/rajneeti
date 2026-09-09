"use client";

import type { InputHTMLAttributes, ReactNode } from "react";
import { forwardRef, useId } from "react";
import { cn } from "@/lib/cn";

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  hint?: string;
  error?: string;
  leadingIcon?: ReactNode;
  trailingIcon?: ReactNode;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, hint, error, leadingIcon, trailingIcon, className, id, ...rest }, ref) => {
    const autoId = useId();
    const inputId = id ?? autoId;
    return (
      <div className="w-full">
        {label ? (
          <label
            htmlFor={inputId}
            className="mb-1.5 block text-sm font-medium text-parchment-300"
          >
            {label}
          </label>
        ) : null}
        <div className="relative">
          {leadingIcon ? (
            <span className="pointer-events-none absolute inset-y-0 left-3 flex items-center text-muted">
              {leadingIcon}
            </span>
          ) : null}
          <input
            ref={ref}
            id={inputId}
            className={cn(
              "w-full rounded-lg border bg-deep-900/80 px-4 py-2.5 text-ivory",
              "placeholder:text-muted/60 transition-colors duration-150",
              "focus:outline-none focus:ring-2 focus:ring-forest-400/60 focus:border-forest-400/50",
              "disabled:cursor-not-allowed disabled:opacity-50",
              leadingIcon ? "pl-10" : "",
              trailingIcon ? "pr-10" : "",
              error
                ? "border-crimson-500/70 focus:ring-crimson-400/50 focus:border-crimson-400"
                : "border-forest-500/25",
              className,
            )}
            {...rest}
          />
          {trailingIcon ? (
            <span className="pointer-events-none absolute inset-y-0 right-3 flex items-center text-muted">
              {trailingIcon}
            </span>
          ) : null}
        </div>
        {error ? (
          <p className="mt-1.5 text-xs text-crimson-300" role="alert">
            {error}
          </p>
        ) : hint ? (
          <p className="mt-1.5 text-xs text-muted">{hint}</p>
        ) : null}
      </div>
    );
  },
);
Input.displayName = "Input";