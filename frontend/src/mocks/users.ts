import type { UserPublic } from "@/types/user";

export const MOCK_CURRENT_USER: UserPublic = {
  id: "u-1",
  username: "shapla",
  displayName: "শাপলা",
  avatarInitial: "শা",
  level: 12,
  titled: true,
};

export const MOCK_DEMO_CREDENTIALS = {
  email: "shapla@example.com",
  password: "shapla123",
} as const;

export const MOCK_USERS: UserPublic[] = [
  MOCK_CURRENT_USER,
  {
    id: "u-2",
    username: "shongram",
    displayName: "সংগ্রাম",
    avatarInitial: "স",
    level: 9,
  },
  {
    id: "u-3",
    username: "jontrona",
    displayName: "যন্ত্রণা",
    avatarInitial: "য",
    level: 15,
    titled: true,
  },
  {
    id: "u-4",
    username: "prokriti",
    displayName: "প্রকৃতি",
    avatarInitial: "প",
    level: 7,
  },
  {
    id: "u-5",
    username: "bongobhumi",
    displayName: "বঙ্গভূমি",
    avatarInitial: "ব",
    level: 11,
  },
  {
    id: "u-6",
    username: "moylapokkho",
    displayName: "ময়লা পক্ষ",
    avatarInitial: "ম",
    level: 5,
  },
];