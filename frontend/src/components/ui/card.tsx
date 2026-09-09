import type { HTMLAttributes, ReactNode } from "react";
import { cn } from "@/lib/cn";

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
  interactive?: boolean;
  padded?: boolean;
}

export function Card({
  children,
  interactive = false,
  padded = true,
  className,
  ...rest
}: CardProps) {
  return (
    <div
      className={cn(
        "rounded-2xl border border-forest-500/25 bg-surface panel-emboss",
        padded && "p-6",
        interactive &&
          "transition-transform duration-150 hover:-translate-y-0.5 hover:border-gold-500/40 cursor-pointer",
        className,
      )}
      {...rest}
    >
      {children}
    </div>
  );
}