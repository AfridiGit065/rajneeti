import { test } from "node:test";
import assert from "node:assert/strict";
import { parseServerTimestamp } from "./use-countdown.ts";

/**
 * Backend → client clock contract.
 *
 * The server serialises every timestamp as a UTC `LocalDateTime`, which Jackson
 * writes without a zone suffix (e.g. `2026-09-30T23:07:54`). `Date.parse` reads a
 * zone-less ISO string as *local* time, so in any timezone east of UTC the
 * deadline resolved to an instant already in the past. The Challenge panel
 * auto-allows on an expired countdown, so the Challenge button mounted and
 * dismissed itself within one frame — it never appeared.
 *
 * These pin the parse so the bug cannot come back.
 */

const SERVER_STAMP = "2026-09-30T23:07:54";

test("parseServerTimestamp - a zone-less backend timestamp is UTC, not local time", () => {
  assert.equal(
    new Date(parseServerTimestamp(SERVER_STAMP)).toISOString(),
    "2026-09-30T23:07:54.000Z",
  );
});

test("parseServerTimestamp - the result does not depend on the machine timezone", () => {
  // A naive Date.parse gives a different instant per machine, so assert against
  // the fixed UTC instant rather than a duration.
  const parsed = parseServerTimestamp(SERVER_STAMP);
  const expected = Date.parse(`${SERVER_STAMP}Z`);
  assert.equal(parsed, expected);
});

test("parseServerTimestamp - an explicit Z suffix stays UTC", () => {
  assert.equal(
    parseServerTimestamp(`${SERVER_STAMP}Z`),
    Date.parse(`${SERVER_STAMP}Z`),
  );
});

test("parseServerTimestamp - an explicit numeric offset is honoured", () => {
  assert.equal(
    parseServerTimestamp("2026-09-30T23:07:54+06:00"),
    Date.parse("2026-09-30T23:07:54+06:00"),
  );
});

test("parseServerTimestamp - sub-second precision is preserved", () => {
  assert.equal(
    parseServerTimestamp("2026-09-30T23:07:54.321"),
    Date.parse("2026-09-30T23:07:54.321Z"),
  );
});

test("parseServerTimestamp - an unparseable value returns NaN", () => {
  assert.ok(Number.isNaN(parseServerTimestamp("not-a-date")));
});

test("parseServerTimestamp - a deadline in the future is never already expired", () => {
  const future = new Date(Date.now() + 15_000)
    .toISOString()
    .replace(/\.\d{3}Z$/, "")
    .replace("Z", "");
  assert.ok(parseServerTimestamp(future) > Date.now());
});
