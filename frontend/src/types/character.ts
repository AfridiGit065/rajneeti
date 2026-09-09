export type CharacterId = "minister" | "ghatok" | "dalal" | "amla" | "goyenda";

export interface CharacterAbility {
  nameBn: string;
  nameEn: string;
  cost?: number;
  effect: string;
  effectDetail?: string;
}

export interface CharacterBlock {
  nameBn: string;
  nameEn: string;
  detail: string;
}

export interface Character {
  id: CharacterId;
  nameBn: string;
  nameEn: string;
  taglineBn: string;
  role: string;
  description: string;
  imagePath: string;
  ability: CharacterAbility;
  block: CharacterBlock;
  quote: string;
  accent: "crimson" | "gold" | "forest" | "parchment";
}

export const CARD_BACK_PATH = "/assets/cards/back.png";