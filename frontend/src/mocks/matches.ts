import type { GamePlayer, GameState, MatchStatus } from "@/types/game";
import { MOCK_USERS } from "./users";

let cardSeq = 0;
function influenceCard(character: "minister" | "ghatok" | "dalal" | "amla" | "goyenda") {
  cardSeq += 1;
  return {
    id: `card-${cardSeq}`,
    characterId: character,
    revealed: false,
  };
}

function makePlayer(
  index: number,
  user: (typeof MOCK_USERS)[number],
  opts: Partial<GamePlayer> = {},
): GamePlayer {
  return {
    id: `p-${index + 1}`,
    userId: user.id,
    username: user.username,
    displayName: user.displayName,
    isHost: index === 0,
    isAlive: true,
    isTurn: index === 0,
    coins: [2, 2, 4, 6][index] ?? 2,
    influenceCards: [influenceCard("minister"), influenceCard("ghatok")],
    seatIndex: index,
    ...opts,
  };
}

const players = [
  makePlayer(0, MOCK_USERS[0]!, { coins: 5 }),
  makePlayer(1, MOCK_USERS[1]!, { isTurn: false, coins: 3 }),
  makePlayer(2, MOCK_USERS[2]!, { isTurn: true, coins: 8 }),
  makePlayer(3, MOCK_USERS[3]!, { isTurn: false, coins: 2 }),
];

export const MOCK_GAME_STATE: GameState = {
  matchId: "match-1",
  roomId: "room-1",
  status: "IN_PROGRESS" as MatchStatus,
  phase: "action_resolution",
  players,
  currentTurnPlayerId: players[2]!.id,
  turnOrder: players.map((p) => p.id),
  turnNumber: 7,
  deckCount: 11,
  revealedCardsCount: 2,
  winnerPlayerId: null,
  activeAction: { action: "tax", claimedCharacter: "minister" },
  pendingChallenge: null,
  pendingBlock: null,
  log: [
    {
      id: "log-1",
      timestamp: new Date().toISOString(),
      text: "গেম শুরু! সবাই 2টি গোপন কার্ড পেয়েছে।",
      kind: "info",
    },
    {
      id: "log-2",
      timestamp: new Date().toISOString(),
      text: "শাপলা কর আদায় অ্যাকশন চ্যালেঞ্জ করে মন্ত্রী প্রমাণ পেল।",
      kind: "challenge",
    },
    {
      id: "log-3",
      timestamp: new Date().toISOString(),
      text: "যন্ত্রণা মন্ত্রী দাবি করে কর আদায় ঘোষণা করেছে — সাড়া দিন!",
      kind: "action",
    },
  ],
  startedAt: new Date(Date.now() - 1000 * 60 * 9).toISOString(),
};

export const MOCK_AUTO_PLAYER: GamePlayer = {
  id: "p-auto",
  userId: "auto",
  username: "bot",
  displayName: "AI সহকারী",
  isHost: false,
  isAlive: true,
  isTurn: false,
  coins: 2,
  influenceCards: [influenceCard("dalal"), influenceCard("amla")],
  seatIndex: 9,
};

/**
 * Finished game used to demo the game-over screen (/game/match-2).
 * All players except the winner are eliminated; the winner keeps two
 * influence cards and finishes with the most coins.
 */
export const MOCK_FINISHED_GAME_STATE: GameState = {
  matchId: "match-2",
  roomId: "room-2",
  status: "FINISHED" as MatchStatus,
  phase: "game_over",
  players: [
    makePlayer(0, MOCK_USERS[0]!, { isTurn: false, coins: 8 }),
    makePlayer(1, MOCK_USERS[1]!, {
      isTurn: false,
      isAlive: false,
      coins: 0,
      influenceCards: [],
    }),
    makePlayer(2, MOCK_USERS[2]!, {
      isTurn: false,
      isAlive: false,
      coins: 0,
      influenceCards: [],
    }),
    makePlayer(3, MOCK_USERS[3]!, {
      isTurn: false,
      isAlive: false,
      coins: 0,
      influenceCards: [],
    }),
  ],
  currentTurnPlayerId: null,
  turnOrder: ["p-1", "p-2", "p-3", "p-4"],
  turnNumber: 14,
  deckCount: 6,
  revealedCardsCount: 6,
  winnerPlayerId: "p-1",
  activeAction: null,
  pendingChallenge: null,
  pendingBlock: null,
  log: [
    {
      id: "fin-1",
      timestamp: new Date().toISOString(),
      text: "শাপলা শেষ খেলোয়াড় হিসেবে বিজয়ী!",
      kind: "info",
    },
    {
      id: "fin-2",
      timestamp: new Date().toISOString(),
      text: "প্রকৃতি ক্ষমতা দখলে অপসারিত হয়েছে।",
      kind: "elimination",
    },
    {
      id: "fin-3",
      timestamp: new Date().toISOString(),
      text: "যন্ত্রণা সরিয়ে দেওয়ায় অপসারিত হয়েছে।",
      kind: "elimination",
    },
    {
      id: "fin-4",
      timestamp: new Date().toISOString(),
      text: "সংগ্রাম ক্ষমতা দখলে অপসারিত হয়েছে।",
      kind: "elimination",
    },
  ],
  startedAt: new Date(Date.now() - 1000 * 60 * 20).toISOString(),
  endedAt: new Date(Date.now() - 1000 * 60 * 2).toISOString(),
};