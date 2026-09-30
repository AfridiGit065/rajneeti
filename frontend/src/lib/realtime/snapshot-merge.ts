import type { GameState } from "@/types/game";

/**
 * Merges a realtime snapshot into the state the board is already showing.
 *
 * The server broadcasts two scopes. A `public` snapshot is what every client
 * receives: it hides other players' hands and, crucially, never carries the
 * acting player's Exchange card pool, which is only ever sent to that player in
 * a `private` snapshot. Replacing the whole state with a public snapshot would
 * therefore drop an in-flight Exchange choice (and the visible hand count) the
 * moment any broadcast lands — the selection UI would appear and vanish again.
 *
 * Private snapshots are authoritative and applied as-is.
 */
export function mergeSnapshotIntoGame(
  prev: GameState | null,
  incoming: GameState,
  selfId: string,
  scope: "public" | "private",
): GameState {
  if (scope !== "public" || !prev) return incoming;

  const patch: Partial<GameState> = {};

  const prevMe = prev.players.find((player) => player.userId === selfId);
  const hasRealCards =
    prevMe !== undefined &&
    prevMe.influenceCards.length > 0 &&
    !prevMe.influenceCards[0].id.startsWith("hidden-");
  if (hasRealCards && prevMe) {
    patch.players = incoming.players.map((player) =>
      player.userId === selfId
        ? {
            ...player,
            influenceCards: prevMe.influenceCards.slice(
              0,
              player.influenceCards.length,
            ),
          }
        : player,
    );
  }

  // Keep the offered Exchange pool while that action is still pending; once the
  // action resolves, the public snapshot is the truth and the pool is gone.
  const exchangeStillPending =
    incoming.activeAction?.action === "exchange" &&
    prev.activeAction?.action === "exchange";
  if (!incoming.exchangePool && prev.exchangePool && exchangeStillPending) {
    patch.exchangePool = prev.exchangePool;
  }

  return Object.keys(patch).length > 0 ? { ...incoming, ...patch } : incoming;
}