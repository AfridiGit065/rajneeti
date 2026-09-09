import { CHARACTERS } from "./characters";

/** Sanctioned game constants — do not alter casually. */
export const RULES = {
  maxInfluencePerPlayer: 2,
  startingCoins: 2,
  minPlayers: 2,
  maxPlayers: 6,
  copiesPerCharacter: 3,
  deckSize: CHARACTERS.length * 3,
  coupCost: 7,
  forcedCoupThreshold: 10,
  assassinateCost: 3,
  taxGain: 3,
  stealGain: 2,
  incomeGain: 1,
  foreignAidGain: 2,
  gameLengthMinutes: [10, 20],
} as const;

/** Default player avatar image, used until the player provides one. */
export const AVATAR_PLACEHOLDER = "/assets/ui/avatar-placeholder.png";

export const DECOMPOSED_INFLUENCE_LOSS_COST = 1;

export { CHARACTERS };

export const MAX_CHARACTER_COPIES = RULES.copiesPerCharacter;