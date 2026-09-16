"use client";

import { Badge, type BadgeTone } from "@/components/ui/badge";
import { useRealtimeStore } from "@/store/realtime-store";

const STATUS_TEXT = {
  idle: "সংযোগ নেই",
  connecting: "সংযোগ হচ্ছে…",
  connected: "সংযুক্ত",
  disconnected: "সংযোগ বিচ্ছিন্ন",
} as const;

const STATUS_TONE: Record<keyof typeof STATUS_TEXT, BadgeTone> = {
  idle: "neutral",
  connecting: "gold",
  connected: "emerald",
  disconnected: "crimson",
};

export function RealtimeStatusBadge() {
  const status = useRealtimeStore((s) => s.status);
  return <Badge tone={STATUS_TONE[status]}>{STATUS_TEXT[status]}</Badge>;
}