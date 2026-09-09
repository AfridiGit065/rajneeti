import { PageHeader } from "@/components/layout/page-header";
import {
  AccountSection,
  AppearanceSection,
  AudioSection,
  GameplaySection,
  NotificationSection,
  PrivacySection,
  DangerZoneSection,
} from "@/components/settings";

export default function SettingsPage() {
  return (
    <div>
      <PageHeader
        eyebrow="Preferences"
        title="Settings"
        subtitle="Account, appearance, audio, gameplay and privacy — all in one place."
      />
      <div className="grid items-start gap-6 lg:grid-cols-2">
        <AccountSection />
        <AppearanceSection />
        <AudioSection />
        <GameplaySection />
        <NotificationSection />
        <PrivacySection />
        <div className="lg:col-span-2">
          <DangerZoneSection />
        </div>
      </div>
    </div>
  );
}