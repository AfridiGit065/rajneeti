import type { ReactNode } from "react";
import { cn } from "@/lib/cn";
import { Crown } from "@/components/ui/icons";

interface AuthShellProps {
  children: ReactNode;
  footer?: ReactNode;
  eyebrow: string;
  title: string;
  subtitle?: string;
  className?: string;
}

export function AuthShell({
  children,
  footer,
  eyebrow,
  title,
  subtitle,
  className,
}: AuthShellProps) {
  return (
    <div className={cn("flex w-full justify-center py-6 sm:py-10", className)}>
      <div className="w-full max-w-md animate-fade-up">
        <div className="mb-6 text-center">
          <div className="mx-auto mb-3 inline-flex h-12 w-12 items-center justify-center rounded-xl border border-gold-500/30 bg-deep-800 shadow-panel">
            <Crown className="size-6 text-gold-400" aria-hidden />
          </div>
          <p className="font-bengali text-2xl font-bold text-ivory">রাজনীতি</p>
          <p className="text-[10px] font-semibold uppercase tracking-[0.4em] text-gold-500">
            RAJNEETI · The Game of Power
          </p>
        </div>

        <div className="rounded-2xl border border-forest-500/25 bg-surface panel-emboss panel-texture p-6 sm:p-8">
          <div className="mb-5 border-b border-forest-500/20 pb-5">
            <p className="mb-1 text-xs font-semibold uppercase tracking-[0.25em] text-gold-400">
              {eyebrow}
            </p>
            <h1 className="font-bengali text-2xl font-bold text-ivory">{title}</h1>
            {subtitle ? <p className="mt-2 text-sm leading-relaxed text-muted">{subtitle}</p> : null}
          </div>
          {children}
          {footer ? (
            <div className="mt-6 border-t border-forest-500/20 pt-5 text-center text-sm text-muted">
              {footer}
            </div>
          ) : null}
        </div>
      </div>
    </div>
  );
}