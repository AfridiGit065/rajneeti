import { test } from "node:test";
import assert from "node:assert/strict";
import { toGameState } from "@/lib/game/backend-game-state";
import { MatchStatus, type GamePlayer, type GameState, type InfluenceCard } from "@/types/game";
import type { BackendGameState } from "@/types/backend";

test("toGameState - maps exchangePool from backend regardless of case", () => {
  const backend: BackendGameState = {
    matchId: "m1",
    roomId: "r1",
    roomCode: "ABCD",
    hostUserId: "u1",
    startedAt: new Date().toISOString(),
    status: "IN_PROGRESS",
    phase: "in_progress",
    currentTurnPlayerId: "u1",
    turnNumber: 1,
    deckCount: 11,
    revealedCardsCount: 0,
    players: [
      {
        userId: "u1",
        username: "Actor",
        seatIndex: 0,
        status: "ACTIVE",
        host: true,
        alive: true,
        turn: true,
        coins: 2,
        influenceCount: 2,
        cards: [
          { cardId: "c1", characterId: "minister" },
          { cardId: "c2", characterId: "amla" },
        ],
      },
    ],
    pendingAction: {
      id: "pa1",
      type: "EXCHANGE",
      actorUserId: "u1",
      startedAt: new Date().toISOString(),
      exchangePool: [
        { cardId: "c1", characterId: "minister" },
        { cardId: "c2", characterId: "amla" },
        { cardId: "c3", characterId: "ghatok" },
        { cardId: "c4", characterId: "dalal" },
      ],
    },
    log: [],
    stateVersion: 1,
  };

  const game = toGameState(backend);
  assert.equal(game.activeAction?.action, "exchange");
  assert.equal(game.exchangePool?.length, 4);
  assert.equal(game.exchangePool?.[0].id, "c1");
  assert.equal(game.exchangePool?.[1].id, "c2");
  assert.equal(game.exchangePool?.[2].id, "c3");
  assert.equal(game.exchangePool?.[3].id, "c4");
});

test("Exchange selection logic - tracks selection, toggle deselect, and FIFO replacement", () => {
  const cards: InfluenceCard[] = [
    { id: "card-1", characterId: "minister", revealed: false },
    { id: "card-2", characterId: "amla", revealed: false },
    { id: "card-3", characterId: "ghatok", revealed: false },
    { id: "card-4", characterId: "dalal", revealed: false },
  ];

  let selected = new Set<string>();

  function toggle(cardId: string) {
    if (!cards.some((c) => c.id === cardId)) return;
    const next = new Set(selected);
    if (next.has(cardId)) {
      next.delete(cardId);
    } else if (next.size < 2) {
      next.add(cardId);
    } else {
      const first = Array.from(next)[0];
      if (first) next.delete(first);
      next.add(cardId);
    }
    selected = next;
  }

  // 1. Initial state
  assert.equal(selected.size, 0);

  // 2. Select first card
  toggle("card-1");
  assert.equal(selected.size, 1);
  assert.ok(selected.has("card-1"));

  // 3. Select second card -> reaches required 2 cards
  toggle("card-2");
  assert.equal(selected.size, 2);
  assert.ok(selected.has("card-1"));
  assert.ok(selected.has("card-2"));

  // 4. Toggle selected card again -> deselects
  toggle("card-1");
  assert.equal(selected.size, 1);
  assert.ok(!selected.has("card-1"));
  assert.ok(selected.has("card-2"));

  // 5. Select card-3 -> now card-2 and card-3 selected
  toggle("card-3");
  assert.equal(selected.size, 2);
  assert.ok(selected.has("card-2"));
  assert.ok(selected.has("card-3"));

  // 6. Select card-4 when 2 already selected -> replaces oldest (card-2) with card-4
  toggle("card-4");
  assert.equal(selected.size, 2);
  assert.ok(!selected.has("card-2"));
  assert.ok(selected.has("card-3"));
  assert.ok(selected.has("card-4"));

  // 7. Cannot select non-existent card
  toggle("non-existent-card");
  assert.equal(selected.size, 2);
  assert.ok(!selected.has("non-existent-card"));
});

test("Dock side determination - prefers right side unless occupied by Chronicle", () => {
  function getDockSide(chronicleOpen: boolean): "right" | "left" {
    return chronicleOpen ? "left" : "right";
  }

  assert.equal(getDockSide(false), "right");
  assert.equal(getDockSide(true), "left");
});

test("Reaction opportunity detection - triggers for block and challenge states", () => {
  function detectReaction({
    showBlockOffer,
    blockWindowAction,
    blockEvent,
    showActorResolve,
    blockResultData,
    isChallengeActive,
  }: {
    showBlockOffer: boolean;
    blockWindowAction: string | null;
    blockEvent: boolean;
    showActorResolve: boolean;
    blockResultData: boolean;
    isChallengeActive: boolean;
  }): boolean {
    const hasBlockOffer = Boolean(showBlockOffer && blockWindowAction && !blockEvent);
    const hasBlockResolve = Boolean(showActorResolve && blockWindowAction && !blockEvent);
    const hasBlockEvent = Boolean(blockEvent);
    const hasBlockResult = Boolean(blockResultData);
    const hasChallenge = Boolean(isChallengeActive);

    return hasBlockResult || hasBlockEvent || hasBlockOffer || hasBlockResolve || hasChallenge;
  }

  // Normal turn (no reactions active)
  assert.equal(
    detectReaction({
      showBlockOffer: false,
      blockWindowAction: null,
      blockEvent: false,
      showActorResolve: false,
      blockResultData: false,
      isChallengeActive: false,
    }),
    false,
  );

  // Steal / Foreign Aid block opportunity active
  assert.equal(
    detectReaction({
      showBlockOffer: true,
      blockWindowAction: "steal",
      blockEvent: false,
      showActorResolve: false,
      blockResultData: false,
      isChallengeActive: false,
    }),
    true,
  );

  // Challenge opportunity active (e.g. Tax / Minister challenge)
  assert.equal(
    detectReaction({
      showBlockOffer: false,
      blockWindowAction: null,
      blockEvent: false,
      showActorResolve: false,
      blockResultData: false,
      isChallengeActive: true,
    }),
    true,
  );

  // Block event claimed by opponent
  assert.equal(
    detectReaction({
      showBlockOffer: false,
      blockWindowAction: null,
      blockEvent: true,
      showActorResolve: false,
      blockResultData: false,
      isChallengeActive: false,
    }),
    true,
  );
});
