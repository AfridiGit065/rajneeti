"use client";

import { useEffect } from "react";
import { realtimeSocket } from "@/lib/websocket/stomp-client";
import { useRealtimeStore } from "@/store/realtime-store";
import { useAuthStore } from "@/store/auth-store";
import { useUiStore } from "@/store/ui-store";
import type { BackendGameState } from "@/types/backend";
import type {
  CardRevealPayload,
  WebSocketErrorPayload,
  WebSocketEvent,
} from "@/types/websocket";

const STOMP_TITLE = "রিল-টাইম সংযোগ";

export function RealtimeController() {
  const authStatus = useAuthStore((s) => s.status);
  const accessToken = useAuthStore((s) => s.accessToken);
  const setStatus = useRealtimeStore((s) => s.setStatus);
  const setErrorMessage = useRealtimeStore((s) => s.setErrorMessage);
  const setLastDraw = useRealtimeStore((s) => s.setLastDraw);
  const applyPrivateState = useRealtimeStore((s) => s.applyPrivateState);
  const pushToast = useUiStore((s) => s.pushToast);

  useEffect(() => {
    return realtimeSocket.subscribeStatus(setStatus);
  }, [setStatus]);

  useEffect(() => {
    if (authStatus === "authenticated" && accessToken) {
      realtimeSocket.connect(accessToken);
      return () => realtimeSocket.disconnect();
    }
    realtimeSocket.disconnect();
  }, [authStatus, accessToken]);

  useEffect(() => {
    return realtimeSocket.subscribe("/user/queue/events", (event: WebSocketEvent) => {
      if (event.eventType === "WEBSOCKET_ERROR") {
        const payload = (event.payload ?? {}) as Partial<WebSocketErrorPayload>;
        const message = payload.message ?? "সংযোগে সমস্যা হয়েছে।";
        setErrorMessage(message);
        pushToast({ kind: "error", title: STOMP_TITLE, message });
      }
      if (event.eventType === "CARD_REVEAL") {
        const payload = event.payload as CardRevealPayload;
        if (payload?.reason === "drawn" && event.matchId) {
          setLastDraw({
            matchId: event.matchId,
            playerId: payload.playerId,
            characterId: payload.characterId,
          });
        }
      }
      // Module 23 — private snapshot replies (initial connect, resync
      // requests, and every synced action). Full cartoon-safe per-viewer
      // state including the local player's own cards.
      if (event.eventType === "PRIVATE_STATE") {
        const payload = event.payload as BackendGameState;
        const matchId =
          (typeof payload?.matchId === "string" ? payload.matchId : null) ??
          event.matchId;
        if (matchId && payload) {
          applyPrivateState(matchId, payload);
        }
      }
    });
  }, [setErrorMessage, setLastDraw, pushToast, applyPrivateState]);

  return null;
}