import type {
  LeaderboardEntry,
  MatchHistoryEntry,
  ProfileStats,
} from "@/types/user";
import type { Result } from "@/types/api";

export interface MetaRepository {
  getLeaderboard(): Promise<Result<LeaderboardEntry[]>>;
  getMatchHistory(userId: string): Promise<Result<MatchHistoryEntry[]>>;
  getProfileStats(userId: string): Promise<Result<ProfileStats>>;
}