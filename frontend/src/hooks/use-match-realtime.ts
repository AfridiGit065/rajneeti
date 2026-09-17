"use client";

import { useEffect, useRef } from "react";
import { realtimeSocket } from "@/lib/websocket/stomp-client";
import { useRealtimeStore } from "@/store/realtime-store";
import type { BackendGameState } from "@/types/backend";
import type { WebSocketEvent } from "@/types/websocket";

/**
 * Subscribes to {@code /topic/matches/{matchId}} while the board is mounted.
 *
 * Module 23 — {@code STATE_UPDATED} events carry the authoritative full public
 * snapshot (identical to the REST game state, but with no player's cards).
 * They are ingested directly into the realtime store so the board can render
 * without an extra HTTP round trip. Granular gameplay broadcasts are forwarded
 * to the callback — the board uses them as a fallback and for toasts.
 */
export function useMatchRealtime(
  matchId: string,
  enabled: boolean,
  onMatchEvent?: (event: WebSocketEvent) => void,
) {
  const callbackRef = useRef(onMatchEvent);
  const applyPublicState = useRealtimeStore((s) => s.applyPublicState);

  useEffect(() => {
    callbackRef.current = onMatchEvent;
  });

  useEffect(() => {
    if (!enabled || !matchId) return;
    return realtimeSocket.subscribe(`/topic/matches/${matchId}`, (event) => {
      if (event.eventType === "STATE_UPDATED") {
        const payload = event.payload as BackendGameState;
        if (payload && typeof payload.matchId === "string") {
          applyPublicState(payload.matchId, payload);
        }
      }
      callbackRef.current?.(event);
    });
  }, [matchId, enabled, applyPublicState]);
}