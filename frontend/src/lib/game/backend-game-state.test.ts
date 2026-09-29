import { test } from "node:test";
import assert from "node:assert/strict";
import { toGameState } from "./backend-game-state.ts";
import type { BackendGameState } from "@/types/backend";

/**
 * Module 18 — backend → frontend state projection.
 *
 * These pin the mapping decisions that decide what a player is shown. The two
 * that matter most are the ones that used to be wrong:
 *
 *  - `CREATED` was projected as `IN_PROGRESS`, which rendered a running board
 *    for a match that had not started.
 *  - An unrecognised pending action was coerced to `income`, which silently
 *    relabelled a foreign action as "collect 1 coin".
 */

function baseBackend(overrides: Partial<BackendGameState> = {}): BackendGameState {
  return {
    matchId: "match-1",
    roomId: "room-1",
    roomCode: "RAJNE",
    stateVersion: 1,
    status: "IN_PROGRESS",
    phase: "in_progress",
    players: [
      {
        userId: "p1",
        username: "alice",
        seatIndex: 0,
        status: "ACTIVE",
        host: true,
        alive: true,
        turn: true,
        coins: 5,
        influenceCount: 2,
        isBot: false,
      },
      {
        userId: "p2",
        username: "bob",
        seatIndex: 1,
        status: "ACTIVE",
        host: false,
        alive: true,
        turn: false,
        coins: 5,
        influenceCount: 2,
        isBot: false,
      },
    ],
    currentTurnPlayerId: "p1",
    turnOrder: ["p1", "p2"],
    turnNumber: 1,
    deckCount: 11,
    revealedCardsCount: 0,
    log: [],
    ...overrides,
  } as BackendGameState;
}

test("maps every backend action type to its frontend id", () => {
  const cases: [string, string][] = [
    ["EXCHANGE", "exchange"],
    ["FOREIGN_AID", "foreign_aid"],
    ["ASSASSINATE", "assassinate"],
    ["TAX", "tax"],
    ["STEAL", "steal"],
  ];

  for (const [backendType, expected] of cases) {
    const state = toGameState(
      baseBackend({
        pendingAction: {
          id: "pa-1",
          type: backendType,
          actorUserId: "p1",
          startedAt: "2026-01-01T00:00:00Z",
        },
      } as Partial<BackendGameState>),
    );
    assert.equal(state.activeAction?.action, expected, `action type ${backendType}`);
  }
});

test("does not relabel an unknown pending action as income", () => {
  const state = toGameState(
    baseBackend({
      pendingAction: {
        id: "pa-1",
        type: "SOMETHING_NEW",
        actorUserId: "p1",
        startedAt: "2026-01-01T00:00:00Z",
      },
    } as Partial<BackendGameState>),
  );

  // Dropped entirely rather than shown as a real action with the wrong label.
  assert.equal(state.activeAction, null);
});

test("does not relabel an unknown action verdict as income", () => {
  const state = toGameState(
    baseBackend({
      lastActionResult: {
        id: "ar-1",
        actionType: "SOMETHING_NEW",
        actorUserId: "p1",
        result: "RESOLVED",
        coinsGained: 0,
        coinsLost: 0,
        claimChallenged: false,
        eliminated: false,
        nextTurnPlayerId: "p2",
        nextTurnNumber: 2,
      },
    } as Partial<BackendGameState>),
  );

  assert.equal(state.lastActionResult, null);
});

test("maps a known action verdict onto the frontend model", () => {
  const state = toGameState(
    baseBackend({
      lastActionResult: {
        id: "ar-1",
        actionType: "EXCHANGE",
        actorUserId: "p1",
        result: "CANCELLED",
        coinsGained: 0,
        coinsLost: 0,
        claimChallenged: false,
        eliminated: false,
        nextTurnPlayerId: "p2",
        nextTurnNumber: 2,
      },
    } as Partial<BackendGameState>),
  );

  assert.equal(state.lastActionResult?.actionType, "exchange");
  assert.equal(state.lastActionResult?.result, "CANCELLED");
});

test("maps a CREATED match to WAITING, not IN_PROGRESS", () => {
  const state = toGameState(baseBackend({ status: "CREATED" }));
  assert.equal(state.status, "WAITING");
});

test("maps the remaining backend statuses explicitly", () => {
  assert.equal(toGameState(baseBackend({ status: "IN_PROGRESS" })).status, "IN_PROGRESS");
  assert.equal(toGameState(baseBackend({ status: "FINISHED" })).status, "FINISHED");
  assert.equal(toGameState(baseBackend({ status: "CANCELLED" })).status, "ABANDONED");
});

test("treats an unknown status as WAITING so the client resyncs", () => {
  const state = toGameState(baseBackend({ status: "SOMETHING_ELSE" as never }));
  assert.equal(state.status, "WAITING");
});

test("surfaces the Exchange pool to the acting player only", () => {
  const withPool = toGameState(
    baseBackend({
      pendingAction: {
        id: "pa-1",
        type: "EXCHANGE",
        actorUserId: "p1",
        startedAt: "2026-01-01T00:00:00Z",
        deadlineAt: "2026-01-01T00:01:00Z",
        exchangePool: [
          { cardId: "c1", characterId: "goyenda" },
          { cardId: "c2", characterId: "amla" },
          { cardId: "c3", characterId: "dalal" },
          { cardId: "c4", characterId: "minister" },
        ],
      },
    } as Partial<BackendGameState>),
  );

  assert.equal(withPool.exchangePool?.length, 4);
  assert.equal(withPool.activeAction?.deadlineAt, "2026-01-01T00:01:00Z");
});

test("carries the authoritative stateVersion through unchanged", () => {
  const state = toGameState(baseBackend({ stateVersion: 42 }));
  assert.equal(state.stateVersion, 42);
});
