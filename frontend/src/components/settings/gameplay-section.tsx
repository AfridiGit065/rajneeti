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
      title="Gameplay"
      description="Action confirmations and warnings."
    >
      <SettingToggle
        icon={<Crown className="size-4" aria-hidden />}
        title="Confirm before Coup"
        description="7 coins action — confirm target player to avoid misclicks."
        checked={settings.confirmCoup}
        onCheckedChange={(v) => updateSettings({ confirmCoup: v })}
      />
      <SettingToggle
        icon={<Skull className="size-4" aria-hidden />}
        title="Confirm before Assassination"
        description="3 coins action — confirm target player before launching attempt."
        checked={settings.confirmAssassination}
        onCheckedChange={(v) => updateSettings({ confirmAssassination: v })}
      />
    </SettingsSection>
  );
}