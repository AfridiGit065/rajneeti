"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { LogOut, Trash2, AlertTriangle } from "@/components/ui/icons";
import { SettingsSection } from "./settings-section";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Button } from "@/components/ui/button";
import { useAuthStore } from "@/store/auth-store";
import { useUiStore } from "@/store/ui-store";

export function DangerZoneSection() {
  const router = useRouter();
  const logout = useAuthStore((s) => s.logout);
  const pushToast = useUiStore((s) => s.pushToast);

  const [confirm, setConfirm] = useState<"logout" | "delete" | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleLogout = async () => {
    setSubmitting(true);
    try {
      await logout();
      pushToast({ kind: "success", title: "Logged out", message: "See you next time!" });
      router.replace("/");
    } finally {
      setSubmitting(false);
      setConfirm(null);
    }
  };

  return (
    <SettingsSection
      icon={<AlertTriangle className="size-4.5" aria-hidden />}
      title="Danger Zone"
      description="Use with caution — these actions cannot be easily undone."
    >
      <div className="flex flex-wrap items-center justify-between gap-4 p-6">
        <div className="flex min-w-0 items-center gap-3">
          <span className="flex size-8 shrink-0 items-center justify-center rounded-md border border-forest-500/20 bg-deep-800/60 text-muted">
            <LogOut className="size-4" aria-hidden />
          </span>
          <div className="min-w-0">
            <p className="text-sm font-semibold text-ivory">
              Log Out
            </p>
            <p className="text-xs text-muted">Sign out of your account on this device.</p>
          </div>
        </div>
        <Button variant="secondary" size="sm" onClick={() => setConfirm("logout")}>
          Log Out
        </Button>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-4 rounded-b-2xl p-6">
        <div className="flex min-w-0 items-center gap-3">
          <span className="flex size-8 shrink-0 items-center justify-center rounded-md border border-crimson-500/25 bg-crimson-600/15 text-crimson-300">
            <Trash2 className="size-4" aria-hidden />
          </span>
          <div className="min-w-0">
            <p className="text-sm font-semibold text-ivory">
              Delete Account
            </p>
            <p className="text-xs text-muted">Permanently delete all profile and match data.</p>
          </div>
        </div>
        <Button variant="danger" size="sm" onClick={() => setConfirm("delete")}>
          Delete
        </Button>
      </div>

      <ConfirmDialog
        open={confirm === "logout"}
        onClose={() => setConfirm(null)}
        onConfirm={handleLogout}
        title="Log Out?"
        description="Are you sure you want to sign out? You will need your credentials to log back in."
        confirmLabel="Log Out"
        loading={submitting}
      />

      <ConfirmDialog
        open={confirm === "delete"}
        onClose={() => setConfirm(null)}
        onConfirm={() => {
          setConfirm(null);
          pushToast({
            kind: "info",
            title: "Coming Soon",
            message: "Account deletion is not yet supported in this version.",
          });
        }}
        title="Delete Account?"
        description="This feature is under development and will be available soon."
        confirmLabel="Understood"
      />
    </SettingsSection>
  );
}