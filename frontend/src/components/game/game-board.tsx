"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import Image from "next/image";
import { useRealtimeStore } from "@/store/realtime-store";
import { useMatchRealtime } from "@/hooks/use-match-realtime";
import { realtimeSocket } from "@/lib/websocket/stomp-client";
import { isSuperseded, type VersionKey } from "@/lib/realtime/state-version";
import { toGameState } from "@/lib/game/backend-game-state";
import { cn } from "@/lib/cn";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/ui/empty-state";
import { ErrorState } from "@/components/ui/error-state";
import { LoadingState } from "@/components/ui/loading-state";
import { Landmark, Swords, Trophy, ScrollText, X } from "@/components/ui/icons";
import { GameHeader } from "./game-header";
import { PlayerSeat } from "./player-seat";
import { OpponentSeat } from "./opponent-seat";
import { OwnCards } from "./own-cards";
import { ActionPanel } from "./action-panel";
import { ChallengeFlow } from "./challenge";
import { GameLog } from "./game-log";
import { GameOverScreen } from "./game-over";
import {
  BlockPanel,
  BlockDialog,
  BlockResult,
  BlockOffer,
  BlockWindowPanel,
  type BlockOutcome,
  type BlockResultData,
} from "./block-system";
import {
  InfluenceLostModal,
  EliminationOverlay,
} from "./card-reveal-elimination";
import { ExchangeSelection } from "./exchange-selection";
import { getAction } from "@/lib/game/actions";
import { CHARACTER_MAP } from "@/lib/game/characters";
import { GameService } from "@/services/game-service";
import { useAuthStore } from "@/store/auth-store";
import { useToast } from "@/hooks/use-toast";
import { MatchStatus, type GameActionId, type GamePhase, type GamePlayer, type GameState, type ActionResult } from "@/types/game";
import type { CharacterId } from "@/types/character";

const PHASE_LABEL: Record<GamePhase, string> = {
  setup: "Setup",
  action_selection: "Action Selection",
  action_resolution: "Action Resolution",
  challenge_resolution: "Challenge Resolution",
  block_resolution: "Block Resolution",
  card_reveal: "Card Reveal",
  game_over: "Game Over",
};

/** Actions that open a block window (Module 19). */
const BLOCKABLE_ACTIONS: readonly GameActionId[] = [
  "foreign_aid",
  "steal",
  "assassinate",
];

function isBlockable(id: GameActionId): boolean {
  return BLOCKABLE_ACTIONS.includes(id);
}

function DeckDiscard({ game }: { game: GameState }) {
  return (
    <div className="flex items-center gap-3 sm:gap-4 select-none shrink-0">
      {/* Draw Pile / Deck Stack */}
      <div className="flex flex-col items-center gap-1 group">
        <span className="font-cinzel text-[10px] font-bold uppercase tracking-widest text-gold-400">
          DECK
        </span>
        <div
          className="relative flex h-16 w-12 sm:h-20 sm:w-14 items-center justify-center rounded-xl border border-gold-500/60 bg-gradient-to-b from-[#0e3b2e] to-[#041610] shadow-[2px_2px_0_rgba(201,165,60,0.3),5px_5px_0_rgba(0,0,0,0.6)] cursor-default transition-transform hover:-translate-y-1"
          title={`${game.deckCount} cards remaining in draw pile`}
        >
          <div className="flex size-6 sm:size-7 items-center justify-center rounded-full border border-gold-400/70 bg-deep-950/80 shadow-inner">
            <span className="font-bengali text-xs sm:text-sm font-bold text-gold-300">র</span>
          </div>
        </div>
        <span className="font-mono text-xs font-bold text-gold-300 bg-deep-950/90 px-2 py-0.5 rounded-full border border-gold-500/25">
          {game.deckCount}
        </span>
      </div>

      {/* Discard Pile Stack */}
      <div className="flex flex-col items-center gap-1 group">
        <span className="font-cinzel text-[10px] font-bold uppercase tracking-widest text-crimson-400">
          DISCARD
        </span>
        <div
          className={cn(
            "relative flex h-16 w-12 sm:h-20 sm:w-14 items-center justify-center rounded-xl border cursor-default transition-transform hover:-translate-y-1",
            game.revealedCardsCount > 0
              ? "border-crimson-500/50 bg-gradient-to-b from-[#2a0e14] to-[#120508] shadow-[2px_2px_0_rgba(180,40,60,0.3),5px_5px_0_rgba(0,0,0,0.6)]"
              : "border-forest-500/20 bg-deep-950/40 border-dashed",
          )}
          title={`${game.revealedCardsCount} revealed/lost cards in discard pile`}
        >
          {game.revealedCardsCount > 0 ? (
            <Landmark className="size-4 sm:size-5 text-crimson-400" aria-hidden />
          ) : (
            <span className="text-[10px] text-muted/40 font-mono">0</span>
          )}
        </div>
        <span className="font-mono text-xs font-bold text-crimson-300 bg-deep-950/90 px-2 py-0.5 rounded-full border border-crimson-500/25">
          {game.revealedCardsCount}
        </span>
      </div>
    </div>
  );
}

