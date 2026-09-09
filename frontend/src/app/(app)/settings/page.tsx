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
        eyebrow="পছন্দসমূহ"
        title="সেটিংস"
        subtitle="অ্যাকাউন্ট, চেহারা, অডিও, খেলা ও গোপনীয়তা — সব এক জায়গায়।"
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