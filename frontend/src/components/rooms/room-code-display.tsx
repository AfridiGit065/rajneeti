"use client";

import { RoomCode } from "@/components/game/room-code";
import { Users } from "@/components/ui/icons";

interface RoomCodeDisplayProps {
  code: string;
  playerCount?: number;
  maxPlayers?: number;
}

export function RoomCodeDisplay({ code, playerCount, maxPlayers }: RoomCodeDisplayProps) {
  return (
    <div className="text-center">
      <p className="mb-2 text-xs font-semibold uppercase tracking-[0.25em] text-gold-400">
        রুম কোড
      </p>
      <RoomCode code={code} size="lg" />
      {playerCount !== undefined && maxPlayers !== undefined ? (
        <p className="mt-3 inline-flex items-center gap-1.5 text-xs text-muted">
          <Users className="size-3.5" aria-hidden />
          {playerCount}/{maxPlayers} খেলোয়াড়
        </p>
      ) : null}
    </div>
  );
}