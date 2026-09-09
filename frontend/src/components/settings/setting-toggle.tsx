"use client";

import type { ReactNode } from "react";
import { Switch } from "@/components/ui/switch";

interface SettingToggleProps {
  icon?: ReactNode;
  title: string;
  titleEn?: string;
  description?: string;
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
  disabled?: boolean;
}

/** One settings row: label/description on the left, accessible Switch on the right. */
export function SettingToggle({
  icon,
  title,
  titleEn,
  description,
  checked,
  onCheckedChange,
  disabled = false,
}: SettingToggleProps) {
  return (
    <div className="flex items-start justify-between gap-4 p-6">
      <div className="flex min-w-0 items-start gap-3">
        {icon ? (
          <span className="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-md border border-forest-500/20 bg-deep-800/60 text-muted">
            {icon}
          </span>
        ) : null}
        <div className="min-w-0">
          <p className="text-sm font-semibold text-ivory">
            {title}
            {titleEn ? (
              <span className="ml-2 text-xs font-normal text-muted">{titleEn}</span>
            ) : null}
          </p>
          {description ? <p className="mt-1 text-xs leading-relaxed text-muted">{description}</p> : null}
        </div>
      </div>
      <Switch
        checked={checked}
        onCheckedChange={onCheckedChange}
        disabled={disabled}
        aria-label={title}
        className="mt-0.5"
      />
    </div>
  );
}