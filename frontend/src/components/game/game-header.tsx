import Link from "next/link";
import { Brand } from "@/components/layout/brand";
import { Badge, type BadgeTone } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { LogOut } from "@/components/ui/icons";
import { TurnTimer } from "./turn-timer";
import { MatchStatus, type GameState } from "@/types/game";

const STATUS: Record<MatchStatus, { label: string; tone: BadgeTone }> = {
  [MatchStatus.WAITING]: { label: "অপেক্ষমাণ", tone: "emerald" },
  [MatchStatus.IN_PROGRESS]: { label: "চলছে", tone: "gold" },
  [MatchStatus.FINISHED]: { label: "শেষ", tone: "neutral" },
  [MatchStatus.ABANDONED]: { label: "পরিত্যক্ত", tone: "crimson" },
};

export function GameHeader({
  game,
  selfId,
}: {
  game: GameState;
  selfId: string;
}) {
  const status = STATUS[game.status];
  const currentPlayer = game.players.find((p) => p.id === game.currentTurnPlayerId);

  return (
    <header className="sticky top-0 z-30 border-b border-forest-500/20 bg-deep-950/85 backdrop-blur-lg">
      <div className="mx-auto flex h-16 w-full max-w-7xl items-center justify-between gap-3 px-4 sm:px-6">
        <Brand size="sm" subtitle={false} />
        <div className="flex items-center gap-3">
          <Badge tone={status.tone} className="hidden sm:inline-flex">
            {status.label}
          </Badge>
          <TurnTimer
            playerName={currentPlayer?.displayName ?? currentPlayer?.username ?? ""}
            turnNumber={game.turnNumber}
            isSelf={currentPlayer?.id === selfId}
          />
        </div>
        <Link href="/lobby">
          <Button variant="ghost" size="sm">
            <LogOut className="size-4" aria-hidden />
            <span className="hidden sm:inline">বেরিয়ে যান</span>
          </Button>
        </Link>
      </div>
    </header>
  );
}