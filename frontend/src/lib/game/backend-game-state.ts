import type {
  BackendGameState,
  BackendGamePlayer,
  BackendPendingAction,
} from "@/types/backend";
import type { BotDifficulty } from "@/types/room";
import {
  MatchStatus,
  type ActionIntent,
  type ActionResult,
  type ChallengeResolution,
  type GameActionId,
  type GameLogEntry,
  type GamePhase,
  type GameState,
  type InfluenceCard,
} from "@/types/game";

const CHARACTER_IDS = ["minister", "ghatok", "dalal", "amla", "goyenda"] as const;

type CharacterId = (typeof CHARACTER_IDS)[number];

const BOT_DIFFICULTIES = ["EASY", "MEDIUM", "HARD"] as const;

function toBotDifficulty(value?: string): BotDifficulty | undefined {
  return BOT_DIFFICULTIES.includes(value as (typeof BOT_DIFFICULTIES)[number])
    ? (value as BotDifficulty)
    : undefined;
}

function toCharacterId(value: string): CharacterId {
  return CHARACTER_IDS.includes(value as CharacterId) ? (value as CharacterId) : "minister";
}

function toActionId(type: string): GameActionId {
  switch (type) {
    case "EXCHANGE":
      return "exchange";
    case "FOREIGN_AID":
      return "foreign_aid";
    case "ASSASSINATE":
      return "assassinate";
    case "TAX":
      return "tax";
    case "STEAL":
      return "steal";
    default:
      return "income";
  }
}

function mapStatus(status: BackendGameState["status"]): MatchStatus {
  switch (status) {
    case "FINISHED":
      return MatchStatus.FINISHED;
    case "CANCELLED":
      return MatchStatus.ABANDONED;
    case "CREATED":
    case "IN_PROGRESS":
    default:
      return MatchStatus.IN_PROGRESS;
  }
}

function mapPhase(phase: BackendGameState["phase"]): GamePhase {
  switch (phase) {
    case "setup":
      return "setup";
    case "game_over":
      return "game_over";
    case "in_progress":
    default:
      return "action_selection";
  }
}

/**
 * Module 18 — derives the frontend phase (and the challenge state) from the
 * server response. A pending action that claims a character is in its
 * block/challenge window; a resolved challenge is shown until the next action.
 */
function mapChallengeResolution(backend: BackendGameState): ChallengeResolution | null {
  const challenge = backend.lastChallenge;
  if (!challenge) return null;

  const claimedCharacter = challenge.claimedCharacter
    ? toCharacterId(challenge.claimedCharacter)
    : toCharacterId(
        backend.pendingAction?.claimedCharacter ?? "minister",
      );

  return {
    challengerId: challenge.challengerId,
    claimantId: challenge.claimantId,
    claimedCharacter,
    result: challenge.result === "CLAIM_TRUE" ? "failed" : "success",
    revealedCardId: challenge.revealedCardId,
    revealedCharacterId: challenge.revealedCharacterId
      ? toCharacterId(challenge.revealedCharacterId)
      : undefined,
    influenceLostById: challenge.influenceLostById,
    actionContinues: challenge.actionContinues,
    blockClaim: challenge.blockClaim,
    effectApplied: challenge.actionContinues,
  };
}

const LOG_KINDS: GameLogEntry["kind"][] = ["info", "action", "challenge", "block", "reveal", "elimination"];

/**
 * Module 20 — projects the authoritative Action Resolver verdict onto the
 * frontend model (null when no action has been closed yet).
 */
function mapActionResult(backend: BackendGameState["lastActionResult"]): ActionResult | null {
  if (!backend) return null;

  return {
    actionType: toActionId(backend.actionType),
    result: backend.result,
    actorUserId: backend.actorUserId,
    coinsGained: backend.coinsGained,
    coinsLost: backend.coinsLost,
    blockedByUserId: backend.blockedByUserId,
    blockedCharacter: backend.blockedCharacter
      ? toCharacterId(backend.blockedCharacter)
      : undefined,
    claimChallenged: backend.claimChallenged,
    influenceLostById: backend.influenceLostById,
    eliminated: backend.eliminated,
    nextTurnPlayerId: backend.nextTurnPlayerId,
    nextTurnNumber: backend.nextTurnNumber,
  };
}

