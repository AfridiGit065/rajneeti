"use client";

import type { SelectHTMLAttributes } from "react";
import { forwardRef, useId } from "react";
import { cn } from "@/lib/cn";

interface SelectOption {
  value: string;
  label: string;
  disabled?: boolean;
}

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  options: readonly SelectOption[];
  error?: string;
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  ({ label, options, error, className, id, ...rest }, ref) => {
    const autoId = useId();
    const selectId = id ?? autoId;
    return (
      <div className="w-full">
        {label ? (
          <label
            htmlFor={selectId}
            className="mb-1.5 block text-sm font-medium text-parchment-300"
          >
            {label}
          </label>
        ) : null}
        <select
          ref={ref}
          id={selectId}
          className={cn(
            "w-full cursor-pointer appearance-none rounded-lg border bg-deep-900/80 px-4 py-2.5 text-ivory",
            "transition-colors duration-150",
            "focus:outline-none focus:ring-2 focus:ring-forest-400/60 focus:border-forest-400/50",
            "disabled:cursor-not-allowed disabled:opacity-50",
            "bg-[url('data:image/svg+xml;utf8,<svg xmlns=%22http://www.w3.org/2000/svg%22 fill=%22none%22 viewBox=%220 0 24 24%22 stroke=%22%23ddc9a4%22 stroke-width=%222%22><path stroke-linecap=%22round%22 stroke-linejoin=%22round%22 d=%22M19 9l-7 7-7-7%22/></svg>')] bg-[length:16px] bg-[right_0.75rem_center] bg-no-repeat pr-10",
            error
              ? "border-crimson-500/70"
              : "border-forest-500/25",
            className,
          )}
          {...rest}
        >
          {options.map((opt) => (
            <option
              key={opt.value}
              value={opt.value}
              disabled={opt.disabled}
              className="bg-deep-900 text-ivory"
            >
              {opt.label}
            </option>
          ))}
        </select>
        {error ? (
          <p className="mt-1.5 text-xs text-crimson-300" role="alert">
            {error}
          </p>
        ) : null}
      </div>
    );
  },
);
Select.displayName = "Select";