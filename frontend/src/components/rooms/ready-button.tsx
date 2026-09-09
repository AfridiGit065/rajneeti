"use client";

import { Button } from "@/components/ui/button";
import { CheckCheck, TimerReset } from "@/components/ui/icons";

interface ReadyButtonProps {
  isReady: boolean;
  disabled?: boolean;
  loading?: boolean;
  onToggle: () => void;
}

/** Toggles the current player's ready state (রেডি ⇄ আনরেডি). */
export function ReadyButton({ isReady, disabled, loading, onToggle }: ReadyButtonProps) {
  return isReady ? (
    <Button
      type="button"
      variant="secondary"
      fullWidth
      disabled={disabled}
      loading={loading}
      onClick={onToggle}
    >
      <TimerReset className="size-4" aria-hidden />
      আনরেডি
    </Button>
  ) : (
    <Button
      type="button"
      variant="premium"
      fullWidth
      disabled={disabled}
      loading={loading}
      onClick={onToggle}
    >
      <CheckCheck className="size-4" aria-hidden />
      রেডি
    </Button>
  );
}