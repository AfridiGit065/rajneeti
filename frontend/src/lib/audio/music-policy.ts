export interface MusicGate {
  musicEnabled: boolean;
  /** False until the first user gesture unlocks audio playback. */
  unlocked: boolean;
  visible: boolean;
}

export type MusicPlayback = "play" | "pause";

/**
 * Single source of truth for the background track. Kept pure so the rule is
 * testable: the element only plays when the user asked for music, the browser
 * has allowed audio, and the tab is actually on screen.
 */
export function resolveMusicPlayback(gate: MusicGate): MusicPlayback {
  if (!gate.musicEnabled) return "pause";
  if (!gate.unlocked) return "pause";
  if (!gate.visible) return "pause";
  return "play";
}

/**
 * Candidates in preference order. The track ships as `.mpeg`, but the extension
 * has changed before, and a wrong guess is an invisible 404 that just means
 * silence, so the first source that actually resolves wins.
 */
export const MUSIC_SOURCES = [
  "/assets/game/music/rajneeti-theme.mpeg",
  "/assets/game/music/rajneeti-theme.mp3",
] as const;

/** Picks the first candidate the server can actually serve. */
export function pickMusicSource(available: readonly string[]): string | null {
  for (const source of MUSIC_SOURCES) {
    if (available.includes(source)) return source;
  }
  return null;
}