function ActiveAction({ game }: { game: GameState }) {
  const active = game.activeAction;
  const action = active ? getAction(active.action) : null;
  const claimedChar = active?.claimedCharacter ? CHARACTER_MAP[active.claimedCharacter] : null;
  const actor = game.players.find((p) => p.id === game.currentTurnPlayerId);
  const target = active?.targetPlayerId ? game.players.find((p) => p.id === active.targetPlayerId) : null;

  return (
    <div
      className={cn(
        "action-focal-card relative overflow-hidden rounded-2xl border transition-all select-none w-full",
        active
          ? "border-gold-500/70 bg-[#061d15]/95 shadow-[0_0_35px_rgba(201,165,60,0.25)] active-action-glow"
          : "border-forest-500/30 bg-[#041610]/90 backdrop-blur-md",
      )}
      role="region"
      aria-label="Active Game Action"
    >
      {/* Ambient radial glow */}
      {active && (
        <div
          className="pointer-events-none absolute inset-0 rounded-2xl"
          style={{
            background: "radial-gradient(ellipse 75% 65% at 50% 0%, rgba(201,165,60,0.18) 0%, transparent 70%)",
          }}
        />
      )}

      {active && action ? (
        <div className="flex flex-col gap-0 px-4 py-3 sm:px-6 sm:py-4">
          {/* Header row: ACTIVE ACTION badge + Phase */}
          <div className="flex items-center justify-between gap-2 border-b border-gold-500/20 pb-2.5 mb-3">
            <div className="flex items-center gap-1.5">
              <Swords className="size-4 text-gold-400" aria-hidden />
              <span className="font-cinzel text-xs font-bold uppercase tracking-widest text-gold-300">
                Active Action
              </span>
            </div>
            <div className="flex items-center gap-2">
              <Badge tone="gold" className="text-[10px] py-0">{PHASE_LABEL[game.phase]}</Badge>
              <span className="font-mono text-[10px] text-muted/60">Round #{game.turnNumber}</span>
            </div>
          </div>

          {/* Main Action Content */}
          <div className="flex items-center gap-4 sm:gap-5">
            {/* Character portrait or Action Icon */}
            <div className="relative shrink-0">
              {claimedChar ? (
                <div className="relative size-16 sm:size-20 overflow-hidden rounded-xl border-2 border-gold-400/70 bg-deep-900 shadow-gold">
                  <Image
                    src={claimedChar.imagePath}
                    alt={claimedChar.nameBn}
                    fill
                    sizes="80px"
                    className="object-cover object-top"
                  />
                  <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-deep-950/70 to-transparent" />
                </div>
              ) : (
                <div className="flex size-16 sm:size-20 shrink-0 items-center justify-center rounded-xl border-2 border-gold-500/40 bg-gradient-to-b from-gold-500/20 to-gold-500/5 shadow-inner">
                  <Swords className="size-8 text-gold-400" aria-hidden />
                </div>
              )}
            </div>

            <div className="min-w-0 flex-1 leading-tight">
              {/* Action name — large and prominent */}
              <div className="flex flex-wrap items-baseline gap-x-2.5 gap-y-0.5">
                <h3 className="text-2xl sm:text-3xl font-black text-ivory tracking-tight leading-none font-cinzel">
                  {action.nameEn}
                </h3>
                <span className="font-bengali text-base sm:text-lg font-bold text-gold-300 leading-none">
                  {action.nameBn}
                </span>
              </div>

              {/* Claim and target statement */}
              <p className="mt-2 text-xs sm:text-sm text-muted/90 leading-relaxed">
                <strong className="text-ivory font-bold">{actor?.displayName ?? actor?.username ?? "Player"}</strong>
                {claimedChar ? (
                  <> claims <span className="text-gold-300 font-bold font-bengali">{claimedChar.nameBn}</span>{" "}
                    <span className="text-muted/60">({claimedChar.nameEn})</span>
                  </>
                ) : (
                  <> executes this action</>
                )}
                {target && (
                  <> targeting <strong className="text-crimson-300 font-bold">{target.displayName ?? target.username}</strong></>
                )}
              </p>
            </div>
          </div>
        </div>
      ) : (
        /* Prominent Centered Waiting State */
        <div className="flex flex-col items-center justify-center gap-1.5 py-4 px-6 text-center">
          <div className="flex items-center gap-2">
            <span className="relative flex size-2.5">
              <span className="absolute inset-0 animate-ping rounded-full bg-gold-400/60" />
              <span className="relative size-2.5 rounded-full bg-gold-400" />
            </span>
            <span className="font-cinzel text-xs font-bold uppercase tracking-[0.25em] text-gold-400">
              Waiting for Action
            </span>
          </div>

          <p className="text-sm font-semibold text-ivory/90 mt-0.5">
            {actor ? (
              actor.isBot || actor.username.toLowerCase().includes("bot") ? (
                <span className="inline-flex items-center gap-1 text-gold-200">
                  <span>{actor.displayName ?? actor.username} is thinking</span>
                  <span className="flex items-center gap-0.5 ml-0.5">
                    <span className="size-1 rounded-full bg-gold-400 animate-dot-1" />
                    <span className="size-1 rounded-full bg-gold-400 animate-dot-2" />
                    <span className="size-1 rounded-full bg-gold-400 animate-dot-3" />
                  </span>
                </span>
              ) : (
                <span>
                  <strong className="text-gold-300 font-bold">{actor.displayName ?? actor.username}</strong>
                  &apos;s turn — deciding next move
                </span>
              )
            ) : (
              "Waiting for next player..."
            )}
          </p>

          <p className="font-bengali text-xs text-muted/60">
            খেলোয়াড়ের পদক্ষেপের প্রতীক্ষায়…
          </p>
        </div>
      )}
    </div>
  );
}

