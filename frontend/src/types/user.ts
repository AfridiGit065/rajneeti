export interface UserPublic {
  id: string;
  username: string;
  displayName: string;
  avatarInitial: string;
  level: number;
  email?: string;
  avatarUrl?: string;
  rating?: number;
  titled?: boolean;
}

export interface AuthSession {
  accessToken: string;
  refreshToken: string;
  user: UserPublic;
  expiresAt: string;
}

export interface LoginInput {
  email: string;
  password: string;
}

export interface RegisterInput {
  username: string;
  displayName: string;
  email: string;
  password: string;
}

export interface ProfileStats {
  wins: number;
  losses: number;
  draws: number;
  winRate: number;
  gamesPlayed: number;
  totalCoinsEarned: number;
  bluffsSucceeded: number;
  challengesWon: number;
  eliminations: number;
}

export interface LeaderboardEntry {
  rank: number;
  user: UserPublic;
  points: number;
  rating?: number;
  wins: number;
  gamesPlayed: number;
  winRate: number;
}

export interface MatchParticipant {
  displayName: string;
  avatarInitial: string;
  position: number;
  isCurrentUser?: boolean;
}

export interface MatchHistoryEntry {
  matchId: string;
  playedAt: string;
  modeName?: string;
  opponentName: string;
  result: "win" | "loss" | "draw";
  durationMinutes: number;
  position: number;
  playerCount: number;
  ratingChange: number;
  participants: MatchParticipant[];
}

export interface GameSettings {
  soundEnabled: boolean;
  musicEnabled: boolean;
  notificationsEnabled: boolean;
  language: "en" | "bn";
  reducedMotion: boolean;
  theme: "emerald" | "midnight";
  confirmCoup: boolean;
  confirmAssassination: boolean;
}
