export type CharacterId = "minister" | "ghatok" | "dalal" | "amla" | "goyenda";

export interface CharacterAbility {
  nameBn: string;
  nameEn: string;
  cost?: number;
  effect: string;
}

export interface Character {
  id: CharacterId;
  nameBn: string;
  nameEn: string;
  role: string;
  imagePath: string;
  ability: CharacterAbility;
  accent: "crimson" | "gold" | "forest" | "parchment";
}

export const CARD_BACK_PATH = "/assets/cards/back.png";