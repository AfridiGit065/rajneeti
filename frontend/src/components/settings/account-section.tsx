"use client";

import { UserIcon, Mail, AtSign, Lock, PenLine } from "@/components/ui/icons";
import { SettingsSection } from "./settings-section";
import { useAuthStore } from "@/store/auth-store";
import { AuthService } from "@/services/auth-service";
import { useUiStore } from "@/store/ui-store";
import { Button } from "@/components/ui/button";

export function AccountSection() {
  const user = useAuthStore((s) => s.user);
  const pushToast = useUiStore((s) => s.pushToast);

  const username = user?.username ?? "guest";
  const email = AuthService.demoCredentials.email;

  const comingSoon = (label: string) =>
    pushToast({ kind: "info", title: label, message: "সম্পাদনা শীঘ্রই আসছে।" });

  return (
    <SettingsSection
      icon={<UserIcon className="size-4.5" aria-hidden />}
      title="অ্যাকাউন্ট"
      titleEn="Account"
      description="তোমার অ্যাকাউন্ট তথ্য।"
    >
      <div className="flex items-center justify-between gap-4 p-6">
        <div className="flex min-w-0 items-center gap-3">
          <span className="flex size-8 shrink-0 items-center justify-center rounded-md border border-forest-500/20 bg-deep-800/60 text-muted">
            <AtSign className="size-4" aria-hidden />
          </span>
          <div className="min-w-0">
            <p className="text-sm font-semibold text-ivory">
              ইউজারনেম <span className="ml-1 text-xs font-normal text-muted">Username</span>
            </p>
            <p className="truncate text-sm text-parchment-300">{username}</p>
          </div>
        </div>
        <Button variant="ghost" size="sm" onClick={() => comingSoon("ইউজারনেম")}>
          <PenLine className="size-3.5" aria-hidden />
          সম্পাদন
        </Button>
      </div>

      <div className="flex items-center justify-between gap-4 p-6">
        <div className="flex min-w-0 items-center gap-3">
          <span className="flex size-8 shrink-0 items-center justify-center rounded-md border border-forest-500/20 bg-deep-800/60 text-muted">
            <Mail className="size-4" aria-hidden />
          </span>
          <div className="min-w-0">
            <p className="text-sm font-semibold text-ivory">
              ইমেইল <span className="ml-1 text-xs font-normal text-muted">Email</span>
            </p>
            <p className="flex items-center gap-1.5 truncate text-sm text-parchment-300">
              {email}
              <Lock className="size-3 shrink-0 text-muted" aria-hidden />
            </p>
          </div>
        </div>
        <span className="shrink-0 rounded-md bg-deep-750 px-2 py-1 text-[10px] font-semibold text-muted">
          ডেমো
        </span>
      </div>
    </SettingsSection>
  );
}