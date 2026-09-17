import { test } from "node:test";
import assert from "node:assert/strict";
import {
  detectVersionGap,
  isSuperseded,
  type VersionKey,
} from "./state-version.ts";

function key(stateVersion: number, scope: "public" | "private"): VersionKey {
  return { stateVersion, scope };
}

test("accepts the first snapshot of a match", () => {
  assert.equal(isSuperseded(undefined, key(1, "public")), false);
  assert.equal(detectVersionGap(undefined, key(1, "public")), false);
});

test("drops older and equal public snapshots", () => {
  const current = key(3, "public");
  assert.equal(isSuperseded(current, key(2, "public")), true);
  assert.equal(isSuperseded(current, key(3, "public")), true);
  assert.equal(isSuperseded(current, key(4, "public")), false);
});

test("lets a private snapshot replace a public snapshot of the same version", () => {
  const current = key(5, "public");
  assert.equal(isSuperseded(current, key(5, "private")), false);
});

test("drops public over private and duplicate private snapshots", () => {
  const current = key(5, "private");
  assert.equal(isSuperseded(current, key(5, "private")), true);
  assert.equal(isSuperseded(current, key(5, "public")), true);
  assert.equal(isSuperseded(current, key(6, "private")), false);
});

test("detects only forward version gaps", () => {
  const current = key(2, "public");
  assert.equal(detectVersionGap(current, key(4, "public")), true);
  assert.equal(detectVersionGap(current, key(3, "public")), false);
  assert.equal(detectVersionGap(current, key(2, "private")), false);
  assert.equal(detectVersionGap(current, key(1, "public")), false);
});