import type { GameSettings } from "@/types/user";

/**
 * Sound effects are synthesised with the Web Audio API, so no asset files ship
 * with the bundle and playback starts with zero network cost.
 */
export const SFX_NAMES = [
  "click",
  "cardPick",
  "cardUnpick",
  "confirm",
  "cancel",
  "error",
  "coin",
  "turn",
  "win",
] as const;

export type SfxName = (typeof SFX_NAMES)[number];

export interface SfxGate {
  soundEnabled: boolean;
}

export interface SfxRuntime {
  /** False until the browser has seen a user gesture (autoplay policy). */
  unlocked: boolean;
}

/**
 * The gate every effect passes through. Browsers block audio until a user
 * gesture, so an effect requested earlier is dropped rather than queued: the UI
 * feedback it accompanies is always newer than the block.
 */
export function shouldPlaySfx(gate: SfxGate, runtime?: Partial<SfxRuntime>): boolean {
  if (!gate.soundEnabled) return false;
  if (runtime && runtime.unlocked === false) return false;
  return true;
}

export interface SoundHintGate extends SfxGate {
  musicEnabled: boolean;
  unlocked: boolean;
}

/**
 * Browsers refuse to play audio until the user interacts with the page, so the
 * game is silently muted on arrival. Without a hint that reads as "the game has
 * no sound" instead of "one click is required".
 */
export function shouldShowSoundHint(gate: SoundHintGate): boolean {
  if (gate.unlocked) return false;
  return gate.soundEnabled || gate.musicEnabled;
}