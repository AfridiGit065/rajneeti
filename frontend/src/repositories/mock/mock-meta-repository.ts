import type { MetaRepository } from "../meta-repository";
import type { LeaderboardEntry, MatchHistoryEntry, ProfileStats } from "@/types/user";
import { ok, type Result } from "@/types/api";
import { MOCK_LEADERBOARD, MOCK_MATCH_HISTORY } from "@/mocks/meta";

const delay = (ms = 350) => new Promise((r) => setTimeout(r, ms));

export const MOCK_PROFILE_STATS: ProfileStats = {
  wins: 71,
  losses: 29,
  draws: 8,
  winRate: 0.66,
  gamesPlayed: 108,
  totalCoinsEarned: 1420,
  bluffsSucceeded: 54,
  challengesWon: 22,
  eliminations: 38,
};

export class MockMetaRepository implements MetaRepository {
  async getLeaderboard(): Promise<Result<LeaderboardEntry[]>> {
    await delay(350);
    return ok(MOCK_LEADERBOARD);
  }

  async getMatchHistory(_userId: string): Promise<Result<MatchHistoryEntry[]>> {
    await delay(350);
    return ok(MOCK_MATCH_HISTORY);
  }

  async getProfileStats(_userId: string): Promise<Result<ProfileStats>> {
    await delay(300);
    return ok(MOCK_PROFILE_STATS);
  }
}