function toInfluenceCards(player: BackendGamePlayer): InfluenceCard[] {
  const hidden: InfluenceCard[] = Array.from(
    { length: Math.max(0, player.influenceCount) },
    (_, i) => ({
      id: `hidden-${player.userId}-${i}`,
      characterId: "minister",
      revealed: false,
    }),
  );

  const seenIds = new Set<string>();
  const own: InfluenceCard[] = [];
  for (const card of player.cards ?? []) {
    if (card?.cardId && !seenIds.has(card.cardId)) {
      seenIds.add(card.cardId);
      own.push({
        id: card.cardId,
        characterId: toCharacterId(card.characterId),
        revealed: false,
      });
    }
  }

  // The backend only ever sends real cards for the requesting player.
  return own.length > 0 ? own : hidden;
}

/**
 * Module 19 — derives the frontend phase (and the block state) from the server
 * response. A pending block claim puts the game in the block window; a block
 * that has not been challenged yet keeps the action pending.
 */
function toPhase(backend: BackendGameState): GamePhase {
  if (backend.lastChallenge) return "challenge_resolution";
  if (backend.pendingAction?.blockerUserId) return "block_resolution";
  if (backend.pendingAction?.claimedCharacter) return "action_resolution";
  return mapPhase(backend.phase);
}

function mapPendingAction(pending: BackendPendingAction | undefined): ActionIntent | null {
  if (!pending) return null;
  const action = toActionId(pending.type);
  const intent: ActionIntent = {
    action,
    claimedCharacter: pending.claimedCharacter
      ? toCharacterId(pending.claimedCharacter)
      : action === "exchange"
      ? "amla"
      : action === "assassinate"
      ? "ghatok"
      : undefined,
  };
  if (pending.targetPlayerId) {
    intent.targetPlayerId = pending.targetPlayerId;
  }
  if (pending.blockerUserId) {
    intent.blockerUserId = pending.blockerUserId;
    intent.blockedCharacter = pending.blockedCharacter
      ? toCharacterId(pending.blockedCharacter)
      : undefined;
  }
  return intent;
}

/**
 * Module 23 — projects a full backend game state (REST response or a realtime
 * {@code STATE_UPDATED}/{@code PRIVATE_STATE} snapshot) onto the frontend model.
 */
export function toGameState(backend: BackendGameState): GameState {
  const currentTurnPlayerId = backend.currentTurnPlayerId ?? null;

  const players = backend.players.map((player) => ({
    id: player.userId,
    userId: player.userId,
    username: player.username,
    displayName: player.username,
    isHost: player.host,
    isAlive: player.alive,
    isTurn: player.turn,
    isBot: player.isBot ?? false,
    botDifficulty: toBotDifficulty(player.botDifficulty),
    coins: player.coins,
    influenceCards: toInfluenceCards(player),
    seatIndex: player.seatIndex,
  }));

  const exchangePool: InfluenceCard[] | undefined =
    backend.pendingAction?.type === "EXCHANGE" && backend.pendingAction.exchangePool
      ? backend.pendingAction.exchangePool.map((card) => ({
          id: card.cardId,
          characterId: toCharacterId(card.characterId),
          revealed: false,
        }))
      : undefined;

  return {
    matchId: backend.matchId,
    roomId: backend.roomId,
    status: mapStatus(backend.status),
    phase: toPhase(backend),
    players,
    currentTurnPlayerId,
    turnOrder: backend.turnOrder ?? players.map((p) => p.id),
    turnNumber: backend.turnNumber,
    deckCount: backend.deckCount,
    revealedCardsCount: backend.revealedCardsCount,
    winnerPlayerId: backend.winnerUserId ?? null,
    activeAction: mapPendingAction(backend.pendingAction),
    pendingChallenge: mapChallengeResolution(backend),
    pendingBlock: null,
    lastActionResult: mapActionResult(backend.lastActionResult),
    exchangePool,
    log: backend.log.map((entry) => ({
      id: entry.id,
      timestamp: entry.timestamp,
      text: entry.text,
      kind: LOG_KINDS.includes(entry.kind as GameLogEntry["kind"]) ? (entry.kind as GameLogEntry["kind"]) : "info",
    })),
    stateVersion: backend.stateVersion,
    startedAt: backend.startedAt,
    endedAt: backend.endedAt,
  };
}