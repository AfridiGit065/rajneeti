"use client";

import { Palette, Sun, Moon, Sparkles } from "@/components/ui/icons";
import { cn } from "@/lib/cn";
import { SettingsSection } from "./settings-section";
import { SettingToggle } from "./setting-toggle";
import { useUiStore } from "@/store/ui-store";

const THEMES = [
  {
    id: "emerald" as const,
    label: "এমেরাল্ড",
    labelEn: "Emerald",
    icon: Sun,
    swatch: "bg-forest-500",
  },
  {
    id: "midnight" as const,
    label: "মিডনাইট",
    labelEn: "Midnight",
    icon: Moon,
    swatch: "bg-[#1d3866]",
  },
];

export function AppearanceSection() {
  const settings = useUiStore((s) => s.settings);
  const updateSettings = useUiStore((s) => s.updateSettings);

  return (
    <SettingsSection
      icon={<Palette className="size-4.5" aria-hidden />}
      title="চেহারা"
      titleEn="Appearance"
      description="থিম আর অ্যানিমেশন পছন্দ — এই ডিভাইসে সংরক্ষিত থাকবে।"
    >
      <div className="p-6">
        <p className="mb-3 text-sm font-semibold text-ivory">
          থিম <span className="ml-1 text-xs font-normal text-muted">Theme</span>
        </p>
        <div className="grid grid-cols-2 gap-3">
          {THEMES.map((theme) => {
            const active = settings.theme === theme.id;
            return (
              <button
                key={theme.id}
                type="button"
                aria-pressed={active}
                onClick={() => updateSettings({ theme: theme.id })}
                className={cn(
                  "flex items-center gap-3 rounded-xl border p-3.5 transition-all duration-150",
                  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold-400",
                  active
                    ? "border-gold-400/60 bg-gold-500/10"
                    : "border-forest-500/25 bg-deep-800/40 hover:border-forest-400/50",
                )}
              >
                <span
                  className={cn(
                    "flex size-8 shrink-0 items-center justify-center rounded-lg border",
                    active ? "border-gold-400/50 text-gold-300" : "border-forest-500/25 text-muted",
                  )}
                >
                  <theme.icon className="size-4" aria-hidden />
                </span>
                <span className="min-w-0 text-left">
                  <span className="block text-sm font-semibold text-ivory">{theme.label}</span>
                  <span className="block text-[10px] uppercase tracking-wider text-muted">
                    {theme.labelEn}
                  </span>
                </span>
              </button>
            );
          })}
        </div>
      </div>

      <SettingToggle
        icon={<Sparkles className="size-4" aria-hidden />}
        title="হালকা অ্যানিমেশন"
        titleEn="Reduced motion"
        description="অ্যানিমেশন ও ট্রানজিশন কমিয়ে দাও — ব্যাটারি ও ফোকাস বাঁচায়।"
        checked={settings.reducedMotion}
        onCheckedChange={(v) => updateSettings({ reducedMotion: v })}
      />
    </SettingsSection>
  );
}