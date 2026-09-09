import type { Character } from "@/types/character";

/** Game roster. Card artwork lives at /public/assets/cards/<id>.png and can be swapped freely. */
export const CHARACTERS: readonly Character[] = [
  {
    id: "minister",
    nameBn: "মন্ত্রী",
    nameEn: "THE MINISTER",
    role: "সংগ্রাহক · Collector",
    imagePath: "/assets/cards/minister.png",
    accent: "gold",
    ability: {
      nameBn: "কর আদায়",
      nameEn: "Tax Collection",
      cost: 0,
      effect: "ট্যাক্স আদায়: +3 কয়েন",
    },
  },
  {
    id: "ghatok",
    nameBn: "ঘাতক",
    nameEn: "THE ASSASSIN",
    role: "অন্ধকার ব্রোকার · Dark Broker",
    imagePath: "/assets/cards/ghatok.png",
    accent: "crimson",
    ability: {
      nameBn: "সরিয়ে দেওয়া",
      nameEn: "Assassinate",
      cost: 3,
      effect: "টার্গেট 1 ইনফ্লুয়েন্স হারায়",
    },
  },
  {
    id: "dalal",
    nameBn: "দালাল",
    nameEn: "THE BROKER",
    role: "মধ্যস্বত্বভোগী · Middleman",
    imagePath: "/assets/cards/dalal.png",
    accent: "forest",
    ability: {
      nameBn: "চুরি",
      nameEn: "Steal",
      cost: 0,
      effect: "টার্গেট থেকে 2 কয়েন চুরি",
    },
  },
  {
    id: "amla",
    nameBn: "আমলা",
    nameEn: "THE BUREAUCRAT",
    role: "চক্রপুরুষ · Machine Man",
    imagePath: "/assets/cards/amla.png",
    accent: "parchment",
    ability: {
      nameBn: "কার্ড বদল",
      nameEn: "Exchange",
      cost: 0,
      effect: "2 কার্ড নাও, বেছে রাখো 2টি",
    },
  },
  {
    id: "goyenda",
    nameBn: "গোয়েন্দা",
    nameEn: "THE DETECTIVE",
    role: "তদন্তকারী · Investigator",
    imagePath: "/assets/cards/goyenda.png",
    accent: "crimson",
    ability: {
      nameBn: "সুরক্ষা",
      nameEn: "Assassination Guard",
      cost: 0,
      effect: "সরিয়ে দেওয়া হামলা ব্লক করে",
    },
  },
];

export const CHARACTER_MAP: Readonly<Record<string, Character>> =
  Object.fromEntries(CHARACTERS.map((c) => [c.id, c]));

export function isCharacterId(value: string): value is Character["id"] {
  return Object.prototype.hasOwnProperty.call(CHARACTER_MAP, value);
}