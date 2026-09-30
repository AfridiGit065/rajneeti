import { test } from "node:test";
import assert from "node:assert/strict";
import {
  EXCHANGE_KEEP_COUNT,
  createExchangeSelection,
  exchangeKeepCardIds,
  exchangeSelectionCount,
  isExchangeSelectionComplete,
  pruneExchangeSelection,
  toggleExchangeCard,
} from "@/lib/game/exchange-selection";

const POOL = ["card-a", "card-b", "card-c", "card-d"];

function select(
  state: ReturnType<typeof createExchangeSelection>,
  ...ids: string[]
) {
  return ids.reduce((acc, id) => toggleExchangeCard(acc, id, POOL), state);
}

test("1. four cards are offered to the UI", () => {
  assert.equal(POOL.length, 4);
  assert.equal(EXCHANGE_KEEP_COUNT, 2);
});

test("2. clicking an unselected card selects it", () => {
  const state = select(createExchangeSelection(), "card-a");
  assert.deepEqual(state.selectedIds, ["card-a"]);
  assert.equal(exchangeSelectionCount(state), 1);
});

test("3. clicking a selected card deselects it", () => {
  const state = toggleExchangeCard(
    select(createExchangeSelection(), "card-a"),
    "card-a",
    POOL,
  );
  assert.deepEqual(state.selectedIds, []);
});

test("4. at most two cards can be selected", () => {
  const state = select(createExchangeSelection(), "card-a", "card-b");
  const afterThird = select(state, "card-c", "card-d");
  assert.equal(exchangeSelectionCount(afterThird), EXCHANGE_KEEP_COUNT);
});

test("5. confirm is disabled at zero selections", () => {
  assert.equal(
    isExchangeSelectionComplete(createExchangeSelection()),
    false,
  );
});

test("6. confirm is disabled at one selection", () => {
  const state = select(createExchangeSelection(), "card-a");
  assert.equal(isExchangeSelectionComplete(state), false);
});

test("7. confirm is enabled at exactly two selections", () => {
  const state = select(createExchangeSelection(), "card-a", "card-c");
  assert.equal(isExchangeSelectionComplete(state), true);
});

test("8. a third card cannot be selected while two are held", () => {
  const state = select(createExchangeSelection(), "card-a", "card-b");
  const afterThird = toggleExchangeCard(state, "card-c", POOL);

  assert.equal(afterThird, state, "state must be returned untouched");
  assert.deepEqual(afterThird.selectedIds, ["card-a", "card-b"]);
});

test("9. deselecting one card lets another card be selected", () => {
  const held = select(createExchangeSelection(), "card-a", "card-b");
  const freed = toggleExchangeCard(held, "card-a", POOL);
  const reselected = toggleExchangeCard(freed, "card-c", POOL);

  assert.deepEqual(reselected.selectedIds, ["card-b", "card-c"]);
  assert.equal(isExchangeSelectionComplete(reselected), true);
});

test("10. confirm sends exactly the two selected cards, in pool order", () => {
  const state = select(createExchangeSelection(), "card-d", "card-b");
  assert.deepEqual(exchangeKeepCardIds(state, POOL), ["card-b", "card-d"]);
});

test("11. nothing is ever selected automatically or at random", () => {
  const fresh = createExchangeSelection();
  assert.deepEqual(fresh.selectedIds, []);

  const untouched = pruneExchangeSelection(
    select(fresh, "card-a"),
    POOL,
  );
  assert.deepEqual(untouched.selectedIds, ["card-a"]);
});

test("12. the exchange timeout is not owned by the selection state", () => {
  // The selection logic holds no clock: a stale pool (challenge restored the
  // hand, window expired server-side) can only prune ids, never resolve or
  // auto-confirm the action.
  const held = select(createExchangeSelection(), "card-a", "card-b");
  const afterPoolChange = pruneExchangeSelection(held, ["card-c", "card-d"]);

  assert.deepEqual(afterPoolChange.selectedIds, []);
  assert.equal(isExchangeSelectionComplete(afterPoolChange), false);
  assert.deepEqual(exchangeKeepCardIds(afterPoolChange, ["card-c", "card-d"]), []);
});

test("a card id outside the pool is ignored", () => {
  const state = select(createExchangeSelection(), "not-in-pool");
  assert.deepEqual(state.selectedIds, []);
});
