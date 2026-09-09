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
      title="Audio"
      description="Sound effects and background music."
    >
      <SettingToggle
        icon={<Volume2 className="size-4" aria-hidden />}
        title="Sound Effects"
        description="Coins, cards, and UI button sound effects."
        checked={settings.soundEnabled}
        onCheckedChange={(v) => updateSettings({ soundEnabled: v })}
      />
      <SettingToggle
        icon={<Music className="size-4" aria-hidden />}
        title="Background Music"
        description="Lobby and match atmospheric music."
        checked={settings.musicEnabled}
        onCheckedChange={(v) => updateSettings({ musicEnabled: v })}
      />
    </SettingsSection>
  );
}