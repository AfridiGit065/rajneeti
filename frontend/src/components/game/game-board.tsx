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
import { ExchangeSelection } from "./exchange-selection";
import { GameLog } from "./game-log";
import {
  BlockPanel,
  BlockDialog,
  BlockResult,
  type BlockEvent,
  type BlockResultData,
  type BlockOutcome,
} from "./block-system";
import {
  InfluenceLostModal,
  EliminationOverlay,
} from "./card-reveal-elimination";
import { getAction } from "@/lib/game/actions";
import { CHARACTER_MAP } from "@/lib/game/characters";
import { GameService } from "@/services/game-service";
import { MOCK_CURRENT_USER } from "@/mocks/users";
import { useAuthStore } from "@/store/auth-store";
import { useToast } from "@/hooks/use-toast";
import { MatchStatus, type GameActionId, type GamePhase, type GamePlayer, type GameState } from "@/types/game";

const PHASE_LABEL: Record<GamePhase, string> = {
  setup: "Setup",
  action_selection: "Action Selection",
  action_resolution: "Action Resolution",
  challenge_resolution: "Challenge Resolution",
  block_resolution: "Block Resolution",
  card_reveal: "Card Reveal",
  game_over: "Game Over",
};

function DeckDiscard({ game }: { game: GameState }) {
  return (
    <div className="rounded-2xl border border-forest-500/25 bg-surface p-4 panel-emboss">
      <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted">
        Deck & Discard
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
          <span className="text-[0.65rem] uppercase tracking-wider text-muted">Deck</span>
        </div>
        <div className="flex flex-col items-center gap-2">
          <span className="flex size-10 items-center justify-center rounded-lg border border-crimson-500/30 bg-deep-800 opacity-70">
            <Landmark className="size-4 text-crimson-300" aria-hidden />
          </span>
          <span className="font-mono text-lg font-bold text-crimson-300">
            {game.revealedCardsCount}
          </span>
          <span className="text-[0.65rem] uppercase tracking-wider text-muted">Revealed</span>
        </div>
      </div>
    </div>
  );
}

