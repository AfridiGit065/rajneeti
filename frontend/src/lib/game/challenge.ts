import type { CharacterId } from "@/types/character";
import type { InfluenceCard } from "@/types/game";

export type ChallengeVerdict = "true_claim" | "bluff";

export interface ChallengeSimulation {
  verdict: ChallengeVerdict;
  revealedCharacterId: CharacterId;
}

/**
 * Mock challenge resolver. In a real build this decision lives on the game
 * server; here it is derived deterministically from the claimant's hand so the
 * reveal sequence stays honest — no hidden card is exposed before the reveal.
 */
export function simulateChallenge(
  claimantCards: InfluenceCard[],
  claimed: CharacterId,
): ChallengeSimulation {
  const held = claimantCards.find((card) => card.characterId === claimed);
  if (held) {
    return { verdict: "true_claim", revealedCharacterId: held.characterId };
  }
  return { verdict: "bluff", revealedCharacterId: claimed };
}

export const CHALLENGE_RESPONSE_SECONDS = 15;