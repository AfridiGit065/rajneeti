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

  return describeCountdown(remaining);
}

/**
 * Counts down to a backend-supplied deadline instead of a local duration, so
 * the displayed clock tracks the authoritative server window rather than
 * guessing when it started. This is display only: the server is what resolves
 * the action when the deadline passes, so a slow or backgrounded tab can never
 * change the outcome.
 */
export function useDeadlineCountdown(deadlineAt: string | undefined, fallbackSeconds: number) {
  const [remaining, setRemaining] = useState(() =>
    deadlineAt ? secondsUntil(deadlineAt, fallbackSeconds) : fallbackSeconds,
  );

  useEffect(() => {
    if (!deadlineAt) {
      setRemaining(fallbackSeconds);
      return;
    }
    setRemaining(secondsUntil(deadlineAt, fallbackSeconds));
    const id = window.setInterval(() => {
      setRemaining(Math.max(0, secondsUntil(deadlineAt, 0)));
    }, TICK_S * 1000);
    return () => window.clearInterval(id);
  }, [deadlineAt, fallbackSeconds]);

  return describeCountdown(remaining);
}

function secondsUntil(deadlineAt: string, fallbackSeconds: number) {
  const deadline = Date.parse(deadlineAt);
  if (Number.isNaN(deadline)) return fallbackSeconds;
  return Math.max(0, (deadline - Date.now()) / 1000);
}

function describeCountdown(remaining: number) {
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
