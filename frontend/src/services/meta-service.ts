import { repositories } from "@/repositories";
import type {
  LeaderboardEntry,
  MatchHistoryEntry,
  ProfileStats,
} from "@/types/user";
import type { Result } from "@/types/api";

export const MetaService = {
  async getLeaderboard(): Promise<Result<LeaderboardEntry[]>> {
    return repositories.meta.getLeaderboard();
  },
  async getMatchHistory(userId: string): Promise<Result<MatchHistoryEntry[]>> {
    return repositories.meta.getMatchHistory(userId);
  },
  async getProfileStats(userId: string): Promise<Result<ProfileStats>> {
    return repositories.meta.getProfileStats(userId);
  },
};