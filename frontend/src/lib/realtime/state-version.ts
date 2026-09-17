/**
 * Module 23 — monotonic version bookkeeping for realtime game snapshots.
 *
 * The backend broadcasts a full {@code STATE_UPDATED} (viewer-neutral) snapshot
 * on the match topic and a full {@code PRIVATE_STATE} (viewer-aware) snapshot
 * to the requesting player for every sync. Both carry the same
 * {@code stateVersion} that increments once per server-side sync.
 *
 * These helpers decide whether an incoming snapshot replaces the locally kept
 * one, and whether the client fell behind and should request a resync.
 */

export interface VersionKey {
  stateVersion: number;
  scope: "public" | "private";
}

export interface GameSnapshotLike extends VersionKey {
  receivedAt: number;
}

/**
 * True when the incoming snapshot must be dropped.
 *
 * <ul>
 *   <li>older version — stale;</li>
 *   <li>same version, both public — duplicate broadcast;</li>
 *   <li>same version, private over public — accepted (private carries cards);</li>
 *   <li>same version, public over private — dropped (private is a superset).</li>
 * </ul>
 */
export function isSuperseded(
  current: VersionKey | undefined,
  incoming: VersionKey,
): boolean {
  if (!current) return false;
  if (incoming.stateVersion < current.stateVersion) return true;
  if (incoming.stateVersion === current.stateVersion) {
    return !(incoming.scope === "private" && current.scope !== "private");
  }
  return false;
}

/**
 * True when the incoming version leaps beyond the next expected revision,
 * meaning the client likely missed a broadcast and should resync. Onboarding
 * a match from cold (no local revision yet) is never treated as a gap.
 */
export function detectVersionGap(
  current: VersionKey | undefined,
  incoming: VersionKey,
): boolean {
  if (!current) return false;
  return incoming.stateVersion > current.stateVersion + 1;
}