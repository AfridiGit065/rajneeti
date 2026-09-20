import Link from "next/link";
import { Brand } from "@/components/layout/brand";
import { Button } from "@/components/ui/button";
import { LogOut, Flame, ScrollText } from "@/components/ui/icons";
import { TurnTimer } from "./turn-timer";
import { cn } from "@/lib/cn";
import { MatchStatus, type GameState } from "@/types/game";

const STATUS_LABELS: Record<MatchStatus, string> = {
  [MatchStatus.WAITING]: "WAITING FOR PLAYERS",
  [MatchStatus.IN_PROGRESS]: "MATCH IN PROGRESS",
  [MatchStatus.FINISHED]: "MATCH FINISHED",
  [MatchStatus.ABANDONED]: "MATCH ABANDONED",
};

export function GameHeader({
  game,
  selfId,
  onToggleChronicle,
  isChronicleOpen = false,
  chronicleCount = 0,
}: {
  game: GameState;
  selfId: string;
  onToggleChronicle?: () => void;
  isChronicleOpen?: boolean;
  chronicleCount?: number;
}) {
  const statusLabel = STATUS_LABELS[game.status];
  const currentPlayer = game.players.find((p) => p.id === game.currentTurnPlayerId);
  const isMyTurn = currentPlayer?.id === selfId;

  return (
    <header className="sticky top-0 z-30 h-14 border-b border-forest-500/20 bg-deep-950/85 backdrop-blur-xl select-none">
      <div className="mx-auto flex h-full w-full max-w-7xl items-center justify-between gap-3 px-3 sm:px-6">
        {/* Left: Brand + Status Pill */}
        <div className="flex items-center gap-3">
          <Brand size="sm" subtitle={false} />
          <span className="hidden md:inline-flex items-center rounded-full border border-gold-500/20 bg-gold-500/10 px-2.5 py-0.5 font-cinzel text-[10px] font-bold tracking-widest text-gold-300/90 uppercase">
            {statusLabel}
          </span>
        </div>

        {/* Center: Round + Prominent Turn Indicator */}
        <div className="flex items-center gap-2 sm:gap-3">
          <span className="hidden sm:inline font-cinzel text-xs font-semibold tracking-wider text-muted/80 uppercase">
            Round {game.turnNumber}
          </span>

          <span className="hidden sm:inline text-forest-500/40">•</span>

          <div
            className={cn(
              "flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-bold transition-all",
              isMyTurn
                ? "border border-gold-500/70 bg-gold-500/20 text-gold-200 your-turn-glow shadow-gold"
                : currentPlayer
                  ? "border border-forest-500/25 bg-deep-900/70 text-muted"
                  : "border border-forest-500/20 bg-deep-900/50 text-muted",
            )}
          >
            {isMyTurn ? (
              <>
                <Flame className="size-3.5 text-gold-400 animate-pulse" aria-hidden />
                <span className="font-cinzel tracking-wider uppercase text-gold-200">YOUR TURN</span>
              </>
            ) : currentPlayer ? (
              <span className="truncate max-w-[140px] sm:max-w-none text-muted">
                Waiting for <strong className="text-ivory font-semibold">{currentPlayer.displayName ?? currentPlayer.username}</strong>
              </span>
            ) : null}
          </div>
        </div>

        {/* Right: Chronicle Toggle + Timer + Leave Match */}
        <div className="flex items-center gap-2 sm:gap-3">
          {onToggleChronicle && (
            <button
              type="button"
              onClick={onToggleChronicle}
              className={cn(
                "flex items-center gap-1.5 rounded-lg px-2.5 py-1 text-xs font-semibold transition-all cursor-pointer",
                isChronicleOpen
                  ? "bg-gold-500/20 text-gold-200 border border-gold-500/60 shadow-gold"
                  : "bg-deep-900/80 text-muted hover:text-ivory hover:bg-deep-850 border border-forest-500/25",
              )}
              title="Toggle Match Chronicle / Log"
            >
              <ScrollText className="size-3.5 text-gold-400" aria-hidden />
              <span className="hidden sm:inline font-cinzel uppercase tracking-wider text-[11px]">Chronicle</span>
              {chronicleCount > 0 && (
                <span className="rounded px-1.5 py-0.2 font-mono text-[10px] font-bold bg-gold-500/15 text-gold-300 border border-gold-500/30">
                  {chronicleCount}
                </span>
              )}
            </button>
          )}

          <TurnTimer
            playerName={currentPlayer?.displayName ?? currentPlayer?.username ?? ""}
            turnNumber={game.turnNumber}
            isSelf={isMyTurn}
          />
          <Link href="/lobby">
            <Button variant="ghost" size="sm" className="text-muted hover:text-crimson-300">
              <LogOut className="size-3.5 mr-1" aria-hidden />
              <span className="hidden sm:inline text-xs font-cinzel uppercase tracking-wider">Leave</span>
            </Button>
          </Link>
        </div>
      </div>
    </header>
  );
}