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