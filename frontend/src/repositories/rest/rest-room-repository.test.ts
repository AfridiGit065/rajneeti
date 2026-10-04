import { test } from "node:test";
import assert from "node:assert/strict";

import { RestRoomRepository, toRoomSummary } from "./rest-room-repository.ts";
import { configureApiClient } from "@/lib/api-client";
import type { BackendRoom } from "@/types/backend";

/**
 * Module 06 — Ready System wiring.
 *
 * The lobby UI never keeps a private copy of readiness: it renders whatever the
 * backend last returned. These tests pin the two halves of that contract —
 * that the backend `ready` flag is projected faithfully, and that the Ready /
 * Unready intent is routed to the matching endpoint.
 */

function backendRoom(
  ready: boolean,
  overrides: Partial<BackendRoom> = {},
): BackendRoom {
  const room: BackendRoom = {
    id: "room-1",
    roomCode: "RAJ123",
    hostId: "host-1",
    hostUsername: "ayesha",
    status: "WAITING",
    maxPlayers: 6,
    currentPlayers: 2,
    canStart: false,
    players: [
      {
        id: "host-1",
        username: "ayesha",
        seatNumber: 1,
        ready,
        isHost: true,
        isBot: false,
        joinedAt: "2026-01-01T00:00:00Z",
      },
      {
        id: "guest-1",
        username: "kabir",
        seatNumber: 2,
        ready: false,
        isHost: false,
        isBot: false,
        joinedAt: "2026-01-01T00:01:00Z",
      },
    ],
    createdAt: "2026-01-01T00:00:00Z",
  };
  return { ...room, ...overrides };
}

/** Minimal `fetch` double that records the calls and replays canned responses. */
function stubFetch(
  responses: Array<{ status?: number; body: unknown }>,
): { calls: Array<{ url: string; method: string }>; restore: () => void } {
  const original = globalThis.fetch;
  const calls: Array<{ url: string; method: string }> = [];
  let index = 0;

  globalThis.fetch = (async (input: RequestInfo | URL, init?: RequestInit) => {
    calls.push({ url: String(input), method: init?.method ?? "GET" });
    const next = responses[Math.min(index, responses.length - 1)];
    index += 1;
    const status = next.status ?? 200;
    return {
      ok: status >= 200 && status < 300,
      status,
      json: async () => next.body,
    } as Response;
  }) as typeof fetch;

  return { calls, restore: () => { globalThis.fetch = original; } };
}

function successEnvelope(data: unknown) {
  return { success: true, message: "ok", data, timestamp: "2026-01-01T00:00:00Z" };
}

configureApiClient({ getAccessToken: () => "test-token", onUnauthorized: () => {} });

test("a player's ready flag is read from the backend, never assumed", () => {
  assert.equal(toRoomSummary(backendRoom(true)).players[0].isReady, true);
  assert.equal(toRoomSummary(backendRoom(false)).players[0].isReady, false);
});

test("every seated player's own ready flag is projected independently", () => {
  const room = backendRoom(false, {
    players: [
      { id: "host-1", username: "ayesha", seatNumber: 1, ready: true, isHost: true, joinedAt: "2026-01-01T00:00:00Z" },
      { id: "guest-1", username: "kabir", seatNumber: 2, ready: false, isHost: false, joinedAt: "2026-01-01T00:01:00Z" },
    ],
  });

  const summary = toRoomSummary(room);

  assert.deepEqual(summary.players.map((p) => p.isReady), [true, false]);
  assert.deepEqual(summary.players.map((p) => p.seatIndex), [0, 1]);
});

test("Ready posts to the room ready endpoint and returns the refreshed room", async () => {
  const stub = stubFetch([{ body: successEnvelope(backendRoom(true)) }]);
  try {
    const result = await new RestRoomRepository().readyUp("room-1", true);

    assert.equal(stub.calls.length, 1);
    assert.equal(stub.calls[0].method, "POST");
    assert.ok(stub.calls[0].url.endsWith("/api/rooms/room-1/ready"));
    assert.equal(result.ok, true);
    assert.equal(result.ok && result.data.players[0].isReady, true);
  } finally {
    stub.restore();
  }
});

test("Unready posts to the room unready endpoint", async () => {
  const stub = stubFetch([{ body: successEnvelope(backendRoom(false)) }]);
  try {
    const result = await new RestRoomRepository().readyUp("room-1", false);

    assert.equal(stub.calls.length, 1);
    assert.equal(stub.calls[0].method, "POST");
    assert.ok(stub.calls[0].url.endsWith("/api/rooms/room-1/unready"));
    assert.equal(result.ok && result.data.players[0].isReady, false);
  } finally {
    stub.restore();
  }
});

test("a rejected ready request surfaces the backend error instead of a local state", async () => {
  const stub = stubFetch([
    {
      status: 400,
      body: {
        status: 400,
        error: "INVALID_ROOM_STATE",
        message: "Cannot change readiness when room status is IN_GAME",
        path: "/api/rooms/room-1/ready",
        timestamp: "2026-01-01T00:00:00Z",
      },
    },
  ]);
  try {
    const result = await new RestRoomRepository().readyUp("room-1", true);

    assert.equal(result.ok, false);
    assert.equal(!result.ok && result.error.error, "INVALID_ROOM_STATE");
  } finally {
    stub.restore();
  }
});

test("an unauthorized ready request is reported with its 401 status", async () => {
  const stub = stubFetch([
    {
      status: 401,
      body: {
        status: 401,
        error: "UNAUTHORIZED",
        message: "Full authentication is required to access this resource",
        timestamp: "2026-01-01T00:00:00Z",
      },
    },
  ]);
  try {
    const result = await new RestRoomRepository().readyUp("room-1", true);

    assert.equal(result.ok, false);
    assert.equal(!result.ok && result.error.status, 401);
  } finally {
    stub.restore();
  }
});
