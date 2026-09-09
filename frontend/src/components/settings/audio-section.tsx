"use client";

import { Volume2, Music } from "@/components/ui/icons";
import { SettingsSection } from "./settings-section";
import { SettingToggle } from "./setting-toggle";
import { useUiStore } from "@/store/ui-store";

export function AudioSection() {
  const settings = useUiStore((s) => s.settings);
  const updateSettings = useUiStore((s) => s.updateSettings);

  return (
    <SettingsSection
      icon={<Volume2 className="size-4.5" aria-hidden />}
      title="অডিও"
      titleEn="Audio"
      description="খেলার শব্দ ও ব্যাকগ্রাউন্ড মিউজিক।"
    >
      <SettingToggle
        icon={<Volume2 className="size-4" aria-hidden />}
        title="সাউন্ড ইফেক্ট"
        titleEn="Sound effects"
        description="কয়েন, কার্ড ও বাটনের শব্দ।"
        checked={settings.soundEnabled}
        onCheckedChange={(v) => updateSettings({ soundEnabled: v })}
      />
      <SettingToggle
        icon={<Music className="size-4" aria-hidden />}
        title="মিউজিক"
        titleEn="Music"
        description="লবি ও খেলার ব্যাকগ্রাউন্ড মিউজিক।"
        checked={settings.musicEnabled}
        onCheckedChange={(v) => updateSettings({ musicEnabled: v })}
      />
    </SettingsSection>
  );
}