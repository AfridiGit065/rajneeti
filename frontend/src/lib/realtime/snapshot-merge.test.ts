import { test } from "node:test";
import assert from "node:assert/strict";
import { mergeSnapshotIntoGame } from "@/lib/realtime/snapshot-merge";
import type { ExchangeSelectionGateInput } from "@/lib/game/exchange-selection";
import { MatchStatus, type ActionIntent, type GamePlayer, type GameState } from "@/types/game";

const SELF = "self-user";
const OPPONENT = "bot-user";
const POOL = [
  { id: "card-1", characterId: "goyenda" as const, revealed: false },
  { id: "card-2", characterId: "goyenda" as const, revealed: false },
  { id: "card-3", characterId: "ghatok" as const, revealed: false },
  { id: "card-4", characterId: "minister" as const, revealed: false },
];
const EXCHANGE: ActionIntent = { action: "exchange", claimedCharacter: "amla" };

function player(
  userId: string,
  overrides: Partial<GamePlayer> = {},
): GamePlayer {
  return {
    id: userId,
    userId,
    username: userId,
    displayName: userId,
    isHost: userId === SELF,
    isAlive: true,
    isTurn: userId === SELF,
    coins: 1,
    influenceCards: [],
    seatIndex: userId === SELF ? 0 : 1,
    ...overrides,
  };
}

function state(overrides: Partial<GameState> = {}): GameState {
  return {
    matchId: "m1",
    roomId: "r1",
    status: MatchStatus.WAITING,
    phase: "action_resolution",
    players: [player(SELF), player(OPPONENT)],
    currentTurnPlayerId: SELF,
    turnOrder: [SELF, OPPONENT],
    turnNumber: 3,
    deckCount: 9,
    revealedCardsCount: 0,
    winnerPlayerId: null,
    activeAction: null,
    pendingChallenge: null,
    pendingBlock: null,
    lastActionResult: null,
    log: [],
    startedAt: "2026-01-01T00:00:00Z",
    ...overrides,
  };
}

/** A public broadcast: no pool, own hand replaced by hidden placeholders. */
function publicSnapshot(overrides: Partial<GameState> = {}): GameState {
  return state({
    activeAction: EXCHANGE,
    players: [
      player(SELF, {
        influenceCards: Array.from({ length: 4 }, (_unused, index) => ({
          id: `hidden-${SELF}-${index + 1}`,
          characterId: "goyenda" as const,
          revealed: false,
        })),
      }),
      player(OPPONENT, {
        influenceCards: Array.from({ length: 2 }, (_unused, index) => ({
          id: `hidden-${OPPONENT}-${index + 1}`,
          characterId: "dalal" as const,
          revealed: false,
        })),
      }),
    ],
    ...overrides,
  });
}

test("keeps the exchange pool across a public snapshot while the action is pending", () => {
  const prev = state({
    activeAction: EXCHANGE,
    exchangePool: POOL,
    players: [player(SELF, { influenceCards: POOL }), player(OPPONENT)],
  });

  const merged = mergeSnapshotIntoGame(prev, publicSnapshot(), SELF, "public");

  assert.deepEqual(merged.exchangePool, POOL);
  assert.equal(
    merged.exchangePool?.length,
    4,
    "the pick UI needs all 4 offered cards",
  );
});

test("keeps the visible own hand so the pool matches what the player sees", () => {
  const prev = state({
    activeAction: EXCHANGE,
    exchangePool: POOL,
    players: [player(SELF, { influenceCards: POOL }), player(OPPONENT)],
  });

  const merged = mergeSnapshotIntoGame(prev, publicSnapshot(), SELF, "public");

  const me = merged.players.find((p) => p.userId === SELF);
  assert.deepEqual(me?.influenceCards.map((c) => c.id), POOL.map((c) => c.id));
});

test("drops the pool once the exchange action resolved", () => {
  const prev = state({
    activeAction: EXCHANGE,
    exchangePool: POOL,
    players: [player(SELF, { influenceCards: POOL }), player(OPPONENT)],
  });
  const resolved = publicSnapshot({ activeAction: null });

  const merged = mergeSnapshotIntoGame(prev, resolved, SELF, "public");

  assert.equal(merged.exchangePool, undefined);
});

test("drops the pool when another action took over", () => {
  const prev = state({
    activeAction: EXCHANGE,
    exchangePool: POOL,
    players: [player(SELF, { influenceCards: POOL }), player(OPPONENT)],
  });
  const steal = publicSnapshot({
    activeAction: { action: "steal", targetPlayerId: OPPONENT },
  });

  const merged = mergeSnapshotIntoGame(prev, steal, SELF, "public");

  assert.equal(merged.exchangePool, undefined);
});

test("a private snapshot is authoritative and applied as-is", () => {
  const prev = state({
    activeAction: EXCHANGE,
    exchangePool: POOL,
    players: [player(SELF, { influenceCards: POOL }), player(OPPONENT)],
  });
  const incoming = state({ activeAction: null, currentTurnPlayerId: OPPONENT });

  const merged = mergeSnapshotIntoGame(prev, incoming, SELF, "private");

  assert.equal(merged, incoming);
  assert.equal(merged.exchangePool, undefined);
});

test("the merge keeps the selection gate open through a broadcast", () => {
  const prev = state({
    activeAction: EXCHANGE,
    exchangePool: POOL,
    players: [player(SELF, { influenceCards: POOL }), player(OPPONENT)],
  });

  const merged = mergeSnapshotIntoGame(prev, publicSnapshot(), SELF, "public");

  const gate: ExchangeSelectionGateInput = {
    status: merged.status,
    activeAction: merged.activeAction,
    exchangePool: merged.exchangePool,
    currentTurnPlayerId: merged.currentTurnPlayerId,
  };
  assert.equal(gate.activeAction?.action, "exchange");
  assert.equal(gate.exchangePool?.length, 4);
});
