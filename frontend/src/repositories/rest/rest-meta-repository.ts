import { apiClient } from "@/lib/api-client";
import type {
  LeaderboardEntry,
  MatchHistoryEntry,
  MatchParticipant,
  ProfileStats,
  UserPublic,
} from "@/types/user";
import type { MetaRepository } from "../meta-repository";
import type { Result } from "@/types/api";

interface BackendLeaderboardEntry {
  rank: number;
  userId: string;
  username: string;
  avatarUrl?: string | null;
  rating: number;
  wins: number;
  losses: number;
  totalMatches: number;
  winRate: number;
}

interface BackendMatchHistoryEntry {
  matchId: string;
  playedAt: string;
  durationMinutes: number;
  position: number;
  playerCount: number;
  result: "WIN" | "LOSS" | "DRAW";
  eliminated: boolean;
  participants: Array<{
    userId: string;
    username: string;
    avatarUrl?: string | null;
    seatNumber: number;
    finalRank?: number | null;
    isCurrentUser: boolean;
  }>;
}

interface BackendStatistics {
  userId: string;
  username: string;
  rating: number;
  totalMatches: number;
  wins: number;
  losses: number;
  winRate: number;
  totalCoinsEarned: number;
  totalCoinsSpent: number;
}

function publicUser(id: string, username: string, avatarUrl?: string | null): UserPublic {
  return {
    id,
    username,
    displayName: username,
    avatarInitial: username.slice(0, 2).toUpperCase(),
    level: 1,
    avatarUrl: avatarUrl ?? undefined,
    rating: undefined,
  };
}

function participant(
  p: BackendMatchHistoryEntry["participants"][number],
): MatchParticipant {
  return {
    displayName: p.username,
    avatarInitial: p.username.slice(0, 2).toUpperCase(),
    position: p.seatNumber,
    isCurrentUser: p.isCurrentUser,
  };
}

export class RestMetaRepository implements MetaRepository {
  async getLeaderboard(): Promise<Result<LeaderboardEntry[]>> {
    const result = await apiClient.get<BackendLeaderboardEntry[]>("/api/leaderboard");
    if (!result.ok) return result;
    return {
      ok: true,
      data: result.data.map((entry) => ({
        rank: entry.rank,
        user: publicUser(entry.userId, entry.username, entry.avatarUrl),
        points: entry.rating,
        rating: entry.rating,
        wins: entry.wins,
        gamesPlayed: entry.totalMatches,
        winRate: entry.winRate,
      })),
    };
  }

  async getMatchHistory(_userId: string): Promise<Result<MatchHistoryEntry[]>> {
    const result = await apiClient.get<BackendMatchHistoryEntry[]>("/api/me/match-history");
    if (!result.ok) return result;
    return {
      ok: true,
      data: result.data.map((entry) => {
        const participants = entry.participants.map(participant);
        const opponent = participants.find((p) => !p.isCurrentUser) ?? participants[0];
        return {
          matchId: entry.matchId,
          playedAt: entry.playedAt,
          opponentName: opponent?.displayName ?? "",
          result: entry.result === "WIN" ? "win" : entry.result === "LOSS" ? "loss" : "draw",
          durationMinutes: entry.durationMinutes,
          position: entry.position,
          playerCount: entry.playerCount,
          ratingChange: 0,
          participants,
        };
      }),
    };
  }

  async getProfileStats(_userId: string): Promise<Result<ProfileStats>> {
    const result = await apiClient.get<BackendStatistics>("/api/profile/statistics");
    if (!result.ok) return result;
    const stats = result.data;
    return {
      ok: true,
      data: {
        wins: stats.wins,
        losses: stats.losses,
        draws: 0,
        winRate: stats.winRate,
        gamesPlayed: stats.totalMatches,
        totalCoinsEarned: stats.totalCoinsEarned,
        bluffsSucceeded: 0,
        challengesWon: 0,
        eliminations: 0,
      },
    };
  }
}