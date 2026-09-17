import type { BotDifficulty } from "@/types/room";

export function botDifficultyLabel(difficulty?: BotDifficulty): string {
  switch (difficulty) {
    case "EASY":
      return "Easy";
    case "HARD":
      return "Hard";
    case "MEDIUM":
    default:
      return "Medium";
  }
}