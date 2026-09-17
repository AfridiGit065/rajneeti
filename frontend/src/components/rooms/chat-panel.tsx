"use client";

import { useEffect, useRef, useState } from "react";
import { MessageSquare } from "@/components/ui/icons";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { realtimeSocket } from "@/lib/websocket/stomp-client";
import { useRealtimeStore } from "@/store/realtime-store";
import { useAuthStore } from "@/store/auth-store";
import { cn } from "@/lib/cn";

function formatTime(timestamp: string): string {
  const date = new Date(timestamp);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
}

const EMPTY_MESSAGES: NonNullable<
  ReturnType<typeof useRealtimeStore.getState>["chatMessages"][string]
> = [];

export function ChatPanel({ roomId }: { roomId: string }) {
  const messages = useRealtimeStore(
    (s) => s.chatMessages[roomId] ?? EMPTY_MESSAGES,
  );
  const currentUser = useAuthStore((s) => s.user);
  const [draft, setDraft] = useState("");
  const scrollRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const el = scrollRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  }, [messages]);

  function send() {
    const message = draft.trim();
    if (!message) return;
    realtimeSocket.sendChat(roomId, message);
    setDraft("");
  }

  return (
    <div className="flex h-full flex-col rounded-2xl border border-forest-500/25 bg-surface panel-emboss">
      <div className="flex items-center gap-2 border-b border-forest-500/20 px-4 py-3">
        <MessageSquare className="size-4 text-gold-400" aria-hidden />
        <h3 className="text-sm font-semibold text-ivory">রুম চ্যাট</h3>
        <span className="ml-auto text-xs text-muted">{messages.length} বার্তা</span>
      </div>

      <div ref={scrollRef} className="max-h-72 flex-1 space-y-3 overflow-y-auto px-4 py-4">
        {messages.length === 0 ? (
          <p className="py-6 text-center text-sm text-muted">কোনো বার্তা নেই। কথা শুরু করো!</p>
        ) : (
          messages.map((message) => {
            const mine = message.senderId === currentUser?.id;
            return (
              <div key={message.id} className={cn("flex flex-col", mine ? "items-end" : "items-start")}>
                <div className="flex items-baseline gap-2">
                  <span
                    className={cn(
                      "text-xs font-medium",
                      mine ? "text-gold-300" : "text-parchment-300",
                    )}
                  >
                    {mine ? "তুমি" : message.senderUsername}
                  </span>
                  <span className="text-[10px] uppercase text-muted">
                    {formatTime(message.timestamp)}
                  </span>
                </div>
                <div
                  className={cn(
                    "mt-0.5 max-w-[85%] rounded-2xl px-3 py-2 text-sm break-words",
                    mine
                      ? "rounded-br-sm bg-gradient-to-b from-forest-500 to-forest-600 text-deep-950"
                      : "rounded-bl-sm bg-deep-800 text-ivory",
                  )}
                >
                  {message.message}
                </div>
              </div>
            );
          })
        )}
      </div>

      <form
        className="flex gap-2 border-t border-forest-500/20 p-3"
        onSubmit={(e) => {
          e.preventDefault();
          send();
        }}
      >
        <Input
          aria-label="চ্যাট বার্তা"
          placeholder="বার্তা লিখো…"
          value={draft}
          maxLength={500}
          onChange={(e) => setDraft(e.target.value)}
        />
        <Button type="submit" variant="premium" size="md" disabled={!draft.trim()}>
          পাঠান
        </Button>
      </form>
    </div>
  );
}