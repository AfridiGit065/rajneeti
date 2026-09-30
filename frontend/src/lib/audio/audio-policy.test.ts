import { test } from "node:test";
import assert from "node:assert/strict";
import { SFX_NAMES, type SfxName, shouldPlaySfx } from "@/lib/audio/sfx-policy";
import { resolveMusicPlayback } from "@/lib/audio/music-policy";

test("every declared sound effect has a playable name", () => {
  assert.ok(SFX_NAMES.length > 0);
  assert.equal(new Set(SFX_NAMES).size, SFX_NAMES.length, "names must be unique");
  for (const name of SFX_NAMES) assert.match(name, /^[a-z][a-zA-Z]*$/);
});

test("sound effects stay silent while the setting is off", () => {
  assert.equal(shouldPlaySfx({ soundEnabled: false }), false);
  assert.equal(shouldPlaySfx({ soundEnabled: true }), true);
});

test("sound effects are suppressed before the user unlocks audio", () => {
  assert.equal(shouldPlaySfx({ soundEnabled: true }, { unlocked: false }), false);
  assert.equal(shouldPlaySfx({ soundEnabled: true }, { unlocked: true }), true);
});

test("sending an unknown effect never throws and stays silent", () => {
  const played: SfxName[] = [];
  const dispatch = (name: string) => {
    if (!shouldPlaySfx({ soundEnabled: true }, { unlocked: true })) return;
    if (!SFX_NAMES.includes(name as SfxName)) return;
    played.push(name as SfxName);
  };
  dispatch("not-a-real-effect");
  dispatch("cardPick");
  assert.deepEqual(played, ["cardPick"]);
});

test("music plays only when enabled, unlocked and the tab is visible", () => {
  assert.equal(
    resolveMusicPlayback({ musicEnabled: true, unlocked: true, visible: true }),
    "play",
  );
});

test("music stays paused until the first user interaction", () => {
  assert.equal(
    resolveMusicPlayback({ musicEnabled: true, unlocked: false, visible: true }),
    "pause",
  );
});

test("music pauses when the setting is off", () => {
  assert.equal(
    resolveMusicPlayback({ musicEnabled: false, unlocked: true, visible: true }),
    "pause",
  );
});

test("music pauses while the tab is hidden", () => {
  assert.equal(
    resolveMusicPlayback({ musicEnabled: true, unlocked: true, visible: false }),
    "pause",
  );
});

test("switching the toggle off and on again resumes music", () => {
  const state = { musicEnabled: true, unlocked: true, visible: true };
  const on = resolveMusicPlayback(state);
  const off = resolveMusicPlayback({ ...state, musicEnabled: false });
  const back = resolveMusicPlayback({ ...state, musicEnabled: true });
  assert.deepEqual([on, off, back], ["play", "pause", "play"]);
});