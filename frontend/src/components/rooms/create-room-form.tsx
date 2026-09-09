"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import type { FormEvent } from "react";

import { RoomService } from "@/services/room-service";
import { useRoomStore } from "@/store/room-store";
import { useToast } from "@/hooks/use-toast";
import { RULES } from "@/lib/game/rules";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import {
  AlertTriangle,
  DoorOpen,
  Globe,
  Lock,
  Sparkles,
} from "@/components/ui/icons";

const NAME_MAX = 24;

type Visibility = "public" | "private";

const PLAYER_OPTIONS = Array.from(
  { length: RULES.maxPlayers - RULES.minPlayers + 1 },
  (_, i) => {
    const value = String(RULES.minPlayers + i);
    return { value, label: `${value} জন` };
  },
);

export function CreateRoomForm() {
  const router = useRouter();
  const setActiveRoom = useRoomStore((s) => s.setActiveRoom);
  const { info } = useToast();

  const [name, setName] = useState("");
  const [maxPlayers, setMaxPlayers] = useState("4");
  const [visibility, setVisibility] = useState<Visibility>("public");
  const [nameError, setNameError] = useState<string | undefined>(undefined);
  const [submitting, setSubmitting] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setServerError(null);

    if (name.trim().length > NAME_MAX) {
      setNameError(`রুমের নাম সর্বোচ্চ ${NAME_MAX} অক্ষরের হতে পারে।`);
      return;
    }
    setNameError(undefined);

    setSubmitting(true);
    const result = await RoomService.createRoom({
      name: name.trim(),
      maxPlayers: Number(maxPlayers),
    });

    if (!result.ok) {
      setSubmitting(false);
      setServerError(result.error.message);
      return;
    }

    setActiveRoom(result.data);
    router.push(`/rooms/${result.data.roomId}`);
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="space-y-5">
      {serverError ? (
        <div
          role="alert"
          className="flex items-start gap-2.5 rounded-lg border border-crimson-500/35 bg-crimson-600/15 px-3.5 py-2.5 text-sm text-crimson-200"
        >
          <AlertTriangle className="mt-0.5 size-4 shrink-0" aria-hidden />
          <span>{serverError}</span>
        </div>
      ) : null}

      <Input
        label="রুমের নাম (ঐচ্ছিক)"
        type="text"
        placeholder="যেমন — দেরাজ অ্যাভিনিউ"
        maxLength={NAME_MAX}
        spellCheck={false}
        value={name}
        error={nameError}
        hint={`${name.length}/${NAME_MAX}`}
        disabled={submitting}
        onChange={(e) => {
          setName(e.target.value);
          setNameError(undefined);
        }}
      />

      <Select
        label="সর্বোচ্চ খেলোয়াড়"
        options={PLAYER_OPTIONS}
        value={maxPlayers}
        disabled={submitting}
        onChange={(e) => setMaxPlayers(e.target.value)}
      />

      <div>
        <span className="mb-1.5 block text-sm font-medium text-parchment-300">
          রুমের ধরন
        </span>
        <div className="flex items-center gap-3">
          <div className="grid flex-1 grid-cols-2 gap-1 rounded-lg border border-deep-700/70 bg-deep-900/60 p-1">
            {(
              [
                { value: "public", label: "পাবলিক", icon: <Globe className="size-4" aria-hidden /> },
                { value: "private", label: "প্রাইভেট", icon: <Lock className="size-4" aria-hidden /> },
              ] as const
            ).map((opt) => (
              <button
                key={opt.value}
                type="button"
                disabled={submitting}
                onClick={() => {
                  setVisibility(opt.value);
                  info("রুমের ধরন", "পাবলিক/প্রাইভেট সুবিধা শীঘ্রই আসছে।");
                }}
                className={
                  visibility === opt.value
                    ? "flex items-center justify-center gap-2 rounded-md bg-gold-500/15 py-2 text-sm font-medium text-gold-300"
                    : "flex items-center justify-center gap-2 rounded-md py-2 text-sm font-medium text-muted transition-colors hover:text-ivory"
                }
              >
                {opt.icon}
                {opt.label}
              </button>
            ))}
          </div>
          <Badge tone="neutral">শীঘ্রই</Badge>
        </div>
      </div>

      <Button
        type="submit"
        variant="premium"
        size="lg"
        fullWidth
        loading={submitting}
      >
        <DoorOpen className="size-4" aria-hidden />
        ঘর তৈরি করুন
      </Button>

      <p className="flex items-center justify-center gap-1.5 text-xs text-muted">
        <Sparkles className="size-3.5 text-gold-400" aria-hidden />
        তৈরি হলেই একটি মক রুম কোড পাওয়া যাবে।
      </p>
    </form>
  );
}