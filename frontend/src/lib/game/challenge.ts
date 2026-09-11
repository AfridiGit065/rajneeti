import type { CharacterId } from "@/types/character";
import type { ChallengeResolution } from "@/types/game";
import type { InfluenceCard } from "@/types/game";

export type ChallengeVerdict = "true_claim" | "bluff";

export interface ChallengeSimulation {
  verdict: ChallengeVerdict;
  revealedCharacterId: CharacterId;
}

/**
 * Maps the server-authoritative challenge result to the frontend simulation
 * type so the existing reveal sequence keeps working without redesigning the
 * presentation layer. The backend already made the verdict decision; this is
 * purely a shape adapter.
 */
export function challengeToSimulation(
  challenge: ChallengeResolution,
): ChallengeSimulation {
  return {
    verdict: challenge.result === "failed" ? "true_claim" : "bluff",
    revealedCharacterId: (challenge.revealedCharacterId as CharacterId) ?? "minister",
  };
}

/**
 * Mock challenge resolver kept as a fallback for demo/offline mode. The real
 * flow now uses {@link challengeToSimulation} instead.
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