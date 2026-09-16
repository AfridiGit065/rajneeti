"use client";

import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import type { WebSocketEvent } from "@/types/websocket";

const API_URL = (process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080").replace(/\/$/, "");

const SOCKET_URL = `${API_URL.replace(/^http/, "ws")}/ws`;

export type RealtimeStatus = "idle" | "connecting" | "connected" | "disconnected";

type EventHandler = (event: WebSocketEvent) => void;
type StatusHandler = (status: RealtimeStatus) => void;
type NativeSubscription = ReturnType<Client["subscribe"]>;

interface DestinationEntry {
  handlers: Set<EventHandler>;
  subscription: NativeSubscription | null;
}

function parseEvent(body: string): WebSocketEvent | null {
  try {
    const event = JSON.parse(body) as WebSocketEvent;
    if (!event || typeof event.eventType !== "string") return null;
    return event;
  } catch {
    return null;
  }
}

/**
 * Singleton STOMP-over-SockJS client for the Module 22 realtime stream.
 * Lazily creates native subscriptions per destination on connect and replays
 * requested destinations across reconnects. Dispatches only the parsed
 * {@link WebSocketEvent} envelope to subscribers.
 */
class RealtimeSocket {
  private client: Client | null = null;
  private token: string | null = null;
  private status: RealtimeStatus = "idle";
  private statusHandlers = new Set<StatusHandler>();
  private destinations = new Map<string, DestinationEntry>();

  subscribeStatus(handler: StatusHandler): () => void {
    this.statusHandlers.add(handler);
    return () => {
      this.statusHandlers.delete(handler);
    };
  }

  private setStatus(status: RealtimeStatus) {
    if (this.status === status) return;
    this.status = status;
    this.statusHandlers.forEach((handler) => handler(status));
  }

  connect(token: string) {
    if (this.client?.connected && this.token === token) return;
    this.disconnect();
    this.token = token;

    const client = new Client({
      webSocketFactory: () => new SockJS(SOCKET_URL) as unknown as WebSocket,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      heartbeatIncoming: 0,
      heartbeatOutgoing: 0,
    });

    client.onConnect = () => {
      this.setStatus("connected");
      this.destinations.forEach((entry, destination) => {
        if (!entry.subscription) {
          entry.subscription = this.subscribeNative(client, destination, entry);
        }
      });
    };

    client.onWebSocketClose = () => {
      this.setStatus("disconnected");
    };

    client.onStompError = () => {
      this.setStatus("disconnected");
    };

    this.client = client;
    this.setStatus("connecting");
    client.activate();
  }

  disconnect() {
    this.client?.deactivate();
    this.client = null;
    this.token = null;
    this.destinations.forEach((entry) => {
      entry.subscription = null;
    });
    this.setStatus("idle");
  }

  private subscribeNative(
    client: Client,
    destination: string,
    entry: DestinationEntry,
  ): NativeSubscription {
    return client.subscribe(destination, (frame) => {
      const event = parseEvent(frame.body);
      if (!event) return;
      entry.handlers.forEach((handler) => handler(event));
    });
  }

  subscribe(destination: string, handler: EventHandler): () => void {
    let entry = this.destinations.get(destination);
    if (!entry) {
      entry = { handlers: new Set(), subscription: null };
      this.destinations.set(destination, entry);
    }
    entry.handlers.add(handler);

    if (this.client?.connected && !entry.subscription) {
      entry.subscription = this.subscribeNative(this.client, destination, entry);
    }

    return () => {
      const current = this.destinations.get(destination);
      if (!current) return;
      current.handlers.delete(handler);
      if (current.handlers.size === 0) {
        current.subscription?.unsubscribe();
        this.destinations.delete(destination);
      }
    };
  }

  sendJson(destination: string, body: unknown) {
    if (!this.client?.connected) return;
    this.client.publish({ destination, body: JSON.stringify(body) });
  }

  sendChat(roomId: string, message: string) {
    this.sendJson(`/app/rooms/${roomId}/chat`, { message });
  }
}

export const realtimeSocket = new RealtimeSocket();