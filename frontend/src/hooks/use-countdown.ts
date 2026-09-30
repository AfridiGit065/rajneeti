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
  const windowKey = `${deadlineAt ?? ""}|${fallbackSeconds}`;
  const [tick, setTick] = useState(() => ({
    windowKey,
    remaining: initialRemaining(deadlineAt, fallbackSeconds),
  }));

  // Re-derive on the deadline itself changing rather than inside an effect, so a
  // new window starts on its own clock without a cascading render.
  if (tick.windowKey !== windowKey) {
    setTick({ windowKey, remaining: initialRemaining(deadlineAt, fallbackSeconds) });
  }

  const remaining = tick.windowKey === windowKey ? tick.remaining : initialRemaining(deadlineAt, fallbackSeconds);

  useEffect(() => {
    if (!deadlineAt) return;
    const id = window.setInterval(() => {
      setTick((prev) =>
        prev.windowKey === windowKey
          ? { windowKey, remaining: Math.max(0, secondsUntil(deadlineAt, 0)) }
          : prev,
      );
    }, TICK_S * 1000);
    return () => window.clearInterval(id);
  }, [deadlineAt, windowKey]);

  return describeCountdown(remaining);
}

function initialRemaining(deadlineAt: string | undefined, fallbackSeconds: number) {
  return deadlineAt ? secondsUntil(deadlineAt, fallbackSeconds) : fallbackSeconds;
}

/**
 * Parses a backend timestamp into epoch milliseconds.
 *
 * The server stores everything in UTC and serialises it as a `LocalDateTime`,
 * which Jackson writes without a zone suffix (`2026-09-30T23:07:54`). The
 * spec reads a zone-less ISO string as *local* time, so east of UTC the parsed
 * instant landed in the past — the Challenge countdown then reported `expired`
 * on its very first render and auto-allowed the claim, so the Challenge button
 * mounted and dismissed itself within one frame and was never seen.
 *
 * A stamp that already carries an offset (`Z` or `+06:00`) is left untouched.
 */
export function parseServerTimestamp(value: string): number {
  const hasZone = /(?:Z|[+-]\d{2}:?\d{2})$/i.test(value.trim());
  return Date.parse(hasZone ? value : `${value.trim()}Z`);
}

function secondsUntil(deadlineAt: string, fallbackSeconds: number) {
  const deadline = parseServerTimestamp(deadlineAt);
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
