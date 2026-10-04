import { test } from "node:test";
import assert from "node:assert/strict";
import { SFX_NAMES, type SfxName, shouldPlaySfx, shouldShowSoundHint } from "@/lib/audio/sfx-policy";
import { resolveMusicPlayback, pickMusicSource, MUSIC_SOURCES } from "@/lib/audio/music-policy";

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

test("the sound hint shows until the first interaction unlocks audio", () => {
  assert.equal(
    shouldShowSoundHint({ soundEnabled: true, musicEnabled: true, unlocked: false }),
    true,
  );
  assert.equal(
    shouldShowSoundHint({ soundEnabled: true, musicEnabled: true, unlocked: true }),
    false,
  );
});

test("the sound hint stays hidden when the player turned audio off entirely", () => {
  assert.equal(
    shouldShowSoundHint({ soundEnabled: false, musicEnabled: false, unlocked: false }),
    false,
  );
});

test("the sound hint appears for music-only players", () => {
  assert.equal(
    shouldShowSoundHint({ soundEnabled: false, musicEnabled: true, unlocked: false }),
    true,
  );
});

test("the shipped track extension is preferred", () => {
  assert.match(MUSIC_SOURCES[0], /\.mp3$/);
});

test("the first source the server serves wins", () => {
  assert.equal(pickMusicSource(["/assets/game/music/rajneeti-theme.mpeg"]), MUSIC_SOURCES[1]);
  assert.equal(
    pickMusicSource(["/assets/game/music/rajneeti-theme.mpeg", "/assets/game/music/rajneeti-theme.mp3"]),
    MUSIC_SOURCES[0],
  );
});

test("no available source yields null instead of a silent 404", () => {
  assert.equal(pickMusicSource([]), null);
});