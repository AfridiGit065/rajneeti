"use client";

import { Bell } from "@/components/ui/icons";
import { SettingsSection } from "./settings-section";
import { SettingToggle } from "./setting-toggle";
import { useUiStore } from "@/store/ui-store";

export function NotificationSection() {
  const settings = useUiStore((s) => s.settings);
  const updateSettings = useUiStore((s) => s.updateSettings);

  return (
    <SettingsSection
      icon={<Bell className="size-4.5" aria-hidden />}
      title="নোটিফিকেশন"
      titleEn="Notifications"
      description="খেলা সংক্রান্ত খবর।"
    >
      <SettingToggle
        icon={<Bell className="size-4" aria-hidden />}
        title="গেম নোটিফিকেশন"
        titleEn="Game notifications"
        description="তোমার পালা, চ্যালেঞ্জ ও ফলাফলের খবর।"
        checked={settings.notificationsEnabled}
        onCheckedChange={(v) => updateSettings({ notificationsEnabled: v })}
      />
    </SettingsSection>
  );
}