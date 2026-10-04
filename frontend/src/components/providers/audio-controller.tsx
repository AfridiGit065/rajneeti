"use client";

import { useEffect, useState } from "react";
import { audioEngine } from "@/lib/audio/audio-engine";
import { shouldShowSoundHint } from "@/lib/audio/sfx-policy";
import { useUiStore } from "@/store/ui-store";
import { Volume2 } from "@/components/ui/icons";

/**
 * Owns the audio lifecycle for the app: unlocks playback on the first gesture,
 * keeps the theme track in sync with the Audio settings and the tab visibility.
 */
export function AudioController() {
  const soundEnabled = useUiStore((s) => s.settings.soundEnabled);
  const musicEnabled = useUiStore((s) => s.settings.musicEnabled);
  const [unlocked, setUnlocked] = useState(() => audioEngine.isUnlocked);

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
    const unsubscribe = audioEngine.subscribe(() => {
      setUnlocked(audioEngine.isUnlocked);
      audioEngine.syncMusic();
    });
    return () => {
      document.removeEventListener("visibilitychange", onVisibility);
      unsubscribe();
    };
  }, []);

  const showHint = shouldShowSoundHint({ soundEnabled, musicEnabled, unlocked });

  return showHint ? (
    <button
      type="button"
      onPointerDown={audioEngine.unlock.bind(audioEngine)}
      className="fixed bottom-4 left-1/2 z-[70] flex -translate-x-1/2 items-center gap-2 rounded-full border border-gold-500/40 bg-surface/95 px-4 py-2 text-ivory shadow-lg backdrop-blur transition hover:border-gold-400/70 motion-reduce:transition-none"
      aria-label="Enable sound"
    >
      <Volume2 className="size-4 shrink-0 text-gold-300" aria-hidden />
      <span className="font-bengali text-xs">
        সাউন্ড চালু করতে যেকোনো জায়গায় ক্লিক করুন
      </span>
    </button>
  ) : null;
}