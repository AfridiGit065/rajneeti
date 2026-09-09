import type {
  LeaderboardEntry,
  MatchHistoryEntry,
  MatchParticipant,
  UserPublic,
} from "@/types/user";
import { MOCK_USERS } from "./users";

function participant(
  user: UserPublic,
  position: number,
  isCurrentUser = false,
): MatchParticipant {
  return {
    displayName: user.displayName,
    avatarInitial: user.avatarInitial,
    position,
    isCurrentUser,
  };
}

const [shapla, shongram, jontrona, prokriti, bongobhumi, moylapokkho] = [
  MOCK_USERS[0]!,
  MOCK_USERS[1]!,
  MOCK_USERS[2]!,
  MOCK_USERS[3]!,
  MOCK_USERS[4]!,
  MOCK_USERS[5]!,
];

export const MOCK_LEADERBOARD: LeaderboardEntry[] = [
  { rank: 1, user: jontrona, points: 5820, rating: 1680, wins: 84, gamesPlayed: 121, winRate: 0.69 },
  { rank: 2, user: shapla, points: 5110, rating: 1520, wins: 71, gamesPlayed: 108, winRate: 0.66 },
  { rank: 3, user: bongobhumi, points: 4780, rating: 1445, wins: 66, gamesPlayed: 104, winRate: 0.63 },
  { rank: 4, user: shongram, points: 3990, rating: 1310, wins: 55, gamesPlayed: 92, winRate: 0.6 },
  { rank: 5, user: prokriti, points: 3320, rating: 1190, wins: 44, gamesPlayed: 81, winRate: 0.54 },
  { rank: 6, user: moylapokkho, points: 2850, rating: 1075, wins: 38, gamesPlayed: 76, winRate: 0.5 },
];

export const MOCK_MATCH_HISTORY: MatchHistoryEntry[] = [
  {
    matchId: "match-1",
    playedAt: new Date(Date.now() - 1000 * 60 * 22).toISOString(),
    modeName: "ক্লাসিক",
    opponentName: "যন্ত্রণা",
    result: "win",
    durationMinutes: 14,
    position: 1,
    playerCount: 4,
    ratingChange: 14,
    participants: [
      participant(shapla, 1, true),
      participant(jontrona, 2),
      participant(shongram, 3),
      participant(prokriti, 4),
    ],
  },
  {
    matchId: "match-0",
    playedAt: new Date(Date.now() - 1000 * 60 * 60 * 3).toISOString(),
    modeName: "ক্লাসিক",
    opponentName: "বঙ্গভূমি",
    result: "loss",
    durationMinutes: 17,
    position: 3,
    playerCount: 5,
    ratingChange: -11,
    participants: [
      participant(bongobhumi, 1),
      participant(jontrona, 2),
      participant(shapla, 3, true),
      participant(shongram, 4),
      participant(moylapokkho, 5),
    ],
  },
  {
    matchId: "match-4",
    playedAt: new Date(Date.now() - 1000 * 60 * 60 * 26).toISOString(),
    modeName: "দ্রুত",
    opponentName: "প্রকৃতি",
    result: "win",
    durationMinutes: 11,
    position: 1,
    playerCount: 3,
    ratingChange: 9,
    participants: [
      participant(shapla, 1, true),
      participant(prokriti, 2),
      participant(moylapokkho, 3),
    ],
  },
  {
    matchId: "match-2",
    playedAt: new Date(Date.now() - 1000 * 60 * 60 * 49).toISOString(),
    modeName: "ক্লাসিক",
    opponentName: "যন্ত্রণা",
    result: "win",
    durationMinutes: 20,
    position: 1,
    playerCount: 4,
    ratingChange: 18,
    participants: [
      participant(shapla, 1, true),
      participant(jontrona, 2),
      participant(shongram, 3),
      participant(prokriti, 4),
    ],
  },
  {
    matchId: "match-5",
    playedAt: new Date(Date.now() - 1000 * 60 * 60 * 76).toISOString(),
    modeName: "ক্লাসিক",
    opponentName: "বঙ্গভূমি",
    result: "win",
    durationMinutes: 24,
    position: 1,
    playerCount: 6,
    ratingChange: 16,
    participants: [
      participant(shapla, 1, true),
      participant(bongobhumi, 2),
      participant(shongram, 3),
      participant(prokriti, 4),
      participant(moylapokkho, 5),
      participant(jontrona, 6),
    ],
  },
  {
    matchId: "match-3",
    playedAt: new Date(Date.now() - 1000 * 60 * 60 * 118).toISOString(),
    modeName: "দ্রুত",
    opponentName: "সংগ্রাম",
    result: "loss",
    durationMinutes: 9,
    position: 4,
    playerCount: 4,
    ratingChange: -13,
    participants: [
      participant(shongram, 1),
      participant(moylapokkho, 2),
      participant(prokriti, 3),
      participant(shapla, 4, true),
    ],
  },
];