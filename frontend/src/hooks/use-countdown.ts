"use client";

import { useEffect, useState } from "react";

const TICK_S = 0.25;

/**
 * Ticks a countdown starting from `totalSeconds`.
 * The countdown always starts at mount; restart it by remounting the
 * component (render `<Timer key={turnNumber} ... />`).
 */
export function useCountdown(totalSeconds: number) {
  const [remaining, setRemaining] = useState(totalSeconds);

  useEffect(() => {
    const id = window.setInterval(() => {
      setRemaining((value) => Math.max(0, value - TICK_S));
    }, TICK_S * 1000);
    return () => window.clearInterval(id);
  }, []);

  const remainingSeconds = Math.ceil(remaining);
  const isRunning = remainingSeconds > 0;
  const minutes = Math.floor(remainingSeconds / 60);
  const seconds = remainingSeconds % 60;

  return {
    remainingSeconds,
    display: `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`,
    isRunning,
    expired: !isRunning,
  };
}