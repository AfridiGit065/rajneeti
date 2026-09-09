"use client";

import { cn } from "@/lib/cn";
import { CHARACTER_MAP } from "@/lib/game/characters";
import { Modal } from "@/components/ui/modal";
import { Button } from "@/components/ui/button";
import { Swords, AlertTriangle, X } from "@/components/ui/icons";
import type { CharacterId } from "@/types/character";

interface ChallengeDialogProps {
  open: boolean;
  claimantName: string;
  claimedCharacter: CharacterId;
  onConfirm: () => void;
  onCancel: () => void;
}

export function ChallengeDialog({
  open,
  claimantName,
  claimedCharacter,
  onConfirm,
  onCancel,
}: ChallengeDialogProps) {
  const character = CHARACTER_MAP[claimedCharacter];

  return (
    <Modal
      open={open}
      onClose={onCancel}
      title="Declare a Challenge?"
      subtitle="If the claim is truthful, you will lose 1 influence"
      size="sm"
      variant="warning"
      showCloseButton={false}
    >
      {character ? (
        <div className="space-y-5">
          <div className="flex items-center gap-3 rounded-xl border border-gold-500/25 bg-deep-900/70 p-4">
            <span
              className={cn(
                "flex size-10 shrink-0 items-center justify-center rounded-full border text-sm font-bold",
                (() => {
                  switch (character.accent) {
                    case "gold":
                      return "border-gold-500/50 bg-gold-500/10 text-gold-200";
                    case "crimson":
                      return "border-crimson-500/50 bg-crimson-500/10 text-crimson-200";
                    case "forest":
                      return "border-forest-500/50 bg-forest-500/10 text-forest-200";
                    case "parchment":
                      return "border-parchment-500/40 bg-parchment-500/10 text-parchment-200";
                  }
                })(),
              )}
            >
              {character.nameBn.slice(0, 1)}
            </span>
            <div className="min-w-0 leading-tight">
              <p className="truncate text-sm text-muted">
                <span className="font-semibold text-ivory">{claimantName}</span> claims —
                &ldquo;I am {character.nameBn}&rdquo;
              </p>
              <p className="mt-0.5 font-cinzel text-[0.65rem] font-semibold uppercase tracking-[0.2em] text-gold-400">
                {character.nameEn}
              </p>
            </div>
          </div>

          <div className="flex items-start gap-2.5 rounded-xl border border-crimson-500/30 bg-crimson-500/8 px-4 py-3 text-sm text-crimson-200">
            <AlertTriangle className="mt-0.5 size-4 shrink-0" aria-hidden />
            <p>
              If truthful, <span className="font-bold">you</span> will lose 1 influence.
              If bluffing, <span className="font-bold">{claimantName}</span> will lose 1 influence
              and must reveal a card.
            </p>
          </div>

          <div className="flex justify-end gap-3">
            <Button variant="outline" size="md" onClick={onCancel}>
              <X className="size-4" aria-hidden />
              Cancel
            </Button>
            <Button variant="danger" size="md" onClick={onConfirm} className="animate-glow-pulse">
              <Swords className="size-4" aria-hidden />
              Challenge Now
            </Button>
          </div>
        </div>
      ) : null}
    </Modal>
  );
}