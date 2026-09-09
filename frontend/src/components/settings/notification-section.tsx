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
      title="Notifications"
      description="Game announcements and turn alerts."
    >
      <SettingToggle
        icon={<Bell className="size-4" aria-hidden />}
        title="Game Notifications"
        description="Alerts for your turns, challenges, and match resolutions."
        checked={settings.notificationsEnabled}
        onCheckedChange={(v) => updateSettings({ notificationsEnabled: v })}
      />
    </SettingsSection>
  );
}