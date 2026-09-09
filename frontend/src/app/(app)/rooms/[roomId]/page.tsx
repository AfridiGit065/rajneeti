"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { RoomService } from "@/services/room-service";
import { useRoomStore } from "@/store/room-store";
import { useAuthStore } from "@/store/auth-store";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { LoadingState } from "@/components/ui/loading-state";
import { EmptyState } from "@/components/ui/empty-state";
import { ErrorState } from "@/components/ui/error-state";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import {
  RoomCodeDisplay,
  Seat,
  RoomPlayerCard,
} from "@/components/rooms";
import {
  AlertTriangle,
  ArrowLeft,
  CheckCheck,
  Crown,
  DoorOpen,
  LogOut,
  Swords,
} from "@/components/ui/icons";

type ViewState = "loading" | "ready" | "notfound" | "error";

export default function RoomDetailPage() {
  const params = useParams<{ roomId: string }>();
  const roomId = params.roomId;
  const router = useRouter();

  const activeRoom = useRoomStore((s) => s.activeRoom);
  const setActiveRoom = useRoomStore((s) => s.setActiveRoom);
  const currentUser = useAuthStore((s) => s.user);

  const [view, setView] = useState<ViewState>("loading");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [prevRoomId, setPrevRoomId] = useState(roomId);

  // Adjust state during render when navigating between rooms (React-sanctioned pattern).
  if (roomId !== prevRoomId) {
    setPrevRoomId(roomId);
    setView("loading");
    setErrorMessage(null);
  }
  const [joinLoading, setJoinLoading] = useState(false);
  const [leaveLoading, setLeaveLoading] = useState(false);
  const [leaveOpen, setLeaveOpen] = useState(false);

  useEffect(() => {
    let cancelled = false;
    RoomService.getRoomById(roomId).then((result) => {
      if (cancelled) return;
      if (!result.ok) {
        setView(result.error.status === 404 ? "notfound" : "error");
        setErrorMessage(result.error.message);
        return;
      }
      setActiveRoom(result.data);
      setView("ready");
    });
    return () => {
      cancelled = true;
    };
  }, [roomId, reloadKey, setActiveRoom]);

  const room = activeRoom;
  const me = room?.players.find((p) => p.user.id === currentUser?.id) ?? null;
  const isHost = me?.isHost ?? false;
  const playerCount = room?.players.length ?? 0;

  async function joinRoom() {
    if (!room) return;
    setJoinLoading(true);
    const result = await RoomService.joinRoom({ roomCode: room.roomCode });
    setJoinLoading(false);
    if (result.ok) setActiveRoom(result.data);
  }

  async function confirmLeave() {
    if (!room) return;
    setLeaveLoading(true);
    const result = await RoomService.leaveRoom(room.roomId);
    setLeaveLoading(false);
    if (!result.ok) {
      setErrorMessage(result.error.message);
      return;
    }
    setActiveRoom(null);
    router.replace("/lobby");
  }

  if (view === "loading") {
    return (
      <div className="rounded-2xl border border-forest-500/25 bg-surface panel-emboss p-10">
        <LoadingState label="রুম লোড হচ্ছে…" />
      </div>
    );
  }

  if (view === "notfound" || !room) {
    return (
      <EmptyState
        icon={<AlertTriangle className="size-6" aria-hidden />}
        title="রুম পাওয়া যায়নি"
        description="এই রুমটি হয়তো বন্ধ হয়ে গেছে, বা কোড ভুল।"
        action={
          <Link href="/lobby">
            <Button variant="premium">
              লবিতে ফিরে যাও
            </Button>
          </Link>
        }
      />
    );
  }

  if (view === "error") {
    return (
      <ErrorState
        title="রুম লোড করা যায়নি"
        message={errorMessage ?? "অনেক সময় পরে আবার চেষ্টা করো।"}
        action={
          <Button
            variant="primary"
            onClick={() => {
              setView("loading");
              setReloadKey((k) => k + 1);
            }}
          >
            আবার চেষ্টা করো
          </Button>
        }
      />
    );
  }

  const seats = Array.from({ length: room.maxPlayers }, (_, i) => i);
  const playersBySeat = new Map(
    room.players
      .slice()
      .sort((a, b) => a.seatIndex - b.seatIndex)
      .map((p) => [p.seatIndex, p]),
  );

  return (
    <div className="animate-fade-up">
      <Link
        href="/lobby"
        className="mb-6 inline-flex items-center gap-1.5 text-sm text-muted transition-colors hover:text-gold-300"
      >
        <ArrowLeft className="size-4" aria-hidden />
        Back to Lobby
      </Link>

      {room.status !== "WAITING" ? (
        <div className="rounded-2xl border border-forest-500/25 bg-surface panel-emboss p-10 text-center">
          <Badge tone="crimson">{room.status === "IN_PROGRESS" ? "In Progress" : "Finished"}</Badge>
          <p className="mt-4 text-sm text-muted">
            This room is no longer accepting players.
          </p>
        </div>
      ) : (
        <>
          <div className="mb-8 flex flex-col items-center justify-between gap-6 sm:flex-row sm:items-start">
            <RoomCodeDisplay
              code={room.roomCode}
              playerCount={playerCount}
              maxPlayers={room.maxPlayers}
            />
            <div className="flex flex-col items-center gap-2 sm:items-end">
              {room.name ? (
                <p className="text-xl font-semibold text-ivory">{room.name}</p>
              ) : null}
              <Badge
                tone={room.status === "WAITING" ? "emerald" : "crimson"}
              >
                {room.status === "WAITING" ? "Waiting" : room.status}
              </Badge>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setLeaveOpen(true)}
              >
                <LogOut className="size-3.5" aria-hidden />
                Leave Room
              </Button>
            </div>
          </div>

          {me ? (
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {seats.map((seatIndex, index) => {
                const player = playersBySeat.get(seatIndex);
                return (
                  <Seat
                    key={seatIndex}
                    seatIndex={seatIndex}
                    empty={!player}
                    delayMs={index * 60}
                  >
                    {player ? (
                      <RoomPlayerCard player={player} isMe={player.user.id === currentUser?.id} />
                    ) : null}
                  </Seat>
                );
              })}
            </div>
          ) : (
            <div className="rounded-2xl border border-dashed border-forest-500/30 bg-deep-900/40 p-10 text-center">
              <p className="text-lg font-semibold text-ivory">
                You are not in this room yet
              </p>
              <p className="mt-1 text-sm text-muted">
                Click the button below to join.
              </p>
              <Button
                className="mt-5"
                variant="premium"
                loading={joinLoading}
                onClick={joinRoom}
              >
                <DoorOpen className="size-4" aria-hidden />
                Join Room
              </Button>
            </div>
          )}

          {me ? (
            <div className="mt-8 rounded-2xl border border-forest-500/25 bg-surface panel-emboss p-6">
              <div className="flex flex-col items-stretch justify-between gap-4 sm:flex-row sm:items-center">
                <div className="flex-1">
                  <p className="text-sm font-semibold text-ivory">
                    {isHost ? (
                      <span className="inline-flex items-center gap-1.5">
                        <Crown className="size-4 text-gold-400" aria-hidden />
                        You are the Host
                      </span>
                    ) : (
                      "Your Ready Status"
                    )}
                  </p>
                  <p className="mt-1 text-xs text-muted">Game readiness will be available in a future gameplay update.</p>
                </div>
                <div className="flex flex-col gap-3 sm:w-64">
                  <Button variant="outline" size="lg" fullWidth disabled>
                    Ready
                  </Button>
                  {isHost ? (
                    <Button variant="premium" size="lg" fullWidth disabled>
                      <Swords className="size-4" aria-hidden />
                      Start Game
                    </Button>
                  ) : null}
                </div>
              </div>

              <p className="mt-4 flex items-start gap-2 text-xs text-muted">
                <CheckCheck className="mt-0.5 size-4 shrink-0 text-gold-400" aria-hidden />
                Ready and start controls will be enabled with the gameplay module.
              </p>
            </div>
          ) : null}
        </>
      )}


      <ConfirmDialog
        open={leaveOpen}
        onClose={() => setLeaveOpen(false)}
        onConfirm={confirmLeave}
        title="Leave this room?"
        description={
          room.status === "WAITING" && isHost
            ? "You are the host. If players remain, the next player becomes host. Are you sure?"
            : "You will leave the room. You will need the code to rejoin."
        }
        confirmLabel="Leave"
        variant="danger"
        loading={leaveLoading}
      />
    </div>
  );
}
