"use client";

import { SFX_NAMES, type SfxName, shouldPlaySfx } from "./sfx-policy";
import { resolveMusicPlayback, pickMusicSource, MUSIC_SOURCES } from "./music-policy";
import { useUiStore } from "@/store/ui-store";

const MUSIC_VOLUME = 0.34;

/**
 * Audio for the whole app.
 *
 * Effects are synthesised with the Web Audio API (no asset files, no latency)
 * and the theme track is a single looping element. Browsers refuse to start
 * audio before a user gesture, so `unlocked` flips on the first interaction and
 * both paths stay silent until then instead of throwing.
 */
class AudioEngine {
  private ctx: AudioContext | null = null;
  private music: HTMLAudioElement | null = null;
  private musicSource: string | null = null;
  private unlocked = false;
  private listeners = new Set<() => void>();

  private notify() {
    for (const listener of this.listeners) listener();
  }

  get isUnlocked() {
    return this.unlocked;
  }

  subscribe(listener: () => void) {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  }

  /** Called once on the first pointer/key interaction; unlocks Web Audio. */
  unlock() {
    if (this.unlocked) return;
    this.unlocked = true;
    const Ctor =
      typeof window === "undefined"
        ? undefined
        : window.AudioContext ??
          (window as unknown as { webkitAudioContext?: typeof AudioContext })
            .webkitAudioContext;
    if (Ctor) {
      try {
        this.ctx = this.ctx ?? new Ctor();
        if (this.ctx.state === "suspended") void this.ctx.resume();
      } catch {
        this.ctx = null;
      }
    }
    this.notify();
  }

  private context(): AudioContext | null {
    if (!this.unlocked) return null;
    const Ctor =
      typeof window === "undefined"
        ? undefined
        : window.AudioContext ??
          (window as unknown as { webkitAudioContext?: typeof AudioContext })
            .webkitAudioContext;
    if (!Ctor) return null;
    try {
      this.ctx = this.ctx ?? new Ctor();
      if (this.ctx.state === "suspended") void this.ctx.resume();
      return this.ctx;
    } catch {
      return null;
    }
  }

  /** Short enveloped oscillator. Returns nothing: audio must never break a click. */
  private tone(opts: {
    freq: number;
    duration: number;
    type?: OscillatorType;
    gain?: number;
    delay?: number;
    sweepTo?: number;
  }) {
    const ctx = this.context();
    if (!ctx) return;
    const {
      freq,
      duration,
      type = "sine",
      gain = 0.16,
      delay = 0,
      sweepTo,
    } = opts;
    const start = ctx.currentTime + delay;
    const osc = ctx.createOscillator();
    const amp = ctx.createGain();
    osc.type = type;
    osc.frequency.setValueAtTime(freq, start);
    if (sweepTo !== undefined) {
      osc.frequency.exponentialRampToValueAtTime(
        Math.max(1, sweepTo),
        start + duration,
      );
    }
    amp.gain.setValueAtTime(0.0001, start);
    amp.gain.exponentialRampToValueAtTime(gain, start + 0.012);
    amp.gain.exponentialRampToValueAtTime(0.0001, start + duration);
    osc.connect(amp).connect(ctx.destination);
    osc.start(start);
    osc.stop(start + duration + 0.02);
  }

  play(name: SfxName) {
    const { soundEnabled } = useUiStore.getState().settings;
    if (!shouldPlaySfx({ soundEnabled }, { unlocked: this.unlocked })) return;
    if (!SFX_NAMES.includes(name)) return;

    switch (name) {
      case "click":
        this.tone({ freq: 660, duration: 0.05, type: "triangle", gain: 0.09 });
        break;
      case "cardPick":
        this.tone({ freq: 520, duration: 0.1, type: "triangle", gain: 0.16, sweepTo: 880 });
        break;
      case "cardUnpick":
        this.tone({ freq: 560, duration: 0.1, type: "triangle", gain: 0.12, sweepTo: 330 });
        break;
      case "confirm":
        this.tone({ freq: 523, duration: 0.11, type: "sine", gain: 0.15 });
        this.tone({ freq: 784, duration: 0.18, type: "sine", gain: 0.13, delay: 0.09 });
        break;
      case "cancel":
        this.tone({ freq: 330, duration: 0.14, type: "sine", gain: 0.12, sweepTo: 220 });
        break;
      case "error":
        this.tone({ freq: 190, duration: 0.24, type: "sawtooth", gain: 0.1 });
        break;
      case "coin":
        this.tone({ freq: 980, duration: 0.07, type: "square", gain: 0.09 });
        this.tone({ freq: 1320, duration: 0.12, type: "square", gain: 0.07, delay: 0.06 });
        break;
      case "turn":
        this.tone({ freq: 440, duration: 0.16, type: "sine", gain: 0.13 });
        this.tone({ freq: 660, duration: 0.24, type: "sine", gain: 0.12, delay: 0.12 });
        break;
      case "win":
        [523, 659, 784, 1047].forEach((freq, index) => {
          this.tone({ freq, duration: 0.28, type: "triangle", gain: 0.14, delay: index * 0.11 });
        });
        break;
    }
  }

  /** Applies the music policy to the live element. Safe to call every render. */
  syncMusic() {
    if (typeof document === "undefined") return;
    const { musicEnabled } = useUiStore.getState().settings;
    const playback = resolveMusicPlayback({
      musicEnabled,
      unlocked: this.unlocked,
      visible: document.visibilityState !== "hidden",
    });

    if (playback === "pause") {
      this.music?.pause();
      return;
    }
    if (!this.music) {
      const audio = new Audio(this.resolveSource());
      audio.loop = true;
      audio.volume = MUSIC_VOLUME;
      audio.preload = "auto";
      this.music = audio;
    }
    if (this.music.paused) {
      this.music.play().catch(() => {
        /* blocked until a gesture; the next unlock retries */
      });
    }
  }

  /**
   * Probes the candidates once and remembers the winner. A wrong extension is an
   * invisible 404 that looks exactly like "the game has no music", so this never
   * assumes a filename.
   */
  private resolveSource(): string {
    if (this.musicSource) return this.musicSource;
    const fallback = MUSIC_SOURCES[0];
    void (async () => {
      const found: string[] = [];
      await Promise.all(
        MUSIC_SOURCES.map(async (candidate) => {
          try {
            const response = await fetch(candidate, { method: "HEAD" });
            if (response.ok) found.push(candidate);
          } catch {
            /* offline or blocked: keep looking */
          }
        }),
      );
      const picked = pickMusicSource(found);
      if (picked && this.music && this.music.src !== picked) {
        this.music.src = picked;
        if (this.music.paused) {
          this.music.play().catch(() => {
            /* still blocked until a gesture */
          });
        }
      }
      this.musicSource = picked ?? fallback;
    })();
    return fallback;
  }
}

export const audioEngine = new AudioEngine();