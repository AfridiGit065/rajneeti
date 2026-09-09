"use client";

import { Swords, Crown, Skull } from "@/components/ui/icons";
import { SettingsSection } from "./settings-section";
import { SettingToggle } from "./setting-toggle";
import { useUiStore } from "@/store/ui-store";

export function GameplaySection() {
  const settings = useUiStore((s) => s.settings);
  const updateSettings = useUiStore((s) => s.updateSettings);

  return (
    <SettingsSection
      icon={<Swords className="size-4.5" aria-hidden />}
      title="খেলা"
      titleEn="Gameplay"
      description="গুরুত্বপূর্ণ অ্যাকশনে নিশ্চিতকরণ।"
    >
      <SettingToggle
        icon={<Crown className="size-4" aria-hidden />}
        title="ক্ষমতা দখলের আগে নিশ্চিতকরণ"
        titleEn="Confirm before Coup"
        description="৭ কয়েনের কুপ — ভুল টার্গেট এড়াতে জিজ্ঞেস করবে।"
        checked={settings.confirmCoup}
        onCheckedChange={(v) => updateSettings({ confirmCoup: v })}
      />
      <SettingToggle
        icon={<Skull className="size-4" aria-hidden />}
        title="সরিয়ে দেওয়ার আগে নিশ্চিতকরণ"
        titleEn="Confirm before Assassination"
        description="৩ কয়েনের প্রচেষ্টা — টার্গেট নিশ্চিত করে নাও।"
        checked={settings.confirmAssassination}
        onCheckedChange={(v) => updateSettings({ confirmAssassination: v })}
      />
    </SettingsSection>
  );
}