"use client";

import { Dialog } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Swords, Users, CheckCheck } from "@/components/ui/icons";

interface StartMatchDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  playerCount: number;
  maxPlayers: number;
  allReady: boolean;
  loading?: boolean;
}

export function StartMatchDialog({
  open,
  onClose,
  onConfirm,
  playerCount,
  maxPlayers,
  allReady,
  loading = false,
}: StartMatchDialogProps) {
  const canStart = playerCount >= 2 && allReady;
  return (
    <Dialog
      open={open}
      onClose={onClose}
      title="Start the match?"
      subtitle="The match will begin when all players are ready."
      icon={<Swords className="size-5" aria-hidden />}
      footer={
        <>
          <Button variant="ghost" onClick={onClose} disabled={loading}>
            Cancel
          </Button>
          <Button variant="premium" onClick={onConfirm} loading={loading} disabled={!canStart}>
            <Swords className="size-4" aria-hidden />
            Start Game
          </Button>
        </>
      }
    >
      <ul className="space-y-2.5">
        <li className="flex items-center gap-2.5">
          <Users className="size-4 shrink-0 text-gold-400" aria-hidden />
          <span>
            Players: <strong className="text-ivory">{playerCount}/{maxPlayers}</strong>
            {playerCount >= 2 ? null : (
              <span className="ml-1 text-crimson-300">— Minimum 2 required</span>
            )}
          </span>
        </li>
        <li>
          <span className="flex items-center gap-2.5">
            <CheckCheck className="size-4 shrink-0 text-gold-400" aria-hidden />
            <span>
              Ready Status: <strong className="text-ivory">{allReady ? "All Ready" : "Not All Ready"}</strong>
            </span>
          </span>
        </li>
        {!canStart ? (
          <li className="mt-3 rounded-lg border border-crimson-500/35 bg-crimson-600/10 px-3 py-2 text-xs text-crimson-200">
            Can start once all players are ready and at least 2 players have joined.
          </li>
        ) : null}
      </ul>
    </Dialog>
  );
}