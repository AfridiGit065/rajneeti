"use client";

import { useId } from "react";
import { cn } from "@/lib/cn";
import { BlockOffer, BlockPanel, BlockWindowPanel, BlockResult, type BlockEvent, type BlockResultData } from "./block-system";
import { ChallengeFlow } from "./challenge";
import type { CharacterId } from "@/types/character";
import type { GameActionId, GamePlayer, GameState } from "@/types/game";
import { MatchStatus } from "@/types/game";

export interface SideReactionPanelProps {
  game: GameState;
  selfId: string;
  currentPlayer: GamePlayer;
  dockSide?: "right" | "left";
  blockResultData: BlockResultData | null;
  onDismissBlockResult: () => void;
  blockEvent: BlockEvent | null;
  onOpenBlockDialog: () => void;
  showBlockOffer: boolean;
  blockWindowAction: GameActionId | null;
  blockBusy: boolean;
  onBlockSubmit: (claimedCharacter: CharacterId) => void;
  showActorResolve: boolean;
  onResolvePendingAction: () => void;
  isChallengeActive: boolean;
  onAdoptState: (state: GameState) => void;
  onChallengePanelHandled: (key: string) => void;
  onChallengeFlowStarted: (key: string) => void;
  onChallengeFlowSettled: () => void;
  className?: string;
}

/**
 * Reusable docked reaction panel for all reaction / decision windows:
 * - Block Opportunity (React to Steal, Foreign Aid, Assassination)
 * - Block Window Panel (Waiting for reactions / Resolve)
 * - Block Panel (React / Challenge opponent block)
 * - Block Result outcome card
 * - Challenge Opportunity & Flow (React to Tax/Minister, Steal/Dalal, etc.)
 *
 * Desktop:
 * - Positioned on the designated side (prefer right, or left if unavailable)
 * - Vertically centered in the playable board area
 * - Never overlaps the center Active Action card
 * - Never pushes the game board downward
 *
 * Mobile:
 * - Rendered as a compact, touch-friendly bottom sheet
 * - Respects safe area insets
 * - Prevents horizontal overflow
 */
export function SideReactionPanel({
  game,
  selfId,
  currentPlayer,
  dockSide = "right",
  blockResultData,
  onDismissBlockResult,
  blockEvent,
  onOpenBlockDialog,
  showBlockOffer,
  blockWindowAction,
  blockBusy,
  onBlockSubmit,
  showActorResolve,
  onResolvePendingAction,
  isChallengeActive,
  onAdoptState,
  onChallengePanelHandled,
  onChallengeFlowStarted,
  onChallengeFlowSettled,
  className,
}: SideReactionPanelProps) {
  const panelRegionId = useId();

  const hasBlockOffer = Boolean(showBlockOffer && blockWindowAction && !blockEvent);
  const hasBlockResolve = Boolean(showActorResolve && blockWindowAction && !blockEvent);
  const hasBlockEvent = Boolean(blockEvent);
  const hasBlockResult = Boolean(blockResultData);
  const hasChallenge = Boolean(
    isChallengeActive &&
      (game.status === MatchStatus.IN_PROGRESS || game.status === MatchStatus.WAITING)
  );

  const hasContent =
    hasBlockResult || hasBlockEvent || hasBlockOffer || hasBlockResolve || hasChallenge;

  if (!hasContent) {
    return null;
  }

  const content = (
    <div className="flex flex-col gap-3 w-full">
      {/* 1. Block Result Outcome Card */}
      {blockResultData && (
        <div className="w-full animate-fade-in">
          <BlockResult result={blockResultData} onDismiss={onDismissBlockResult} />
        </div>
      )}

      {/* 2. Opponent Block Claim Panel */}
      {blockEvent && (
        <div className="w-full animate-fade-in">
          <BlockPanel
            activeBlock={blockEvent}
            currentPlayer={currentPlayer}
            onOpenDialog={onOpenBlockDialog}
          />
        </div>
      )}

      {/* 3. Block Opportunity Offer (e.g. React to Steal, Foreign Aid, Assassination) */}
      {hasBlockOffer && blockWindowAction && (
        <div className="w-full animate-fade-in">
          <BlockOffer
            action={blockWindowAction}
            busy={blockBusy}
            onBlock={onBlockSubmit}
          />
        </div>
      )}

      {/* 4. Actor Resolve Action Window */}
      {hasBlockResolve && blockWindowAction && (
        <div className="w-full animate-fade-in">
          <BlockWindowPanel
            action={blockWindowAction}
            busy={blockBusy}
            onResolve={onResolvePendingAction}
          />
        </div>
      )}

      {/* 5. Challenge Opportunity & Flow Window */}
      {hasChallenge && (
        <div className="w-full animate-fade-in">
          <ChallengeFlow
            game={game}
            selfId={selfId}
            onResolved={onAdoptState}
            onPanelHandled={onChallengePanelHandled}
            onFlowStarted={onChallengeFlowStarted}
            onFlowSettled={onChallengeFlowSettled}
          />
        </div>
      )}
    </div>
  );

  return (
    <>
      {/* ── DESKTOP DOCKED SIDE PANEL (md and up) ── */}
      <aside
        id={`reaction-panel-desktop-${panelRegionId}`}
        aria-label="Reaction and Decision Controls"
        className={cn(
          "hidden md:flex flex-col justify-center pointer-events-auto select-none absolute z-30 transition-all duration-300",
          dockSide === "right"
            ? "right-3 lg:right-6 xl:right-10"
            : "left-3 lg:left-6 xl:left-10",
          "top-[42%] -translate-y-1/2",
          "w-[340px] lg:w-[380px] xl:w-[400px] max-w-[calc(50vw-250px)]",
          "max-h-[60vh] overflow-y-auto",
          className,
        )}
      >
        {content}
      </aside>

      {/* ── MOBILE COMPACT BOTTOM SHEET (< md) ── */}
      <section
        id={`reaction-panel-mobile-${panelRegionId}`}
        aria-label="Mobile Reaction and Decision Controls"
        className={cn(
          "md:hidden fixed inset-x-0 bottom-0 z-40 flex flex-col pointer-events-auto select-none",
          "max-w-md mx-auto px-3.5 pt-3 pb-[calc(env(safe-area-inset-bottom,0px)+12px)]",
          "bg-deep-950/95 backdrop-blur-md border-t border-gold-500/35 rounded-t-2xl shadow-[0_-8px_30px_rgba(0,0,0,0.8)]",
          "max-h-[50vh] overflow-y-auto animate-slide-up",
          className,
        )}
      >
        <div className="mx-auto mb-2 h-1 w-10 rounded-full bg-white/20 shrink-0" aria-hidden />
        {content}
      </section>
    </>
  );
}
