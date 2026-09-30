"use client";

import { useEffect } from "react";
import { audioEngine } from "@/lib/audio/audio-engine";
import { useUiStore } from "@/store/ui-store";

/**
 * Owns the audio lifecycle for the app: unlocks playback on the first gesture,
 * keeps the theme track in sync with the Audio settings and the tab visibility.
 */
export function AudioController() {
  const soundEnabled = useUiStore((s) => s.settings.soundEnabled);
  const musicEnabled = useUiStore((s) => s.settings.musicEnabled);

  useEffect(() => {
    const unlock = () => audioEngine.unlock();
    window.addEventListener("pointerdown", unlock, { passive: true });
    window.addEventListener("keydown", unlock);
    window.addEventListener("touchstart", unlock, { passive: true });
    return () => {
      window.removeEventListener("pointerdown", unlock);
      window.removeEventListener("keydown", unlock);
      window.removeEventListener("touchstart", unlock);
    };
  }, []);

  useEffect(() => {
    audioEngine.syncMusic();
  }, [musicEnabled, soundEnabled]);

  useEffect(() => {
    const onVisibility = () => audioEngine.syncMusic();
    document.addEventListener("visibilitychange", onVisibility);
    const unsubscribe = audioEngine.subscribe(() => audioEngine.syncMusic());
    return () => {
      document.removeEventListener("visibilitychange", onVisibility);
      unsubscribe();
    };
  }, []);

  return null;
}