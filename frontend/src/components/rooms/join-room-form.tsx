"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import type { FormEvent } from "react";

import { RoomService } from "@/services/room-service";
import { useRoomStore } from "@/store/room-store";
import { normalizeRoomCode, isValidRoomCode } from "@/lib/room/room-code";
import type { RoomSummary } from "@/types/room";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { LoadingState } from "@/components/ui/loading-state";
import { EmptyState } from "@/components/ui/empty-state";
import {
  AlertTriangle,
  Check,
  DoorOpen,
  KeyRound,
  Users,
} from "@/components/ui/icons";

const RECENT_LIMIT = 3;

export function JoinRoomForm() {
  const router = useRouter();
  const setActiveRoom = useRoomStore((s) => s.setActiveRoom);

  const [code, setCode] = useState("");
  const [codeError, setCodeError] = useState<string | undefined>(undefined);
  const [searching, setSearching] = useState(false);
  const [preview, setPreview] = useState<RoomSummary | null>(null);
  const [joining, setJoining] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);

  const [recent, setRecent] = useState<RoomSummary[]>([]);
  const [loadingRecent, setLoadingRecent] = useState(true);

  useEffect(() => {
    let cancelled = false;
    RoomService.listRooms().then((result) => {
      if (cancelled) return;
      setLoadingRecent(false);
      if (result.ok) {
        setRecent(
          result.data.filter((r) => r.status === "WAITING").slice(0, RECENT_LIMIT),
        );
      }
    });
    return () => {
      cancelled = true;
    };
  }, []);

  async function handleSearch(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setServerError(null);

    const normalized = normalizeRoomCode(code);
    setCode(normalized);

    if (!isValidRoomCode(normalized)) {
      setCodeError("৫–৬ অক্ষরের বৈধ রুম কোড দিন।");
      setPreview(null);
      return;
    }
    setCodeError(undefined);

    setSearching(true);
    const result = await RoomService.findRoomByCode(normalized);
    setSearching(false);

    if (!result.ok) {
      setPreview(null);
      setServerError(result.error.message);
      return;
    }
    setPreview(result.data);
  }

  async function handleJoin() {
    if (!preview) return;
    setJoining(true);
    setServerError(null);
    const result = await RoomService.joinRoom({ roomCode: preview.roomCode });

    if (!result.ok) {
      setJoining(false);
      setServerError(result.error.message);
      return;
    }

    setActiveRoom(result.data);
    router.push(`/rooms/${result.data.roomId}`);
  }

  function selectRecent(room: RoomSummary) {
    setCode(room.roomCode);
    setCodeError(undefined);
    setServerError(null);
    setPreview(room);
  }

  return (
    <div className="space-y-6">
      <Card>
        <form onSubmit={handleSearch} noValidate className="space-y-4">
          <p className="text-xs font-semibold uppercase tracking-[0.25em] text-gold-400">
            রুম কোড
          </p>

          <Input
            type="text"
            inputMode="text"
            autoCapitalize="characters"
            autoComplete="off"
            spellCheck={false}
            placeholder="RAJ123"
            maxLength={6}
            value={code}
            error={codeError}
            disabled={searching || joining}
            className="font-mono text-lg font-semibold uppercase tracking-[0.3em]"
            onChange={(e) => {
              setCode(normalizeRoomCode(e.target.value));
              setCodeError(undefined);
              setServerError(null);
              setPreview(null);
            }}
            leadingIcon={<KeyRound className="size-4" aria-hidden />}
          />

          {serverError ? (
            <div
              role="alert"
              className="flex items-start gap-2.5 rounded-lg border border-crimson-500/35 bg-crimson-600/15 px-3.5 py-2.5 text-sm text-crimson-200"
            >
              <AlertTriangle className="mt-0.5 size-4 shrink-0" aria-hidden />
              <span>{serverError}</span>
            </div>
          ) : null}

          {preview ? (
            <div className="animate-fade-up rounded-xl border border-forest-500/25 bg-deep-900/60 p-4">
              <div className="flex items-center justify-between gap-3">
                <div className="min-w-0">
                  <p className="flex items-center gap-2 text-sm font-semibold text-ivory">
                    <Check className="size-4 text-forest-300" aria-hidden />
                    <span className="truncate">
                      {preview.name || "নামহীন রুম"}
                    </span>
                  </p>
                  <p className="mt-0.5 text-xs text-muted">
                    হোস্ট: {preview.host.displayName} ·{" "}
                    <span className="inline-flex items-center gap-1">
                      <Users className="size-3" aria-hidden />
                      {preview.players.length}/{preview.maxPlayers}
                    </span>
                  </p>
                </div>
                <Badge tone="emerald">WAITING</Badge>
              </div>
            </div>
          ) : null}
        </form>

        <div className="mt-5 border-t border-forest-500/20 pt-5">
          <Button
            type="button"
            variant="premium"
            size="lg"
            fullWidth
            disabled={!preview}
            loading={joining}
            onClick={handleJoin}
          >
            <DoorOpen className="size-4" aria-hidden />
            যোগ দিন
          </Button>
          <p className="mt-3 text-center text-xs text-muted">
            কোড লিখে Enter চাপো — সঠিক হলে রুমের তথ্য দেখাবে।
          </p>
        </div>
      </Card>

      <div>
        <p className="mb-3 text-xs font-semibold uppercase tracking-[0.25em] text-gold-400">
          সাম্প্রতিক রুম
        </p>
        {loadingRecent ? (
          <div className="rounded-2xl border border-forest-500/25 bg-surface panel-emboss p-6">
            <LoadingState label="রুম খোঁজা হচ্ছে…" />
          </div>
        ) : recent.length === 0 ? (
          <div className="rounded-2xl border border-forest-500/25 bg-surface panel-emboss p-6">
            <EmptyState
              icon={<Users className="size-5" aria-hidden />}
              title="এখনো কোনো রুম নেই"
              description="প্রথম রুমটি তৈরি করো, নাকি অন্য কোড চেষ্টা করো।"
            />
          </div>
        ) : (
          <div className="grid gap-3 sm:grid-cols-3">
            {recent.map((room) => (
              <button
                key={room.roomId}
                type="button"
                onClick={() => selectRecent(room)}
                className="rounded-xl border border-forest-500/25 bg-surface panel-emboss p-4 text-left transition-all duration-150 hover:-translate-y-0.5 hover:border-gold-500/40"
              >
                <span className="block font-mono text-sm font-semibold tracking-[0.2em] text-gold-300">
                  {room.roomCode}
                </span>
                <span className="mt-1 block truncate text-sm font-medium text-ivory">
                  {room.name || "নামহীন রুম"}
                </span>
                <span className="mt-1 flex items-center gap-1.5 text-xs text-muted">
                  <Users className="size-3" aria-hidden />
                  হোস্ট: {room.host.displayName} · {room.players.length}/{room.maxPlayers}
                </span>
              </button>
            ))}
          </div>
        )}
      </div>

      <p className="text-center text-sm text-muted">
        নিজের ঘর বানাতে চাও?{" "}
        <Link
          href="/rooms/create"
          className="font-semibold text-gold-400 transition-colors hover:text-gold-300"
        >
          রুম তৈরি করো
        </Link>
      </p>
    </div>
  );
}