"use client";

import type { ReactNode } from "react";
import { Card } from "@/components/ui/card";

interface SettingsSectionProps {
  icon: ReactNode;
  title: string;
  titleEn?: string;
  description?: string;
  children: ReactNode;
  className?: string;
}

/** Grouped settings panel: header + stacked rows. */
export function SettingsSection({
  icon,
  title,
  titleEn,
  description,
  children,
  className,
}: SettingsSectionProps) {
  return (
    <Card className={className} padded={false}>
      <div className="border-b border-forest-500/15 p-6 pb-4">
        <div className="flex items-center gap-3">
          <span className="flex size-9 items-center justify-center rounded-lg border border-gold-500/25 bg-gold-500/10 text-gold-400">
            {icon}
          </span>
          <div>
            <h2 className="font-bengali text-lg font-bold text-ivory">{title}</h2>
            {titleEn ? (
              <p className="text-[11px] uppercase tracking-[0.18em] text-muted">
                {titleEn}
              </p>
            ) : null}
          </div>
        </div>
        {description ? <p className="mt-3 text-sm text-muted">{description}</p> : null}
      </div>
      <div className="divide-y divide-forest-500/10">{children}</div>
    </Card>
  );
}