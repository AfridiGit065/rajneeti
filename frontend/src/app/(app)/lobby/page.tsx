"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { DoorOpen, Plus } from "@/components/ui/icons";
import { LobbyHeader, RoomList } from "@/components/lobby";
import { RoomService } from "@/services/room-service";
import type { RoomSummary } from "@/types/room";

export default function LobbyPage() {
  const [rooms, setRooms] = useState<RoomSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchRooms = useCallback(async () => RoomService.listRooms(), []);

  useEffect(() => {
    let cancelled = false;
    fetchRooms().then((result) => {
      if (cancelled) return;
      if (result.ok) {
        setRooms(result.data);
      } else {
        setError(result.error.message);
      }
      setLoading(false);
    });
    return () => {
      cancelled = true;
    };
  }, [fetchRooms]);

  async function retry() {
    setLoading(true);
    setError(null);
    const result = await fetchRooms();
    if (result.ok) {
      setRooms(result.data);
    } else {
      setError(result.error.message);
    }
    setLoading(false);
  }

  return (
    <div className="space-y-6">
      <LobbyHeader />

      <section className="flex flex-col items-start gap-4 rounded-2xl border border-gold-500/25 bg-surface px-5 py-6 panel-emboss sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.25em] text-gold-400">
            Game Lobby
          </p>
          <h1 className="mt-1 text-2xl font-bold text-ivory">Lobby</h1>
          <p className="mt-1 text-sm text-muted">
            Join an open room or create your own to start playing.
          </p>
        </div>
        <div className="flex w-full flex-col gap-3 sm:w-auto sm:flex-row">
          <Link href="/rooms/create" className="flex-1 sm:flex-none">
            <Button variant="premium" size="lg" fullWidth className="sm:w-auto">
              <Plus className="size-5" aria-hidden />
              Create Room
            </Button>
          </Link>
          <Link href="/rooms/join" className="flex-1 sm:flex-none">
            <Button variant="outline" size="lg" fullWidth className="sm:w-auto">
              <DoorOpen className="size-5" aria-hidden />
              Join Room
            </Button>
          </Link>
        </div>
      </section>

      <RoomList rooms={rooms} loading={loading} error={error} onRetry={retry} />
    </div>
  );
}