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