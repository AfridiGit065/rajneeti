"use client";

import { useCallback, useEffect, useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/ui/empty-state";
import { ErrorState } from "@/components/ui/error-state";
import { LoadingState } from "@/components/ui/loading-state";
import { Landmark, ScrollText, Swords, Trophy } from "@/components/ui/icons";
import { GameHeader } from "./game-header";
import { PlayerSeat } from "./player-seat";
import { OpponentSeat } from "./opponent-seat";
import { OwnCards } from "./own-cards";
import { ActionPanel } from "./action-panel";
import { ChallengeFlow } from "./challenge";
import { GameLog } from "./game-log";
import { getAction } from "@/lib/game/actions";
import { CHARACTER_MAP } from "@/lib/game/characters";
import { GameService } from "@/services/game-service";
import { MOCK_CURRENT_USER } from "@/mocks/users";
import { useAuthStore } from "@/store/auth-store";
import { useToast } from "@/hooks/use-toast";
import { MatchStatus, type GameActionId, type GamePhase, type GameState } from "@/types/game";

const PHASE_LABEL: Record<GamePhase, string> = {
  setup: "সেটআপ",
  action_selection: "অ্যাকশন নির্বাচন",
  action_resolution: "অ্যাকশন সমাধান",
  challenge_resolution: "চ্যালেঞ্জ সমাধান",
  block_resolution: "ব্লক সমাধান",
  card_reveal: "কার্ড উন্মোচন",
  game_over: "খেলা শেষ",
};

function DeckDiscard({ game }: { game: GameState }) {
  return (
    <div className="rounded-2xl border border-forest-500/25 bg-surface p-4 panel-emboss">
      <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted">
        ডেক ও ডিসকার্ড
      </p>
      <div className="mt-3 flex items-center justify-center gap-10">
        <div className="flex flex-col items-center gap-2">
          <div className="relative h-14 w-10">
            <span className="absolute -left-1 top-1 block size-10 rounded-lg border border-gold-500/30 bg-deep-700" aria-hidden />
            <span className="absolute -left-0.5 top-0.5 block size-10 rounded-lg border border-gold-500/40 bg-deep-600" aria-hidden />
            <span className="absolute left-0 top-0 flex size-10 items-center justify-center rounded-lg border border-gold-500/50 bg-gradient-to-b from-deep-600 to-deep-800 shadow-gold">
              <ScrollText className="size-4 text-gold-400" aria-hidden />
            </span>
          </div>
          <span className="font-mono text-lg font-bold text-gold-300">{game.deckCount}</span>
          <span className="text-[0.65rem] uppercase tracking-wider text-muted">ডেক</span>
        </div>
        <div className="flex flex-col items-center gap-2">
          <span className="flex size-10 items-center justify-center rounded-lg border border-crimson-500/30 bg-deep-800 opacity-70">
            <Landmark className="size-4 text-crimson-300" aria-hidden />
          </span>
          <span className="font-mono text-lg font-bold text-crimson-300">
            {game.revealedCardsCount}
          </span>
          <span className="text-[0.65rem] uppercase tracking-wider text-muted">উন্মোচিত</span>
        </div>
      </div>
    </div>
  );
}

function ActiveAction({ game }: { game: GameState }) {
  return (
    <div className="rounded-2xl border border-gold-500/25 bg-surface p-4 panel-emboss">
      <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted">
        সক্রিয় অ্যাকশন
      </p>
      {game.activeAction ? (
        <div className="mt-2 flex flex-wrap items-center gap-2">
          <Swords className="size-5 text-gold-400" aria-hidden />
          <span className="text-lg font-bold text-gold-300">
            {getAction(game.activeAction.action).nameBn}
          </span>
          {game.activeAction.claimedCharacter ? (
            <Badge tone="parchment">
              {CHARACTER_MAP[game.activeAction.claimedCharacter]?.nameBn} দাবি
            </Badge>
          ) : null}
        </div>
      ) : (
        <p className="mt-2 text-sm text-muted">কোনো সক্রিয় অ্যাকশন নেই।</p>
      )}
      <div className="mt-3 flex items-center gap-3">
        <Badge tone="gold">
          {PHASE_LABEL[game.phase]}
        </Badge>
        <span className="text-xs text-muted">টার্ন #{game.turnNumber}</span>
      </div>
    </div>
  );
}

export function GameBoard({ matchId }: { matchId: string }) {
  const [game, setGame] = useState<GameState | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState<GameActionId | null>(null);
  const { success, error: notifyError } = useToast();
  const selfId = useAuthStore((s) => s.user)?.id ?? MOCK_CURRENT_USER.id;

  const load = useCallback(async () => GameService.getGameState(matchId), [matchId]);

  useEffect(() => {
    let cancelled = false;
    load().then((result) => {
      if (cancelled) return;
      if (result.ok) {
        setGame(result.data);
      } else {
        setError(result.error.message);
      }
      setLoading(false);
    });
    return () => {
      cancelled = true;
    };
  }, [load]);

  async function retry() {
    setLoading(true);
    setError(null);
    const result = await load();
    if (result.ok) {
      setGame(result.data);
    } else {
      setError(result.error.message);
    }
    setLoading(false);
  }

  async function handleAction(actionId: GameActionId, targetPlayerId?: string) {
    setBusy(actionId);
    const result = await GameService.performAction(matchId, {
      action: actionId,
      targetPlayerId,
    });
    setBusy(null);
    if (result.ok) {
      setGame(result.data);
      success(`${getAction(actionId).nameBn} — অ্যাকশন চলছে`);
    } else {
      notifyError(result.error.message);
    }
  }

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-app px-4">
        <LoadingState label="গেম বোর্ড লোড হচ্ছে…" />
      </div>
    );
  }

  if (error !== null || !game) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-app px-4">
        <div className="w-full max-w-lg">
          <ErrorState
            title="গেম বোর্ড লোড করা যায়নি"
            message={error ?? "ম্যাচের অবস্থা পাওয়া যায়নি।"}
            action={
              <Button variant="premium" onClick={retry}>
                আবার চেষ্টা করুন
              </Button>
            }
          />
        </div>
      </div>
    );
  }

  const currentPlayer =
    game.players.find((p) => p.userId === selfId) ??
    game.players.find((p) => p.isTurn) ??
    game.players[0];

  if (!currentPlayer) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-app px-4">
        <EmptyState
          icon={<Trophy className="size-6" aria-hidden />}
          title="কোনো খেলোয়াড় নেই"
          description="এই ম্যাচে এখনো কোনো খেলোয়াড় যোগ হয়নি।"
        />
      </div>
    );
  }

  const opponents = game.players
    .filter((p) => p.id !== currentPlayer.id)
    .sort((a, b) => a.seatIndex - b.seatIndex);

  const winner = game.winnerPlayerId
    ? game.players.find((p) => p.id === game.winnerPlayerId)
    : null;

  return (
    <div className="flex min-h-screen flex-col">
      <GameHeader game={game} selfId={selfId} />

      <main className="mx-auto grid w-full max-w-7xl flex-1 gap-4 px-4 py-4 sm:px-6 lg:grid-cols-[250px_minmax(0,1fr)_300px]">
        <aside className="order-2 space-y-3 lg:order-1">
          <div className="flex items-center justify-between">
            <h2 className="text-xs font-semibold uppercase tracking-[0.2em] text-muted">
              প্রতিপক্ষ
            </h2>
            <Badge tone="neutral">{opponents.length}</Badge>
          </div>
          {opponents.map((player) => (
            <OpponentSeat key={player.id} player={player} />
          ))}
        </aside>

        <section className="order-1 flex flex-col gap-4 lg:order-2">
          {game.status === MatchStatus.FINISHED && winner ? (
            <div className="flex items-center justify-center gap-2 rounded-2xl border border-gold-500/40 bg-gold-500/10 px-4 py-3">
              <Trophy className="size-5 text-gold-300" aria-hidden />
              <p className="font-bengali font-semibold text-gold-200">
                বিজয়ী: {winner.displayName ?? winner.username}
              </p>
            </div>
          ) : null}

          <div className="grid gap-4 md:grid-cols-2">
            <ActiveAction game={game} />
            <DeckDiscard game={game} />
          </div>

          {game.status === MatchStatus.IN_PROGRESS ? (
            <ChallengeFlow game={game} selfId={selfId} onResolved={setGame} />
          ) : null}

          <div className="mt-auto flex flex-col items-center justify-end gap-4 lg:flex-row lg:items-end">
            <PlayerSeat player={currentPlayer} />
            <OwnCards cards={currentPlayer.influenceCards} />
          </div>

          <ActionPanel
            player={currentPlayer}
            opponents={opponents}
            busy={busy !== null}
            onAction={(actionId, targetPlayerId) => void handleAction(actionId, targetPlayerId)}
          />
        </section>

        <aside className="order-3">
          <GameLog entries={game.log} />
        </aside>
      </main>
    </div>
  );
}