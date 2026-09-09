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
      pushToast({ kind: "success", title: "লগআউট হয়েছে", message: "আবার দেখা হবে!" });
      router.replace("/");
    } finally {
      setSubmitting(false);
      setConfirm(null);
    }
  };

  return (
    <SettingsSection
      icon={<AlertTriangle className="size-4.5" aria-hidden />}
      title="ডেঞ্জার জোন"
      titleEn="Danger Zone"
      description="সতর্কভাবে ব্যবহার করো — এই কাজগুলো সহজে ফেরানো যায় না।"
    >
      <div className="flex flex-wrap items-center justify-between gap-4 p-6">
        <div className="flex min-w-0 items-center gap-3">
          <span className="flex size-8 shrink-0 items-center justify-center rounded-md border border-forest-500/20 bg-deep-800/60 text-muted">
            <LogOut className="size-4" aria-hidden />
          </span>
          <div className="min-w-0">
            <p className="text-sm font-semibold text-ivory">
              লগআউট <span className="ml-1 text-xs font-normal text-muted">Log out</span>
            </p>
            <p className="text-xs text-muted">এই ডিভাইস থেকে সাইন আউট করো।</p>
          </div>
        </div>
        <Button variant="secondary" size="sm" onClick={() => setConfirm("logout")}>
          লগআউট
        </Button>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-4 rounded-b-2xl p-6">
        <div className="flex min-w-0 items-center gap-3">
          <span className="flex size-8 shrink-0 items-center justify-center rounded-md border border-crimson-500/25 bg-crimson-600/15 text-crimson-300">
            <Trash2 className="size-4" aria-hidden />
          </span>
          <div className="min-w-0">
            <p className="text-sm font-semibold text-ivory">
              অ্যাকাউন্ট মুছে ফেলো{" "}
              <span className="ml-1 text-xs font-normal text-muted">Delete account</span>
            </p>
            <p className="text-xs text-muted">স্থায়ীভাবে সব তথ্য মুছে যাবে।</p>
          </div>
        </div>
        <Button variant="danger" size="sm" onClick={() => setConfirm("delete")}>
          মুছে ফেলো
        </Button>
      </div>

      <ConfirmDialog
        open={confirm === "logout"}
        onClose={() => setConfirm(null)}
        onConfirm={handleLogout}
        title="লগআউট?"
        description="তুমি এই ডিভাইস থেকে সাইন আউট হতে যাচ্ছ। আবার ঢুকতে পুনরায় লগইন করতে হবে।"
        confirmLabel="লগআউট"
        loading={submitting}
      />

      <ConfirmDialog
        open={confirm === "delete"}
        onClose={() => setConfirm(null)}
        onConfirm={() => {
          setConfirm(null);
          pushToast({
            kind: "info",
            title: "শীঘ্রই আসছে",
            message: "অ্যাকাউন্ট মুছে ফেলার ফিচারটি এখনো চালু হয়নি।",
          });
        }}
        title="অ্যাকাউন্ট মুছে ফেলো?"
        description="এই ফিচারটি এখনো তৈরি হয়নি — শীঘ্রই আসছে।"
        confirmLabel="শীঘ্রই আসছে"
      />
    </SettingsSection>
  );
}