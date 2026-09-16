"use client";

import { useEffect, useRef } from "react";
import { realtimeSocket } from "@/lib/websocket/stomp-client";
import type { WebSocketEvent } from "@/types/websocket";

/**
 * Subscribes to {@code /topic/matches/{matchId}} while the board is mounted.
 * Gameplay broadcasts are forwarded to the callback — the board re-fetches the
 * authoritative (player-safe) game state instead of trusting event payloads.
 */
export function useMatchRealtime(
  matchId: string,
  enabled: boolean,
  onMatchEvent?: (event: WebSocketEvent) => void,
) {
  const callbackRef = useRef(onMatchEvent);

  useEffect(() => {
    callbackRef.current = onMatchEvent;
  });

  useEffect(() => {
    if (!enabled || !matchId) return;
    return realtimeSocket.subscribe(`/topic/matches/${matchId}`, (event) => {
      callbackRef.current?.(event);
    });
  }, [matchId, enabled]);
}