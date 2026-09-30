/**
 * Exchange card-selection rules (Amla "কার্ড বদল").
 *
 * The player is offered the 4 cards of their hand during a pending Exchange and
 * must keep EXACTLY 2 of them; the rest go back to the deck. The backend
 * (GameEngine.confirmExchange) stays authoritative: it rejects anything other
 * than exactly 2 distinct card ids that exist in the pool. This module only
 * mirrors that contract for the UI so the player can never reach a state the
 * server will refuse.
 */

import { MatchStatus, type ActionIntent } from "@/types/game";

/** The number of cards the player must keep. Mirrors the backend rule. */
export const EXCHANGE_KEEP_COUNT = 2;

export interface ExchangeSelectionState {
  /** Card ids the player currently keeps, in the order they were picked. */
  readonly selectedIds: readonly string[];
}

export function createExchangeSelection(): ExchangeSelectionState {
  return { selectedIds: [] };
}

export function exchangeSelectionCount(
  state: ExchangeSelectionState,
): number {
  return state.selectedIds.length;
}

/**
 * Drops ids that are no longer part of the offered pool. A challenge that
 * restores the hand rebuilds the pool, so a selection made before the change can
 * reference cards the server will never accept.
 */
export function pruneExchangeSelection(
  state: ExchangeSelectionState,
  poolIds: readonly string[],
): ExchangeSelectionState {
  const available = new Set(poolIds);
  const selectedIds = state.selectedIds.filter((id) => available.has(id));
  return selectedIds.length === state.selectedIds.length
    ? state
    : { selectedIds };
}

/**
 * Selects, deselects, or ignores a card:
 * - an unselected card is selected while fewer than `keepCount` cards are held;
 * - a selected card is always deselected, which frees a slot;
 * - a third card is never selected while `keepCount` cards are already held,
 *   the click is ignored rather than silently swapping a kept card out.
 *
 * Ids outside the pool are ignored. Nothing here chooses a card on the
 * player's behalf: the state starts empty and only grows by an explicit click.
 */
export function toggleExchangeCard(
  state: ExchangeSelectionState,
  cardId: string,
  poolIds: readonly string[],
  keepCount: number = EXCHANGE_KEEP_COUNT,
): ExchangeSelectionState {
  if (!poolIds.includes(cardId)) return state;

  if (state.selectedIds.includes(cardId)) {
    return {
      selectedIds: state.selectedIds.filter((id) => id !== cardId),
    };
  }

  if (state.selectedIds.length >= keepCount) return state;

  return { selectedIds: [...state.selectedIds, cardId] };
}

/** True only when exactly `keepCount` cards are selected. */
export function isExchangeSelectionComplete(
  state: ExchangeSelectionState,
  keepCount: number = EXCHANGE_KEEP_COUNT,
): boolean {
  return state.selectedIds.length === keepCount;
}

/**
 * The card ids to send to the backend, in the order the pool is rendered, so
 * the request is deterministic regardless of the order the player clicked.
 */
export function exchangeKeepCardIds(
  state: ExchangeSelectionState,
  poolIds: readonly string[],
): string[] {
  const selected = new Set(state.selectedIds);
  return poolIds.filter((id) => selected.has(id));
}

/** The part of the game state the overlay gate needs. */
export interface ExchangeSelectionGateInput {
  status: MatchStatus;
  activeAction: ActionIntent | null;
  exchangePool?: readonly { id: string }[];
  currentTurnPlayerId: string | null;
}

/**
 * True when the local player must pick 2 cards out of the offered pool.
 *
 * The gate deliberately does NOT test for `MatchStatus.IN_PROGRESS`: a running
 * match reports the backend status `CREATED`, which the client maps to
 * `WAITING`, so an IN_PROGRESS test hid the selection UI for every live match
 * and left the player looking at an inert hand. The pending action, the
 * private pool the server only sends to the acting player, and the turn are
 * the authoritative signals that the choice is waiting.
 */
export function shouldShowExchangeSelection(
  game: ExchangeSelectionGateInput | null | undefined,
  selfId: string,
): boolean {
  if (!game) return false;
  if (game.status === MatchStatus.FINISHED) return false;
  if (game.status === MatchStatus.ABANDONED) return false;
  if (game.activeAction?.action !== "exchange") return false;
  if (!game.exchangePool || game.exchangePool.length === 0) return false;
  return game.currentTurnPlayerId === selfId;
}
