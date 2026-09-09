import Link from "next/link";
import { Badge, type BadgeTone } from "@/components/ui/badge";
import { Button, type ButtonVariant } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { RoomCode } from "@/components/game/room-code";
import { Users } from "@/components/ui/icons";
import { PlayerMiniProfile } from "./player-mini-profile";
import type { RoomStatus, RoomSummary } from "@/types/room";

const ROOM_STATUS: Record<RoomStatus, { label: string; tone: BadgeTone }> = {
  WAITING: { label: "অপেক্ষমাণ", tone: "emerald" },
  IN_PROGRESS: { label: "চলছে", tone: "gold" },
  FINISHED: { label: "শেষ", tone: "neutral" },
};

export function RoomCard({ room }: { room: RoomSummary }) {
  const status = ROOM_STATUS[room.status];
  const filled = room.players.length >= room.maxPlayers;

  const joinLabel =
    room.status === "IN_PROGRESS"
      ? "খেলা চলছে"
      : room.status === "FINISHED"
        ? "ওভারভিউ"
        : filled
          ? "রুম পূর্ণ"
          : "যোগ দিন";
  const joinVariant: ButtonVariant =
    room.status === "IN_PROGRESS" || filled
      ? "secondary"
      : room.status === "FINISHED"
        ? "outline"
        : "premium";

  return (
    <Card interactive className="flex flex-col gap-4 animate-fade-up">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <RoomCode code={room.roomCode} size="sm" />
          <h3 className="mt-2 truncate font-bengali text-lg font-semibold text-ivory">
            {room.name}
          </h3>
        </div>
        <Badge tone={status.tone}>{status.label}</Badge>
      </div>

      <div className="flex items-center justify-between gap-3">
        <div className="min-w-0">
          <p className="text-[0.68rem] font-semibold uppercase tracking-wider text-muted">
            হোস্ট
          </p>
          <PlayerMiniProfile user={room.host} size="sm" showRating={false} />
        </div>
        <div
          className="flex shrink-0 items-center gap-1.5 rounded-lg border border-forest-500/20 bg-deep-800/70 px-2.5 py-1.5 text-sm font-medium text-ivory"
          title={`${room.players.length} জন / সর্বোচ্চ ${room.maxPlayers} জন`}
        >
          <Users className="size-4 text-forest-300" aria-hidden />
          {room.players.length}/{room.maxPlayers}
        </div>
      </div>

      <Link href={`/rooms/${room.roomId}`} className="mt-auto block">
        <Button variant={joinVariant} fullWidth>
          {joinLabel}
        </Button>
      </Link>
    </Card>
  );
}