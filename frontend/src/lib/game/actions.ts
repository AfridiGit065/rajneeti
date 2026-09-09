import type { GameAction, GameActionId } from "@/types/game";

/**
 * Core Coup-family actions.
 * Rules must NOT be modified arbitrarily — these are the sanctioned game actions.
 */
export const ACTIONS: readonly GameAction[] = [
  {
    id: "income",
    nameBn: "আয়",
    nameEn: "Income",
    description: "+1 কয়েন অর্জন করো।",
    gain: 1,
    challengeable: false,
    blockable: false,
  },
  {
    id: "foreign_aid",
    nameBn: "বিদেশি অনুদান",
    nameEn: "Foreign Aid",
    description: "+2 কয়েন অর্জন করো। মন্ত্রী ব্লক করতে পারে।",
    gain: 2,
    blockableBy: ["minister"],
    challengeable: false,
    blockable: true,
  },
  {
    id: "tax",
    nameBn: "কর আদায়",
    nameEn: "Tax",
    description: "মন্ত্রী দাবি করো: +3 কয়েন অর্জন করো।",
    gain: 3,
    requiresCharacter: "minister",
    challengeable: true,
    blockable: false,
  },
  {
    id: "steal",
    nameBn: "চুরি",
    nameEn: "Steal",
    description: "দালাল দাবি করো: টার্গেট থেকে 2 কয়েন চুরি করো।",
    requiresCharacter: "dalal",
    gain: 2,
    blockableBy: ["dalal", "amla"],
    challengeable: true,
    blockable: true,
  },
  {
    id: "exchange",
    nameBn: "কার্ড বদল",
    nameEn: "Exchange",
    description: "আমলা দাবি করো: 2 কার্ড নাও, 2টি বেছে রাখো।",
    requiresCharacter: "amla",
    challengeable: true,
    blockable: false,
  },
  {
    id: "assassinate",
    nameBn: "সরিয়ে দেওয়া",
    nameEn: "Assassinate",
    description: "ঘাতক দাবি করো: 3 কয়েন দাও, টার্গেট 1 ইনফ্লুয়েন্স হারায়।",
    requiresCharacter: "ghatok",
    cost: 3,
    blockableBy: ["goyenda"],
    challengeable: true,
    blockable: true,
  },
  {
    id: "coup",
    nameBn: "ক্ষমতা দখল",
    nameEn: "Coup",
    description: "7 কয়েন দাও: টার্গেট 1 ইনফ্লুয়েন্স হারায়। ব্লক বা চ্যালেঞ্জ করা যায় না।",
    cost: 7,
    gain: 0,
    challengeable: false,
    blockable: false,
  },
];

export const ACTION_MAP: Readonly<Record<string, GameAction>> =
  Object.fromEntries(ACTIONS.map((a) => [a.id, a]));

export function getAction(id: GameActionId): GameAction {
  return ACTION_MAP[id]!;
}

export function canChallenge(action: GameActionId): boolean {
  return getAction(action).challengeable;
}

export function canBlock(action: GameActionId, claim: string[]): boolean {
  const definition = getAction(action);
  if (!definition.blockable || !definition.blockableBy) return false;
  return definition.blockableBy.some((id) => claim.includes(id));
}