function ActiveAction({ game }: { game: GameState }) {
  return (
    <div className="rounded-2xl border border-gold-500/25 bg-surface p-4 panel-emboss">
      <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted">
        Active Action
      </p>
      {game.activeAction ? (
        <div className="mt-2 flex flex-wrap items-center gap-2">
          <Swords className="size-5 text-gold-400" aria-hidden />
          <span className="text-lg font-bold text-gold-300">
            {getAction(game.activeAction.action).nameEn} ({getAction(game.activeAction.action).nameBn})
          </span>
          {game.activeAction.claimedCharacter ? (
            <Badge tone="parchment">
              {CHARACTER_MAP[game.activeAction.claimedCharacter]?.nameBn} Claimed
            </Badge>
          ) : null}
        </div>
      ) : (
        <p className="mt-2 text-sm text-muted">No active action.</p>
      )}
      <div className="mt-3 flex items-center gap-3">
        <Badge tone="gold">
          {PHASE_LABEL[game.phase]}
        </Badge>
        <span className="text-xs text-muted">Turn #{game.turnNumber}</span>
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

  // Block System State (Module F20)
  const [activeBlock, setActiveBlock] = useState<BlockEvent | null>(null);
  const [blockDialogOpen, setBlockDialogOpen] = useState(false);
  const [blockResultData, setBlockResultData] = useState<BlockResultData | null>(null);

  // Card Reveal & Elimination State (Module F21)
  const [influenceLostOpen, setInfluenceLostOpen] = useState(false);
  const [influenceLostReason, setInfluenceLostReason] = useState("");
  const [eliminationPlayer, setEliminationPlayer] = useState<GamePlayer | null>(null);

  // Exchange selection overlay (Module 15) — opens while the local player has
  // a pending Exchange. `game.exchangePool` is only populated for the actor.
  const [exchangeOpen, setExchangeOpen] = useState(false);

  const load = useCallback(async () => GameService.getGameState(matchId), [matchId]);

  useEffect(() => {
    let cancelled = false;
    load().then((result) => {
      if (cancelled) return;
      if (result.ok) {
        setGame(result.data);
        if (result.data.exchangePool) setExchangeOpen(true);
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
      if (result.data.exchangePool) setExchangeOpen(true);
    } else {
      setError(result.error.message);
    }
    setLoading(false);
  }

  async function handleAction(actionId: GameActionId, targetPlayerId?: string) {
    setBusy(actionId);
    const result = await GameService.performAction(matchId, {
      action: actionId,
      claimedCharacter: actionId === "tax" ? ("minister" as const) : undefined,
      targetPlayerId,
    });
    setBusy(null);

    if (result.ok) {
      setGame(result.data);
      if (result.data.exchangePool) setExchangeOpen(true);
      success(`${getAction(actionId).nameBn} — অ্যাকশন চলছে`);

      // Mock block opportunity triggers for F20 demonstration
      if (actionId === "foreign_aid" || actionId === "steal" || actionId === "assassinate") {
        const potentialBlocker =
          (targetPlayerId ? result.data.players.find((p) => p.id === targetPlayerId) : null) ??
          result.data.players.find((p) => p.userId !== selfId && p.isAlive);

        if (potentialBlocker) {
          const claimedCharacter =
            actionId === "foreign_aid"
              ? "minister"
              : actionId === "steal"
              ? "amla"
              : "goyenda";

          const actor =
            result.data.players.find((p) => p.userId === selfId) ?? result.data.players[0]!;

          const newBlockEvent: BlockEvent = {
            blocker: potentialBlocker,
            targetOrActor: actor,
            actionId,
            claimedCharacter,
          };

          setActiveBlock(newBlockEvent);
          setBlockDialogOpen(true);
        }
      }

      // If action is Coup, trigger card loss for the target player (F21 simulation)
      if (actionId === "coup" && targetPlayerId) {
        const target = result.data.players.find((p) => p.id === targetPlayerId);
        if (target) {
          setInfluenceLostReason(`ক্ষমতা দখল (Coup) আক্রমণের শিকার হওয়ায় ১টি ইনফ্লুয়েন্স হারাচ্ছেন`);
          setInfluenceLostOpen(true);
        }
      }
    } else {
      notifyError(result.error.message);
    }
  }

  function handleBlockResolution(outcome: BlockOutcome) {
    if (!activeBlock || !game) return;

    const challenger =
      game.players.find((p) => p.userId === selfId) ?? game.players[0]!;

    const resultData: BlockResultData = {
      outcome,
      blocker: activeBlock.blocker,
      challenger,
      claimedCharacter: activeBlock.claimedCharacter,
      actionId: activeBlock.actionId,
    };

    setBlockResultData(resultData);
    setActiveBlock(null);

    // If challenger or blocker loses influence, trigger F21 Card Reveal
    if (outcome === "challenger_loses_influence") {
      setInfluenceLostReason("ব্লকের বিরুদ্ধে করা চ্যালেঞ্জে ব্যর্থ হওয়ায় ১টি ইনফ্লুয়েন্স হারাচ্ছেন");
      setInfluenceLostOpen(true);
    } else if (
      outcome === "blocker_loses_influence" ||
      outcome === "block_claim_is_bluff"
    ) {
      setInfluenceLostReason("ব্লকের মিথ্যা দাবি ফাঁস হওয়ায় ব্লকার ১টি ইনফ্লুয়েন্স হারাচ্ছেন");
      setInfluenceLostOpen(true);
    }
  }

  function handleConfirmCardReveal(revealedCardId: string) {
    if (!game) return;
    setInfluenceLostOpen(false);

    // Update game state: reveal the selected card
    const updatedPlayers = game.players.map((p) => {
      const hasCard = p.influenceCards.some((c) => c.id === revealedCardId);
      if (!hasCard) return p;

      const updatedCards = p.influenceCards.map((c) =>
        c.id === revealedCardId ? { ...c, revealed: true } : c,
      );
      const remainingHidden = updatedCards.filter((c) => !c.revealed).length;
      const isAlive = remainingHidden > 0;

      // If reached 0 influence, trigger EliminationOverlay
      if (!isAlive && p.isAlive) {
        setEliminationPlayer({ ...p, isAlive: false, influenceCards: updatedCards });
      }

      return {
        ...p,
        isAlive,
        influenceCards: updatedCards,
      };
    });

    setGame({
      ...game,
      players: updatedPlayers,
      revealedCardsCount: game.revealedCardsCount + 1,
    });

    success("কার্ড সফলভাবে উন্মোচিত হয়েছে");
  }

  function handleBlockResultDismiss() {
    if (blockResultData?.actionId === "foreign_aid" && game) {
      const blocked =
        blockResultData.outcome === "block_succeeds" ||
        blockResultData.outcome === "challenger_loses_influence";
      void GameService.resolveForeignAid(matchId, blocked).then((result) => {
        if (result.ok) setGame(result.data);
      });
    }
    setBlockResultData(null);
  }

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-app px-4">
        <LoadingState label="Loading game board…" />
      </div>
    );
  }

  if (error !== null || !game) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-app px-4">
        <div className="w-full max-w-lg">
          <ErrorState
            title="Could not load game board"
            message={error ?? "Match state could not be found."}
            action={
              <Button variant="premium" onClick={retry}>
                Try Again
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
          title="No Players"
          description="No players have joined this match yet."
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

      <main className="mx-auto grid w-full max-w-7xl flex-1 gap-4 px-3 py-3 sm:px-4 sm:py-4 lg:grid-cols-[250px_minmax(0,1fr)_300px]">
        {/* === Opponents column (sidebar on desktop, horizontal scroll strip on mobile) === */}
        <aside className="order-1 space-y-3 lg:order-1">
          <div className="flex items-center justify-between">
            <h2 className="text-xs font-semibold uppercase tracking-[0.2em] text-muted">
              Opponents ({opponents.length})
            </h2>
            <span className="text-[10px] text-muted lg:hidden">
              Scroll right ➔
            </span>
            <Badge tone="neutral" className="hidden lg:inline-flex">{opponents.length}</Badge>
          </div>
          {/* Scrollable on mobile/tablet, stacked column on desktop */}
          <div className="flex gap-3 overflow-x-auto pb-2 pt-1 lg:flex-col lg:overflow-visible lg:pb-0 scrollbar-thin">
            {opponents.map((player) => (
              <div key={player.id} className="min-w-[240px] sm:min-w-[260px] lg:min-w-0 flex-shrink-0 lg:flex-shrink">
                <OpponentSeat player={player} />
              </div>
            ))}
          </div>
        </aside>

        {/* === Main center column === */}
        <section className="order-2 flex flex-col gap-4 lg:order-2">
          {game.status === MatchStatus.FINISHED && winner ? (
            <div className="flex items-center justify-center gap-2 rounded-2xl border border-gold-500/40 bg-gold-500/10 px-4 py-3">
              <Trophy className="size-5 text-gold-300" aria-hidden />
              <p className="font-semibold text-gold-200">
                Winner: {winner.displayName ?? winner.username}
              </p>
            </div>
          ) : null}

          {/* Block result card if recently resolved */}
          {blockResultData && (
            <BlockResult
              result={blockResultData}
              onDismiss={handleBlockResultDismiss}
            />
          )}

          {/* Active Block Indicator */}
          {activeBlock && (
            <BlockPanel
              activeBlock={activeBlock}
              currentPlayer={currentPlayer}
              onOpenDialog={() => setBlockDialogOpen(true)}
            />
          )}

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

        {/* === Game Log (hidden on small screens, visible on desktop) === */}
        <aside className="order-3 hidden lg:block">
          <GameLog entries={game.log} />
        </aside>
      </main>

      {/* Module F20: Block Dialog Modal */}
      {activeBlock && (
        <BlockDialog
          event={activeBlock}
          currentPlayer={currentPlayer}
          open={blockDialogOpen}
          onClose={() => setBlockDialogOpen(false)}
          onResolve={handleBlockResolution}
        />
      )}

      {/* Module F21: Influence Lost Modal (Card Reveal Choice) */}
      <InfluenceLostModal
        open={influenceLostOpen}
        player={currentPlayer}
        reason={influenceLostReason}
        onConfirmReveal={handleConfirmCardReveal}
      />

      {/* Module F21: Elimination Dramatic Overlay */}
      {eliminationPlayer && (
        <EliminationOverlay
          open={eliminationPlayer !== null}
          eliminatedPlayer={eliminationPlayer}
          onFinish={() => setEliminationPlayer(null)}
        />
      )}

      {/* Module 15: Exchange Selection Overlay */}
      {game.exchangePool && exchangeOpen ? (
        <ExchangeSelection
          matchId={matchId}
          cards={game.exchangePool}
          onResolved={(next) => setGame(next)}
          onClose={() => setExchangeOpen(false)}
        />
      ) : null}
    </div>
  );
}