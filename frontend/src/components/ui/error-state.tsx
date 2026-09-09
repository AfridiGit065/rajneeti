import type { ReactNode } from "react";
import { cn } from "@/lib/cn";
import { AlertTriangle } from "./icons";

interface ErrorStateProps {
  title?: string;
  message?: string;
  action?: ReactNode;
  className?: string;
}

export function ErrorState({
  title = "কিছু একটা ভুল হয়েছে",
  message = "আবার চেষ্টা করুন।",
  action,
  className,
}: ErrorStateProps) {
  return (
    <div
      className={cn(
        "flex flex-col items-center justify-center gap-3 rounded-2xl border border-crimson-500/30 bg-crimson-500/5 px-6 py-14 text-center",
        className,
      )}
      role="alert"
    >
      <div className="flex size-12 items-center justify-center rounded-full bg-crimson-600/20 text-crimson-300">
        <AlertTriangle className="size-6" aria-hidden />
      </div>
      <h3 className="font-medium text-ivory">{title}</h3>
      <p className="max-w-sm text-sm text-muted">{message}</p>
      {action ? <div className="mt-2">{action}</div> : null}
    </div>
  );
}