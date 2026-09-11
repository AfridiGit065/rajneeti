"use client";

import { useEffect, useMemo, useState } from "react";
import { canChallenge } from "@/lib/game/actions";
import { challengeToSimulation, simulateChallenge, type ChallengeSimulation } from "@/lib/game/challenge";
import { GameService } from "@/services/game-service";
import { Loader2, Gavel } from "@/components/ui/icons";
import { ChallengePanel } from "./challenge-panel";
import { ChallengeDialog } from "./challenge-dialog";
import { ChallengeResult } from "./challenge-result";
import { CardReveal } from "./card-reveal";
import type { GameState } from "@/types/game";
import type { CharacterId } from "@/types/character";

type FlowStage =
  | "idle"
  | "panel"
  | "confirm"
  | "resolving"
  | "result"
  | "reveal"
  | "replacement";

interface ChallengeFlowProps {
  game: GameState;
  selfId: string;
  onResolved?: (next: GameState) => void;
}

const STAGE_DURATION_MS: Partial<Record<FlowStage, number>> = {
  resolving: 1400,
  result: 2400,
  reveal: 2100,
  replacement: 1900,
};

/** Next stage in the reveal sequence, or "idle" when the sequence is over. */
const STAGE_NEXT: Partial<Record<FlowStage, FlowStage>> = {
  resolving: "result",
  result: "reveal",
  reveal: "replacement",
  replacement: "idle",
};

/**
 * Drives the challenge system for claims made by opponents. The verdict comes
 * from the backend Challenge Manager (returned as `lastChallenge`); the local
 * mock is kept only as a fallback when the server response is unavailable.
 */
export function ChallengeFlow({ game, selfId, onResolved }: ChallengeFlowProps) {
  const [stage, setStage] = useState<FlowStage>("idle");
  const [simulation, setSimulation] = useState<ChallengeSimulation | null>(null);
  const [handledKey, setHandledKey] = useState<string | null>(null);

  const self = useMemo(
    () => game.players.find((player) => player.userId === selfId),
    [game.players, selfId],
  );
  const claimant = useMemo(
    () => game.players.find((player) => player.id === game.currentTurnPlayerId),
    [game.players, game.currentTurnPlayerId],
  );

  const activeClaim = game.activeAction;
  const showPanel = (() => {
    if (!activeClaim?.claimedCharacter) return false;
    if (game.phase !== "action_resolution") return false;
    if (!claimant || claimant.userId === selfId) return false;
    return canChallenge(activeClaim.action);
  })();
  const claimKey = showPanel
    ? `${activeClaim!.action}:${claimant!.id}:${game.turnNumber}`
    : null;
  const panelVisible = claimKey !== null && handledKey !== claimKey;

  const view: FlowStage =
    stage === "idle" ? (panelVisible ? "panel" : "idle") : stage;

  useEffect(() => {
    if (stage === "idle" || stage === "panel" || stage === "confirm") return;
    const next = STAGE_NEXT[stage];
    const delay = STAGE_DURATION_MS[stage] ?? 1800;
    const timer = window.setTimeout(
      () => setStage(next ?? "idle"),
      delay,
    );
    return () => window.clearTimeout(timer);
  }, [stage]);

  function handleChallenge() {
    if (!activeClaim?.claimedCharacter || !claimant || !claimKey) return;
    setHandledKey(claimKey);
    setSimulation(
      simulateChallenge(claimant.influenceCards, activeClaim.claimedCharacter),
    );
    setStage("confirm");
  }

  function handleAllow() {
    if (!claimKey) return;
    setHandledKey(claimKey);
    setStage("idle");
  }

  function confirmChallenge() {
    if (!activeClaim?.claimedCharacter) return;
    void GameService.challenge(game.matchId).then((result) => {
      if (!result.ok) {
        setStage("panel");
        return;
      }
      const next = result.data;
      const verdict = next.pendingChallenge;
      setSimulation(
        verdict
          ? challengeToSimulation(verdict)
          : {
              verdict: "bluff",
              revealedCharacterId: (activeClaim.claimedCharacter ?? "minister") as CharacterId,
            },
      );
      onResolved?.(next);
    });
    setStage("resolving");
  }

  const sim = simulation;
  const claimantName = claimant?.displayName ?? claimant?.username ?? "দাবিকারী";
  const challengerName = self?.displayName ?? self?.username ?? "চ্যালেঞ্জকারী";

  return (
    <>
      {view === "panel" ? (
        <ChallengePanel
          key={claimKey ?? "claim"}
          claimant={claimant!}
          claimedCharacter={activeClaim!.claimedCharacter!}
          onChallenge={handleChallenge}
          onAllow={handleAllow}
        />
      ) : null}

      {view === "confirm" && sim ? (
        <ChallengeDialog
          open
          claimantName={claimantName}
          claimedCharacter={sim.revealedCharacterId}
          onConfirm={confirmChallenge}
          onCancel={() => setStage("panel")}
        />
      ) : null}

      {view === "resolving" ? (
        <div
          className="fixed inset-0 z-[60] flex flex-col items-center justify-center gap-4 bg-deep-950/90 px-4 backdrop-blur-md animate-fade-in"
          role="status"
          aria-label="Challenge Resolving"
        >
          <span className="relative flex size-16 items-center justify-center rounded-full border border-gold-500/40 bg-deep-900/80 shadow-gold">
            <Gavel className="size-8 text-gold-300" aria-hidden />
            <Loader2 className="absolute -right-1 -top-1 size-5 animate-spin text-crimson-300" aria-hidden />
          </span>
          <p className="font-cinzel text-xl font-bold text-ivory uppercase tracking-wider">Challenge Pending…</p>
          <p className="text-sm text-muted">Verifying claimed influence card</p>
        </div>
      ) : null}

      {view === "result" && sim ? (
        <ChallengeResult
          verdict={sim.verdict}
          challengerName={challengerName}
          claimantName={claimantName}
        />
      ) : null}

      {view === "reveal" && sim ? (
        <CardReveal characterId={sim.revealedCharacterId} variant="reveal" />
      ) : null}

      {view === "replacement" && sim ? (
        <CardReveal
          characterId={sim.revealedCharacterId}
          variant="replacement"
        />
      ) : null}
    </>
  );
}

