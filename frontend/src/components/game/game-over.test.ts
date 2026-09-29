import { test } from "node:test";
import assert from "node:assert/strict";
import { buildStandings } from "@/lib/game/standings";
import { toGameState } from "@/lib/game/backend-game-state";
import { MatchStatus, type GamePlayer, type GameState } from "@/types/game";
import type { BackendGameState } from "@/types/backend";

function createPlayer(overrides: Partial<GamePlayer> = {}): GamePlayer {
  return {
    id: overrides.id ?? "p-default",
    userId: overrides.userId ?? "p-default",
    username: overrides.username ?? "player",
    displayName: overrides.displayName ?? overrides.username ?? "player",
    isHost: false,
    isAlive: overrides.isAlive ?? false,
    isTurn: false,
    coins: overrides.coins ?? 2,
    influenceCards: overrides.influenceCards ?? [],
    seatIndex: overrides.seatIndex ?? 0,
    finalRank: overrides.finalRank,
  };
}

function createFinishedGame(players: GamePlayer[], winnerPlayerId: string): GameState {
  return {
    matchId: "match-123",
    roomId: "room-123",
    status: MatchStatus.FINISHED,
    phase: "game_over",
    players,
    currentTurnPlayerId: null,
    turnOrder: players.map((p) => p.id),
    turnNumber: 10,
    deckCount: 7,
    revealedCardsCount: 4,
    winnerPlayerId,
    activeAction: null,
    pendingChallenge: null,
    pendingBlock: null,
    lastActionResult: null,
    log: [],
    startedAt: new Date().toISOString(),
    endedAt: new Date().toISOString(),
  };
}

test("buildStandings - ranks 4 players by backend finalRank ASC (reverse elimination order)", () => {
  // Input backend order: B (rank 4), D (rank 3), A (rank 2), C (rank 1)
  const playerB = createPlayer({ id: "B", username: "B", coins: 10, finalRank: 4, isAlive: false, seatIndex: 1 });
  const playerD = createPlayer({ id: "D", username: "D", coins: 8, finalRank: 3, isAlive: false, seatIndex: 3 });
  const playerA = createPlayer({ id: "A", username: "A", coins: 6, finalRank: 2, isAlive: false, seatIndex: 0 });
  const playerC = createPlayer({ id: "C", username: "C", coins: 0, finalRank: 1, isAlive: true, seatIndex: 2 });

  const game = createFinishedGame([playerB, playerD, playerA, playerC], "C");
  const standings = buildStandings(game);

  assert.equal(standings.length, 4);

  // #1 C — Winner
  assert.equal(standings[0].player.username, "C");
  assert.equal(standings[0].rank, 1);

  // #2 A
  assert.equal(standings[1].player.username, "A");
  assert.equal(standings[1].rank, 2);

  // #3 D
  assert.equal(standings[2].player.username, "D");
  assert.equal(standings[2].rank, 3);

  // #4 B
  assert.equal(standings[3].player.username, "B");
  assert.equal(standings[3].rank, 4);
});

test("buildStandings - does not rank players by coin count or seat index", () => {
  // B has highest coins (15) and lowest seat index (0), but was eliminated first (finalRank 4)
  // C has 0 coins and higher seat index (3), but won (finalRank 1)
  const playerB = createPlayer({ id: "B", username: "B", coins: 15, seatIndex: 0, finalRank: 4, isAlive: false });
  const playerA = createPlayer({ id: "A", username: "A", coins: 5, seatIndex: 1, finalRank: 2, isAlive: false });
  const playerC = createPlayer({ id: "C", username: "C", coins: 0, seatIndex: 3, finalRank: 1, isAlive: true });

  const game = createFinishedGame([playerB, playerA, playerC], "C");
  const standings = buildStandings(game);

  assert.equal(standings[0].player.username, "C");
  assert.equal(standings[0].rank, 1);
  assert.equal(standings[1].player.username, "A");
  assert.equal(standings[1].rank, 2);
  assert.equal(standings[2].player.username, "B");
  assert.equal(standings[2].rank, 4);
});

test("toGameState - maps finalRank from BackendGamePlayer to GamePlayer", () => {
  const backendState: BackendGameState = {
    matchId: "match-1",
    roomId: "room-1",
    roomCode: "RAJNE",
    status: "FINISHED",
    phase: "game_over",
    hostUserId: "p-c",
    winnerUserId: "p-c",
    turnNumber: 8,
    deckCount: 7,
    revealedCardsCount: 3,
    startedAt: "2026-09-30T00:00:00Z",
    endedAt: "2026-09-30T00:10:00Z",
    log: [],
    players: [
      { userId: "p-b", username: "B", seatIndex: 0, status: "ELIMINATED", host: false, alive: false, turn: false, coins: 2, influenceCount: 0, finalRank: 3 },
      { userId: "p-a", username: "A", seatIndex: 1, status: "ELIMINATED", host: false, alive: false, turn: false, coins: 1, influenceCount: 0, finalRank: 2 },
      { userId: "p-c", username: "C", seatIndex: 2, status: "ACTIVE", host: true, alive: true, turn: false, coins: 5, influenceCount: 1, finalRank: 1 },
    ],
  };

  const gameState = toGameState(backendState);

  const playerB = gameState.players.find((p) => p.userId === "p-b");
  const playerA = gameState.players.find((p) => p.userId === "p-a");
  const playerC = gameState.players.find((p) => p.userId === "p-c");

  assert.equal(playerB?.finalRank, 3);
  assert.equal(playerA?.finalRank, 2);
  assert.equal(playerC?.finalRank, 1);
});
