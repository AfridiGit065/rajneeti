import type { CharacterId } from "@/types/character";
import type { InfluenceCard } from "@/types/game";
import { CHARACTERS, CHARACTER_MAP, isCharacterId } from "./characters";
import { RULES } from "./rules";

/** Builds the initial 15-card deck (5 characters × 3 copies). */
export function buildDeck(): InfluenceCard[] {
  const deck: InfluenceCard[] = [];
  let uid = 0;
  for (const character of CHARACTERS) {
    for (let copy = 0; copy < RULES.copiesPerCharacter; copy += 1) {
      deck.push({
        id: `deck-${character.id}-${copy + 1}-${uid}`,
        characterId: character.id,
        revealed: false,
      });
      uid += 1;
    }
  }
  return shuffle(deck);
}

/** Fisher–Yates shuffle (returns a new array). */
export function shuffle<T>(items: readonly T[]): T[] {
  const copy = [...items];
  for (let i = copy.length - 1; i > 0; i -= 1) {
    const j = Math.floor(Math.random() * (i + 1));
    const tmp = copy[i];
    copy[i] = copy[j]!;
    copy[j] = tmp!;
  }
  return copy;
}

/** Draws n cards off the top of the deck. */
export function draw(deck: InfluenceCard[], count: number): {
  drawn: InfluenceCard[];
  deck: InfluenceCard[];
} {
  const limit = Math.min(count, deck.length);
  return {
    drawn: deck.slice(0, limit),
    deck: deck.slice(limit),
  };
}

/** Puts cards back on top of the deck and returns the new deck. */
export function returnToTop(
  deck: InfluenceCard[],
  cards: InfluenceCard[],
): InfluenceCard[] {
  return [...cards, ...deck];
}

export function characterLabel(id: CharacterId): string {
  return CHARACTER_MAP[id]?.nameBn ?? String(id);
}

export function isKnownCharacterId(id: string): boolean {
  return isCharacterId(id);
}