/** Module 20 — compact summary of the last authoritative verdict produced by the backend Action Resolver. */
function ActionResultSummary({
  result,
  players,
}: {
  result: ActionResult;
  players: GamePlayer[];
}) {
  const action = getAction(result.actionType);
  const cancelled = result.result === "CANCELLED";
  const blocker = result.blockedByUserId
    ? players.find((p) => p.id === result.blockedByUserId)
    : undefined;
  const loser = result.influenceLostById
    ? players.find((p) => p.id === result.influenceLostById)
    : undefined;

  return (
    <div className="w-full max-w-lg rounded-xl border border-forest-500/25 bg-deep-900/85 backdrop-blur-md px-4 py-2.5 text-xs select-none shadow-md animate-fade-in">
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-forest-500/15 pb-1.5">
        <div className="flex items-center gap-2">
          <span className="font-bold text-gold-300 font-bengali">{action.nameBn}</span>
          <span className="text-muted text-[11px]">({action.nameEn})</span>
        </div>
        <Badge tone={cancelled ? "crimson" : "emerald"}>
          {cancelled ? "ব্লক — বাতিল (Blocked)" : "সম্পন্ন (Completed)"}
        </Badge>
      </div>
      <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-muted text-[11px]">
        {result.coinsGained !== 0 && (
          <p className="text-forest-300 font-semibold">
            {result.coinsGained > 0 ? `+${result.coinsGained}` : result.coinsGained} Coins
          </p>
        )}
        {blocker && (
          <p>
            Blocker: <strong className="text-ivory">{blocker.displayName ?? blocker.username}</strong>
          </p>
        )}
        {loser && (
          <p className="text-crimson-300">
            Influence Lost: <strong className="text-ivory">{loser.displayName ?? loser.username}</strong>
            {result.eliminated && " (Eliminated!)"}
          </p>
        )}
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
  const selfId = useAuthStore((s) => s.user)?.id ?? "";

  const [blockDialogOpen, setBlockDialogOpen] = useState(false);
  const [blockResultData, setBlockResultData] = useState<BlockResultData | null>(null);
  const [blockBusy, setBlockBusy] = useState(false);
  const [influenceLostOpen, setInfluenceLostOpen] = useState(false);
  const [influenceLostReason, setInfluenceLostReason] = useState("");
  const [eliminationPlayer, setEliminationPlayer] = useState<GamePlayer | null>(null);
  const [chronicleOpen, setChronicleOpen] = useState(false);

  const gameSnapshot = useRealtimeStore((s) => s.gameSnapshots[matchId]);
  const resyncRequested = useRealtimeStore((s) => s.resyncRequested[matchId]);
  const realtimeStatus = useRealtimeStore((s) => s.status);
  const consumeResyncRequest = useRealtimeStore((s) => s.consumeResyncRequest);
  const resetMatchState = useRealtimeStore((s) => s.resetMatchState);

  const appliedVersionRef = useRef<VersionKey | undefined>(undefined);
  const resyncInFlightRef = useRef(false);
  const wasConnectedRef = useRef(false);

  const load = useCallback(async () => GameService.getGameState(matchId), [matchId]);

  const adoptState = useCallback((state: GameState) => {
    appliedVersionRef.current = { stateVersion: state.stateVersion ?? 0, scope: "private" };
    setGame(state);
  }, []);

  const requestResyncNow = useCallback(() => {
    if (resyncInFlightRef.current) return;
    resyncInFlightRef.current = true;
    realtimeSocket.sendSync(matchId, `resync-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`, appliedVersionRef.current?.stateVersion ?? 0);
    window.setTimeout(() => { resyncInFlightRef.current = false; }, 1500);
  }, [matchId]);

  useEffect(() => {
    let cancelled = false;
    load().then((result) => {
      if (cancelled) return;
      if (result.ok) { adoptState(result.data); setLoading(false); }
      else { setError(result.error.message); setLoading(false); }
    });
    return () => { cancelled = true; };
  }, [load, adoptState]);

  useEffect(() => {
    if (!gameSnapshot) return;
    const incoming: VersionKey = { stateVersion: gameSnapshot.stateVersion, scope: gameSnapshot.scope };
    if (isSuperseded(appliedVersionRef.current, incoming)) return;
    appliedVersionRef.current = incoming;
    setGame((prev) => {
      const next = toGameState(gameSnapshot.state);
      if (gameSnapshot.scope === "public" && prev) {
        const prevMe = prev.players.find((p) => p.userId === selfId);
        const hasRealCards = prevMe && prevMe.influenceCards.length > 0 && !prevMe.influenceCards[0].id.startsWith("hidden-");
        if (hasRealCards) {
          return { ...next, players: next.players.map((p) => {
            if (p.userId === selfId) return { ...p, influenceCards: prevMe.influenceCards.slice(0, p.influenceCards.length) };
            return p;
          }) };
        }
      }
      return next;
    });
  }, [gameSnapshot, selfId]);

  useEffect(() => {
    if (realtimeStatus === "connected" && !wasConnectedRef.current) requestResyncNow();
    wasConnectedRef.current = realtimeStatus === "connected";
  }, [realtimeStatus, requestResyncNow]);

  useEffect(() => {
    if (resyncRequested && consumeResyncRequest(matchId)) requestResyncNow();
  }, [resyncRequested, consumeResyncRequest, matchId, requestResyncNow]);

  useEffect(() => {
    return () => { resetMatchState(matchId); };
  }, [matchId, resetMatchState]);

  useMatchRealtime(matchId, !!matchId);

  const [matchTick, setMatchTick] = useState(0);
  useEffect(() => {
    return useRealtimeStore.subscribe((state, prev) => {
      if (state.lastDraw && state.lastDraw !== prev.lastDraw && state.lastDraw.matchId === matchId && state.lastDraw.playerId === selfId) {
        setMatchTick((t) => t + 1);
      }
    });
  }, [matchId, selfId]);

  useEffect(() => {
    if (matchTick === 0) return;
    let cancelled = false;
    load().then((result) => {
      if (cancelled) return;
      if (result.ok) adoptState(result.data);
    });
    return () => { cancelled = true; };
  }, [load, matchTick, adoptState]);

  async function retry() {
    setLoading(true); setError(null);
    const result = await load();
    if (result.ok) adoptState(result.data);
    else setError(result.error.message);
    setLoading(false);
  }

  async function handleAction(actionId: GameActionId, targetPlayerId?: string) {
    setBusy(actionId);
    const result = await GameService.performAction(matchId, {
      action: actionId,
      claimedCharacter: actionId === "tax" ? ("minister" as const) : actionId === "steal" ? ("dalal" as const) : undefined,
      targetPlayerId,
    });
    setBusy(null);
    if (result.ok) {
      adoptState(result.data);
      success(`${getAction(actionId).nameBn} — অ্যাকশন চলছে`);
      if (actionId === "coup" && targetPlayerId) {
        const target = result.data.players.find((p) => p.id === targetPlayerId);
        if (target) { setInfluenceLostReason(`ক্ষমতা দখল (Coup) আক্রমণের শিকার হওয়ায় ১টি ইনফ্লুয়েন্স হারাচ্ছেন`); setInfluenceLostOpen(true); }
      }
    } else { notifyError(result.error.message); }
  }

  async function handleBlockSubmit(claimed: CharacterId) {
    setBlockBusy(true);
    const result = await GameService.block(matchId, claimed);
    setBlockBusy(false);
    if (result.ok) { adoptState(result.data); success("ব্লক দাবি জমা হয়েছে — অ্যাকশনটি ব্লক করা হয়েছে"); }
    else { notifyError(result.error.message); }
  }

  async function handleBlockChallenge() {
    if (!game) return;
    setBlockBusy(true);
    const result = await GameService.challenge(matchId);
    setBlockBusy(false);
    if (result.ok) {
      adoptState(result.data);
      const res = result.data.pendingChallenge;
      if (res?.blockClaim) {
        const challenger = result.data.players.find((p) => p.userId === selfId) ?? result.data.players[0]!;
        const blocker = blockEvent?.blocker ?? challenger;
        const outcome: BlockOutcome = res.result === "failed" ? "challenger_loses_influence" : "block_claim_is_bluff";
        setBlockResultData({ outcome, blocker, challenger, claimedCharacter: res.claimedCharacter, actionId: blockEvent?.actionId ?? result.data.activeAction?.action ?? "income" });
        if (outcome === "challenger_loses_influence") setInfluenceLostReason("ব্লকের বিরুদ্ধে করা চ্যালেঞ্জে ব্যর্থ হওয়ায় ১টি ইনফ্লুয়েন্স হারাচ্ছেন");
        else setInfluenceLostReason("ব্লকের মিথ্যা দাবি ফাঁস হওয়ায় ব্লকার ১টি ইনফ্লুয়েন্স হারাচ্ছেন");
        setInfluenceLostOpen(true);
      }
      setBlockDialogOpen(false);
      success("ব্লক চ্যালেঞ্জ সম্পন্ন হয়েছে");
    } else { notifyError(result.error.message); }
  }

  async function resolvePendingAction() {
    if (!game?.activeAction) return;
    setBlockBusy(true);
    const result = await GameService.resolve(matchId);
    setBlockBusy(false);
    if (result.ok) { adoptState(result.data); setBlockDialogOpen(false); const cancelled = result.data.lastActionResult?.result === "CANCELLED"; success(cancelled ? "ব্লক গৃহীত হয়েছে — অ্যাকশন বাতিল" : "অ্যাকশন সমাধান সম্পন্ন হয়েছে"); }
    else { notifyError(result.error.message); }
  }

  function handleConfirmCardReveal(revealedCardId: string) {
    if (!game) return;
    setInfluenceLostOpen(false);
    const updatedPlayers = game.players.map((p) => {
      const hasCard = p.influenceCards.some((c) => c.id === revealedCardId);
      if (!hasCard) return p;
      const updatedCards = p.influenceCards.map((c) => c.id === revealedCardId ? { ...c, revealed: true } : c);
      const remainingHidden = updatedCards.filter((c) => !c.revealed).length;
      const isAlive = remainingHidden > 0;
      if (!isAlive && p.isAlive) setEliminationPlayer({ ...p, isAlive: false, influenceCards: updatedCards });
      return { ...p, isAlive, influenceCards: updatedCards };
    });
    setGame({ ...game, players: updatedPlayers, revealedCardsCount: game.revealedCardsCount + 1 });
    success("কার্ড সফলভাবে উন্মোচিত হয়েছে");
  }

  const blockEvent = (() => {
    const act = game?.activeAction;
    if (!game?.currentTurnPlayerId || !act?.blockerUserId) return null;
    const blocker = game.players.find((p) => p.id === act.blockerUserId);
    const actor = game.players.find((p) => p.id === game.currentTurnPlayerId);
    if (!blocker || !actor) return null;
    const fallback: CharacterId = act.action === "foreign_aid" ? "minister" : act.action === "assassinate" ? "goyenda" : "dalal";
    return { blocker, targetOrActor: actor, actionId: act.action, claimedCharacter: act.blockedCharacter ?? fallback };
  })();

  const showExchangeSelection = game?.status === MatchStatus.IN_PROGRESS && game?.activeAction?.action === "exchange" && (game?.exchangePool?.length ?? 0) > 0 && game?.currentTurnPlayerId === selfId;

  if (loading) return <div className="flex min-h-screen items-center justify-center bg-app px-4"><LoadingState label="Loading game board…" /></div>;
  if (error !== null || !game) return <div className="flex min-h-screen items-center justify-center bg-app px-4"><div className="w-full max-w-lg"><ErrorState title="Could not load game board" message={error ?? "Match state could not be found."} action={<Button variant="premium" onClick={retry}>Try Again</Button>} /></div></div>;
  if (game.status === MatchStatus.FINISHED) return <GameOverScreen game={game} selfId={selfId} />;

  const currentPlayer = game.players.find((p) => p.id === selfId) ?? game.players.find((p) => p.isTurn) ?? game.players[0];
  if (!currentPlayer) return <div className="flex min-h-screen items-center justify-center bg-app px-4"><EmptyState icon={<Trophy className="size-6" aria-hidden />} title="No Players" description="No players have joined this match yet." /></div>;

  const opponents = game.players.filter((p) => p.id !== currentPlayer.id).sort((a, b) => a.seatIndex - b.seatIndex);
  const blockWindowAction: GameActionId | null = game.activeAction && isBlockable(game.activeAction.action) ? game.activeAction.action : null;
  const isActorOfPending = game.currentTurnPlayerId === currentPlayer.id;
  const showBlockOffer = blockWindowAction !== null && !game.activeAction!.blockerUserId && !game.pendingChallenge && currentPlayer.isAlive && !isActorOfPending;
  const showActorResolve = blockWindowAction !== null && isActorOfPending && !game.activeAction!.blockerUserId;

  // ── Seating Distribution (matches reference screenshot layout) ──
  // Opponents sit to the LEFT (stacked) and TOP CENTER
  // 1 opp  → top
  // 2 opps → 1 left + 1 top
  // 3 opps → 2 left + 1 top          (matches screenshot with 3 bots)
  // 4 opps → 2 left + 1 top + 1 right
  // 5 opps → 2 left + 1 top + 2 right
  const numOpponents = opponents.length;
  let topOpponents: GamePlayer[] = [];
  let leftColOpponents: GamePlayer[] = [];
  let rightColOpponents: GamePlayer[] = [];

  if (numOpponents === 1) {
    topOpponents = [opponents[0]];
  } else if (numOpponents === 2) {
    leftColOpponents = [opponents[0]];
    topOpponents = [opponents[1]];
  } else if (numOpponents === 3) {
    leftColOpponents = [opponents[0], opponents[1]];
    topOpponents = [opponents[2]];
  } else if (numOpponents === 4) {
    leftColOpponents = [opponents[0], opponents[1]];
    topOpponents = [opponents[2]];
    rightColOpponents = [opponents[3]];
  } else {
    leftColOpponents = [opponents[0], opponents[1]];
    topOpponents = [opponents[2]];
    rightColOpponents = [opponents[3], opponents[4]];
  }

  const hasLeftCol = leftColOpponents.length > 0;
  const hasRightCol = rightColOpponents.length > 0;

  return (
    <div className="relative flex h-screen max-h-screen flex-col overflow-hidden select-none">
      {/* ── Layer 0: Cinematic Table Background ── */}
      <div
        className="pointer-events-none fixed inset-0 z-0 bg-cover bg-center bg-no-repeat"
        style={{ backgroundImage: 'url("/assets/game/rajneeti-table-bg.png.png")' }}
        aria-hidden="true"
      />
      <div
        className="pointer-events-none fixed inset-0 z-0"
        style={{
          background:
            "radial-gradient(ellipse 85% 70% at 50% 48%, rgba(8,58,41,0.18) 0%, rgba(3,19,14,0.55) 100%), " +
            "linear-gradient(180deg, rgba(3,19,14,0.32) 0%, rgba(2,12,9,0.50) 100%)",
          boxShadow: "inset 0 0 140px 40px rgba(0,0,0,0.5)",
        }}
        aria-hidden="true"
      />

      {/* ── Header ── */}
      <GameHeader
        game={game}
        selfId={selfId}
        onToggleChronicle={() => setChronicleOpen((prev) => !prev)}
        isChronicleOpen={chronicleOpen}
        chronicleCount={game.log.length}
      />

      {/* ── Main Arena: Table + Chronicle ── */}
      <div className="relative flex flex-1 min-h-0 overflow-hidden z-10">

        {/* ── LEFT COLUMN: Opponents stacked vertically ── */}
        {hasLeftCol && (
          <div className="hidden lg:flex flex-col justify-center gap-3 p-3 w-52 xl:w-56 shrink-0">
            {leftColOpponents.map((player) => (
              <OpponentSeat key={player.id} player={player} />
            ))}
          </div>
        )}

        {/* ── CENTER: The Game Table ── */}
        <div className="flex flex-1 flex-col min-w-0">

          {/* TOP: Top opponent(s) + mobile scrollable row */}
          <div className="shrink-0 flex justify-center items-start pt-2 px-2 gap-3">
            {/* Mobile: all opponents in a scrollable row */}
            <div className="flex lg:hidden items-center justify-start gap-2 overflow-x-auto pb-1 w-full max-w-full">
              {opponents.map((player) => (
                <div key={player.id} className="min-w-[180px] max-w-[220px] shrink-0">
                  <OpponentSeat player={player} compact />
                </div>
              ))}
            </div>
            {/* Desktop: top center seat(s) */}
            <div className="hidden lg:flex items-center justify-center gap-4">
              {topOpponents.map((player) => (
                <div key={player.id} className="w-56 xl:w-60">
                  <OpponentSeat player={player} />
                </div>
              ))}
              {topOpponents.length === 0 && (
                <p className="text-xs text-muted/50 py-2 font-bengali">একক খেলা চলছে…</p>
              )}
            </div>
          </div>

          {/* CENTER ARENA: Active Action + Deck/Discard + overlays */}
          <div className="flex flex-1 items-center justify-center gap-4 px-3 min-h-0 py-2">
            {/* Action column */}
            <div className="flex flex-col items-center gap-2 w-full max-w-md">
              {blockResultData && (
                <BlockResult result={blockResultData} onDismiss={() => setBlockResultData(null)} />
              )}
              {game.lastActionResult ? (
                <ActionResultSummary result={game.lastActionResult} players={game.players} />
              ) : null}
              {blockEvent ? (
                <BlockPanel activeBlock={blockEvent} currentPlayer={currentPlayer} onOpenDialog={() => setBlockDialogOpen(true)} />
              ) : null}
              {showBlockOffer && blockWindowAction ? (
                <BlockOffer action={blockWindowAction} busy={blockBusy} onBlock={handleBlockSubmit} />
              ) : null}
              {showActorResolve && blockWindowAction ? (
                <BlockWindowPanel action={blockWindowAction} busy={blockBusy} onResolve={() => void resolvePendingAction()} />
              ) : null}

              {/* Active Action focal card */}
              <div className="w-full">
                <ActiveAction game={game} />
              </div>

              {/* Challenge Resolution */}
              {game.status === MatchStatus.IN_PROGRESS ? (
                <ChallengeFlow game={game} selfId={selfId} onResolved={adoptState} />
              ) : null}
            </div>

            {/* Deck / Discard stacks */}
            <DeckDiscard game={game} />
          </div>

          {/* BOTTOM STATION: Player pod (left) + Own Cards (center) + spacer (right) */}
          <div className="shrink-0 flex items-end justify-between gap-3 px-3 pb-1">
            {/* Player Info Pod — bottom left */}
            <div className="w-52 xl:w-56 shrink-0">
              <PlayerSeat player={currentPlayer} />
            </div>

            {/* Own Influence Cards — bottom center */}
            <div className="flex-1 flex justify-center">
              <OwnCards cards={currentPlayer.influenceCards} />
            </div>

            {/* Balancing spacer equal to player pod width */}
            <div className="w-52 xl:w-56 shrink-0" />
          </div>

          {/* ACTION DOCK — full width at bottom */}
          <div className="shrink-0 px-2 pb-2">
            <ActionPanel
              player={currentPlayer}
              opponents={opponents}
              busy={busy !== null}
              onAction={(actionId, targetPlayerId) => void handleAction(actionId, targetPlayerId)}
              className="w-full"
            />
          </div>
        </div>

        {/* ── RIGHT COLUMN: Opponents (4-5 player games) ── */}
        {hasRightCol && (
          <div className="hidden lg:flex flex-col justify-center gap-3 p-3 w-52 xl:w-56 shrink-0">
            {rightColOpponents.map((player) => (
              <OpponentSeat key={player.id} player={player} />
            ))}
          </div>
        )}

        {/* ── CHRONICLE PANEL: Inline right panel (not a drawer) ── */}
        {chronicleOpen && (
          <aside
            className={cn(
              "hidden lg:flex w-64 xl:w-72 shrink-0 flex-col border-l border-forest-500/25",
              "bg-[#03120d]/92 backdrop-blur-xl",
            )}
            aria-label="Match Chronicle"
          >
            <div className="flex items-center justify-between border-b border-forest-500/20 px-4 py-3 bg-[#020d09]/70 shrink-0">
              <div className="flex items-center gap-2">
                <ScrollText className="size-4 text-gold-400" aria-hidden />
                <h2 className="font-cinzel text-xs font-bold uppercase tracking-widest text-ivory">
                  Game Log
                </h2>
                <span className="rounded px-1.5 py-0.5 font-mono text-[10px] font-bold bg-gold-500/15 text-gold-300 border border-gold-500/25">
                  {game.log.length}
                </span>
              </div>
              <button
                type="button"
                onClick={() => setChronicleOpen(false)}
                className="rounded-lg p-1.5 text-muted hover:text-ivory hover:bg-forest-900/40 transition-colors cursor-pointer"
                aria-label="Close Chronicle"
              >
                <X className="size-4" aria-hidden />
              </button>
            </div>
            <div className="flex-1 overflow-hidden">
              <GameLog entries={game.log} />
            </div>
          </aside>
        )}

        {/* Mobile Chronicle: slide-over drawer */}
        {chronicleOpen && (
          <>
            <div
              className="fixed inset-0 z-40 bg-black/60 backdrop-blur-xs lg:hidden"
              onClick={() => setChronicleOpen(false)}
              aria-hidden
            />
            <aside
              className="fixed inset-y-0 right-0 z-50 flex w-full max-w-[340px] flex-col border-l border-forest-500/25 bg-[#04150f]/97 backdrop-blur-2xl lg:hidden"
            >
              <div className="flex items-center justify-between border-b border-forest-500/20 px-4 py-3">
                <div className="flex items-center gap-2">
                  <ScrollText className="size-4 text-gold-400" aria-hidden />
                  <h2 className="font-cinzel text-xs font-bold uppercase tracking-widest text-ivory">
                    Game Log ({game.log.length})
                  </h2>
                </div>
                <button
                  type="button"
                  onClick={() => setChronicleOpen(false)}
                  className="rounded-lg p-1.5 text-muted hover:text-ivory hover:bg-forest-900/40 transition-colors cursor-pointer"
                  aria-label="Close Chronicle"
                >
                  <X className="size-4" aria-hidden />
                </button>
              </div>
              <div className="flex-1 overflow-hidden p-2">
                <GameLog entries={game.log} />
              </div>
            </aside>
          </>
        )}
      </div>

      {/* Action Decision Modals & Dialogs */}
      {showExchangeSelection ? (
        <ExchangeSelection matchId={game.matchId} cards={game.exchangePool!} onResolved={adoptState} />
      ) : null}
      {blockEvent ? (
        <BlockDialog
          event={blockEvent}
          currentPlayer={currentPlayer}
          open={blockDialogOpen}
          busy={blockBusy}
          onClose={() => setBlockDialogOpen(false)}
          onAllowBlock={() => void resolvePendingAction()}
          onChallenge={() => void handleBlockChallenge()}
        />
      ) : null}
      <InfluenceLostModal
        open={influenceLostOpen}
        player={currentPlayer}
        reason={influenceLostReason}
        onConfirmReveal={handleConfirmCardReveal}
      />
      {eliminationPlayer ? (
        <EliminationOverlay
          open={eliminationPlayer !== null}
          eliminatedPlayer={eliminationPlayer}
          onFinish={() => setEliminationPlayer(null)}
        />
      ) : null}
    </div>
  );
}