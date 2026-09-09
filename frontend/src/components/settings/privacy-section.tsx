"use client";

import { ShieldCheck, Download, EyeOff } from "@/components/ui/icons";
import { SettingsSection } from "./settings-section";
import { useUiStore } from "@/store/ui-store";

const PLACEHOLDER_ROWS = [
  {
    key: "download",
    icon: Download,
    title: "ডেটা ডাউনলোড",
    titleEn: "Download my data",
  },
  {
    key: "visibility",
    icon: EyeOff,
    title: "গোপনীয়তা নিয়ন্ত্রণ",
    titleEn: "Privacy controls",
  },
];

export function PrivacySection() {
  const pushToast = useUiStore((s) => s.pushToast);

  return (
    <SettingsSection
      icon={<ShieldCheck className="size-4.5" aria-hidden />}
      title="গোপনীয়তা"
      titleEn="Privacy"
      description="এই ফিচারগুলো শীঘ্রই আসছে।"
    >
      {PLACEHOLDER_ROWS.map((row) => (
        <button
          key={row.key}
          type="button"
          onClick={() =>
            pushToast({
              kind: "info",
              title: row.title,
              message: "এই ফিচারটি শীঘ্রই আসছে।",
            })
          }
          className="flex w-full items-center gap-3 p-6 text-left transition-colors hover:bg-deep-800/40"
        >
          <span className="flex size-8 shrink-0 items-center justify-center rounded-md border border-forest-500/20 bg-deep-800/60 text-muted">
            <row.icon className="size-4" aria-hidden />
          </span>
          <span className="flex-1">
            <span className="block text-sm font-semibold text-ivory">
              {row.title}{" "}
              <span className="ml-1 text-xs font-normal text-muted">{row.titleEn}</span>
            </span>
          </span>
          <span className="rounded-md bg-deep-750 px-2 py-1 text-[10px] font-semibold tracking-wide text-gold-400">
            শীঘ্রই
          </span>
        </button>
      ))}
    </SettingsSection>
  );
}