"use client";

import Link from "next/link";
import { Brand } from "@/components/layout/brand";
import { IconButton } from "@/components/ui/icon-button";
import { Settings, Trophy, UserIcon } from "@/components/ui/icons";
import { PlayerMiniProfile } from "./player-mini-profile";
import { useAuthStore } from "@/store/auth-store";
import { MOCK_CURRENT_USER } from "@/mocks/users";

export function LobbyHeader() {
  const user = useAuthStore((s) => s.user) ?? MOCK_CURRENT_USER;

  return (
    <section className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-forest-500/25 bg-surface px-4 py-3.5 panel-emboss sm:px-5">
      <Brand size="sm" subtitle={false} />
      <div className="flex items-center gap-3">
        <PlayerMiniProfile user={user} size="md" />
        <span className="mx-1 hidden h-9 w-px bg-forest-500/25 sm:block" aria-hidden />
        <div className="flex items-center gap-2">
          <Link href="/profile">
            <IconButton label="Profile" variant="gold" size="md">
              <UserIcon className="size-5" aria-hidden />
            </IconButton>
          </Link>
          <Link href="/leaderboard">
            <IconButton label="Leaderboard" size="md">
              <Trophy className="size-5" aria-hidden />
            </IconButton>
          </Link>
          <Link href="/settings">
            <IconButton label="Settings" size="md">
              <Settings className="size-5" aria-hidden />
            </IconButton>
          </Link>
        </div>
      </div>
    </section>
  );
}