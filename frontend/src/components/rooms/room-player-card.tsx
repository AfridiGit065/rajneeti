"use client";

import type { RoomPlayer } from "@/types/room";
import { cn } from "@/lib/cn";
import { Badge } from "@/components/ui/badge";
import { Check, Crown } from "@/components/ui/icons";

interface RoomPlayerCardProps {
  player: RoomPlayer;
  isMe?: boolean;
}

export function RoomPlayerCard({ player, isMe = false }: RoomPlayerCardProps) {
  const ready = player.isReady;
  return (
    <div className="flex flex-col gap-2.5">
      <div className="flex items-center gap-3">
        <span
          className={cn(
            "flex size-10 shrink-0 items-center justify-center rounded-full text-sm font-bold",
            isMe
              ? "bg-gradient-to-b from-gold-400 to-gold-600 text-deep-950 ring-2 ring-gold-400/70"
              : "bg-gradient-to-b from-forest-400 to-forest-600 text-deep-950",
          )}
        >
          {player.user.avatarInitial}
        </span>
        <div className="min-w-0 flex-1">
          <p className="flex items-center gap-1.5 text-sm font-semibold text-ivory">
            <span className="truncate">{player.user.displayName}</span>
            {player.isHost ? (
              <Badge tone="gold" className="shrink-0">
                <Crown className="size-3" aria-hidden />
                হোস্ট
              </Badge>
            ) : null}
            {isMe ? <Badge tone="parchment">আপনি</Badge> : null}
          </p>
          <p className="truncate text-xs text-muted">@{player.user.username}</p>
        </div>
      </div>

      <span key={ready ? "ready" : "waiting"} className="animate-zoom-in">
        <Badge tone={ready ? "emerald" : "neutral"}>
          <Check
            className={cn("size-3", !ready && "opacity-0")}
            aria-hidden={!ready}
          />
          {ready ? "রেডি" : "অপেক্ষা"}
        </Badge>
      </span>
    </div>
  );
}