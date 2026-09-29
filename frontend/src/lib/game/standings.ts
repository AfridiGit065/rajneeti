import type { GamePlayer, GameState } from "@/types/game";

export interface RankingEntry {
  player: GamePlayer;
  rank: number;
}

/**
 * Authoritative final standings: sorted by backend-provided finalRank ascending.
 * The winner is always rank 1 (surviving player), followed by eliminated players
 * in reverse elimination order (last eliminated = rank 2, first eliminated = lowest rank).
 * Does NOT rank by coins, database ID, seat index, or array index.
 */
export function buildStandings(game: GameState): RankingEntry[] {
  return [...game.players]
    .map((player) => {
      const isWinner =
        player.finalRank === 1 ||
        player.id === game.winnerPlayerId ||
        player.userId === game.winnerPlayerId ||
        (game.winnerPlayerId == null && player.isAlive);
      const rank = player.finalRank ?? (isWinner ? 1 : undefined);
      return { player, rank };
    })
    .sort((a, b) => {
      const rankA = a.rank ?? 999;
      const rankB = b.rank ?? 999;
      if (rankA !== rankB) return rankA - rankB;
      if (a.player.isAlive !== b.player.isAlive) return a.player.isAlive ? -1 : 1;
      return 0;
    })
    .map((entry, index) => ({
      player: entry.player,
      rank: entry.rank ?? index + 1,
    }));
}
