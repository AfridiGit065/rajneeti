import type { LeaderboardEntry, MatchHistoryEntry } from "@/types/user";
import { MOCK_USERS } from "./users";

export const MOCK_LEADERBOARD: LeaderboardEntry[] = [
  { rank: 1, user: MOCK_USERS[2]!, points: 5820, wins: 84, gamesPlayed: 121, winRate: 0.69 },
  { rank: 2, user: MOCK_USERS[0]!, points: 5110, wins: 71, gamesPlayed: 108, winRate: 0.66 },
  { rank: 3, user: MOCK_USERS[4]!, points: 4780, wins: 66, gamesPlayed: 104, winRate: 0.63 },
  { rank: 4, user: MOCK_USERS[1]!, points: 3990, wins: 55, gamesPlayed: 92, winRate: 0.6 },
  { rank: 5, user: MOCK_USERS[3]!, points: 3320, wins: 44, gamesPlayed: 81, winRate: 0.54 },
  { rank: 6, user: MOCK_USERS[5]!, points: 2850, wins: 38, gamesPlayed: 76, winRate: 0.5 },
];

export const MOCK_MATCH_HISTORY: MatchHistoryEntry[] = [
  { matchId: "match-1", playedAt: new Date(Date.now() - 1000 * 60 * 22).toISOString(), opponentName: "যন্ত্রণা", result: "win", durationMinutes: 14, position: 1, playerCount: 4 },
  { matchId: "match-0", playedAt: new Date(Date.now() - 1000 * 60 * 60 * 3).toISOString(), opponentName: "সংগ্রাম", result: "loss", durationMinutes: 17, position: 3, playerCount: 5 },
  { matchId: "match-x1", playedAt: new Date(Date.now() - 1000 * 60 * 60 * 26).toISOString(), opponentName: "বঙ্গভূমি", result: "win", durationMinutes: 11, position: 1, playerCount: 3 },
  { matchId: "match-x2", playedAt: new Date(Date.now() - 1000 * 60 * 60 * 49).toISOString(), opponentName: "প্রকৃতি", result: "draw", durationMinutes: 20, position: 2, playerCount: 2 },
];