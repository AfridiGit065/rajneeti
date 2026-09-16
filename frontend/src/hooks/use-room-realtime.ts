"use client";

import { useEffect, useRef } from "react";
import { realtimeSocket } from "@/lib/websocket/stomp-client";
import { useRealtimeStore } from "@/store/realtime-store";
import type {
  ChatMessage,
  ChatMessagePayload,
  WebSocketEvent,
} from "@/types/websocket";

/**
 * Subscribes to the room topic while the current user is a member. Broadcast
 * chat messages are routed into the realtime store; every other event is
 * forwarded to the optional callback so the caller can re-fetch room state.
 */
export function useRoomRealtime(
  roomId: string,
  enabled: boolean,
  onRoomEvent?: (event: WebSocketEvent) => void,
) {
  const callbackRef = useRef(onRoomEvent);

  useEffect(() => {
    callbackRef.current = onRoomEvent;
  });

  useEffect(() => {
    if (!enabled || !roomId) return;
    return realtimeSocket.subscribe(`/topic/rooms/${roomId}`, (event) => {
      if (event.eventType === "CHAT_MESSAGE" && event.payload) {
        const payload = event.payload as ChatMessagePayload;
        const chat: ChatMessage = {
          id: `${event.timestamp}-${payload.senderId}-${Math.random().toString(36).slice(2, 8)}`,
          senderId: payload.senderId,
          senderUsername: payload.senderUsername,
          message: payload.message,
          timestamp: event.timestamp,
        };
        useRealtimeStore.getState().pushChat(roomId, chat);
      }
      callbackRef.current?.(event);
    });
  }, [roomId, enabled]);
}