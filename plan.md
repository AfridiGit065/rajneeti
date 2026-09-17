# Module 19 — Block Manager Implementation Plan

## Overview

Implement the Block Manager for Rajneeti, enabling players to block certain actions by claiming a character. A block claim can itself be challenged, reusing the ChallengeManager.

## Blockable Actions

| Action | Blockable By | Character(s) |
|--------|-------------|--------------|
| FOREIGN_AID | Minister | `minister` |
| STEAL | Dalal or Amla | `dalal`, `amla` |
| ASSASSINATE | Goyenda | `goyenda` |

**Not blockable:** INCOME, TAX, EXCHANGE, COUP

## Block Flow

```
ACTION declared (e.g. Foreign Aid)
  ↓
CHALLENGE WINDOW (existing — ChallengeManager, only for claim-based actions)
  ↓
BLOCK WINDOW (new — BlockManager.block())
  ↓
BLOCK CLAIM recorded on PendingAction
  ↓
BLOCK CHALLENGE WINDOW (ChallengeManager detects block, reuses challenge logic)
  ↓
ACTION RESOLUTION (existing resolve* methods, actor calls with blocked parameter)
```

## State Design

### PendingAction — New Fields

```java
// Module 19 — Block Manager
private final UUID blockerUserId;       // set when block claim is submitted
private final String blockedCharacter;  // "minister" / "dalal" / "amla" / "goyenda"
private final UUID blockChallengerUserId; // set when block is challenged (prevents duplicate)
```

When `blockerUserId` is non-null, a block has been claimed. When `blockChallengerUserId` is non-null, the block has been challenged (no second block challenge allowed).

### GameChallenge — New Field

```java
// Module 19 — true when challenging a block claim rather than an action claim
private final boolean blockClaim;
```

### Block Challenge Resolution

**Truthful block** (blocker has the claimed character):
- Challenger loses 1 influence
- Blocker reveals blocked card → returned to deck → replacement drawn
- Block stands (blockerUserId remains on PendingAction)
- `blockChallengerUserId` set → prevents duplicate block challenge
- `lastChallenge.actionContinues = false` (action is blocked)

**Bluff block** (blocker doesn't have the character):
- Blocker loses 1 influence
- Block removed (blockerUserId and blockedCharacter cleared from PendingAction)
- `blockChallengerUserId` set
- `lastChallenge.actionContinues = true` (action proceeds)

### Actor Resolution

After block challenge resolves, the actor calls the existing resolve* endpoints:
- `pendingAction.blockerUserId != null` → block stands → `resolve(blocked=true)`
- `pendingAction.blockerUserId == null` → no block / block removed → `resolve(blocked=false)`

## Files to Create

### 1. `src/main/java/com/rajneeti/game/BlockManager.java`

@Service with @RequiredArgsConstructor. Depends on GameEngine only (no CardManager/TurnManager needed — block just records state).

**`block(matchId, blockerId, claimedCharacter)`** — @Transactional

Validations:
1. Match active (IN_PROGRESS or CREATED)
2. Pending action exists
3. Action is blockable (FOREIGN_AID, STEAL, ASSASSINATE)
4. Claimed character is valid for the action type
5. Blocker is in the match and active
6. Blocker is not the actor
7. No block already submitted (blockerUserId == null)

On success:
- Rebuild PendingAction with blockerUserId and blockedCharacter set
- Log the block event
- Return safe game state

### 2. `src/test/java/com/rajneeti/game/BlockManagerTest.java`

16 test methods (see Test Plan below).

## Files to Modify

### 3. `PendingAction.java` — add 3 fields

```java
private final UUID blockerUserId;
private final String blockedCharacter;
private final UUID blockChallengerUserId;
```

### 4. `GameChallenge.java` — add 1 field

```java
private final boolean blockClaim;
```

### 5. `ChallengeDto.java` — add 1 field

```java
private boolean blockClaim;
```

### 6. `PendingActionDto.java` — add 2 fields

```java
private UUID blockerUserId;
private String blockedCharacter;
```

### 7. `GameStateMapper.java` — map new fields

- `toResponse()`: map blockerUserId and blockedCharacter from PendingAction to PendingActionDto
- `buildChallengeDto()`: map blockClaim from GameChallenge to ChallengeDto

### 8. `ChallengeManager.java` — support block challenges

Major changes to `challenge()` method:

1. After basic validations (match active, pending exists, challenger active), detect block challenge:
   ```java
   boolean isBlockChallenge = pending.getBlockerUserId() != null
       && pending.getBlockChallengerUserId() == null;
   ```

2. If block challenge → call `challengeBlock()` (new private method)
3. If action challenge → existing logic (unchanged)

**New method: `challengeBlock(state, pending, challengerId, loserCardId)`**

Validations:
- Blocker exists and is active
- Challenger is not the blocker
- Challenger has influence
- Block not already challenged (blockChallengerUserId == null)

Resolution:
- Check if blocker has the blocked character (same pattern as action challenge)
- If claimTrue → `resolveTruthfulBlockChallenge()`: challenger loses, blocker reveals card, block stands
- If claimFalse → `resolveBluffBlockChallenge()`: blocker loses, block removed, action continues

Both methods set `blockClaim=true` on the GameChallenge and `blockChallengerUserId` on the updated PendingAction.

The existing `removeCard()`, `restoreExchangeHand()`, `eliminate()`, and validation helpers are reused for block challenges.

### 9. `GameController.java` — add block endpoint

```java
@PostMapping("/{matchId}/block")
public ResponseEntity<ApiResponse<GameStateResponse>> block(
        @PathVariable UUID matchId,
        @RequestBody BlockRequest request,
        @AuthenticationPrincipal UserPrincipal userPrincipal)
```

New DTO: `BlockRequest` with `claimedCharacter` field.

### 10. `GameControllerTest.java` — add block endpoint test

- Test successful block submission
- Test block on non-blockable action (422)

### Frontend Files

### 11. `frontend/src/types/backend.ts`

Add to `BackendPendingAction`:
```typescript
blockerUserId?: string;
blockedCharacter?: string;
```

Add `BackendBlockRequest`:
```typescript
export interface BackendBlockRequest {
  claimedCharacter: string;
}
```

### 12. `frontend/src/types/game.ts`

Add to `ActionIntent` or `GameState`:
```typescript
blockerUserId?: string;
blockedCharacter?: string;
```

Add `ChallengeResolution.blockClaim?: boolean`.

### 13. `frontend/src/repositories/game-repository.ts`

Add method:
```typescript
block(matchId: string, claimedCharacter: string): Promise<Result<GameState>>;
```

### 14. `frontend/src/repositories/rest/rest-game-repository.ts`

- Implement `block()`: POST `/api/matches/${matchId}/block` with `{ claimedCharacter }`
- Update `mapPendingAction()` to map `blockerUserId` and `blockedCharacter`
- Map blockClaim from challenge response

### 15. `frontend/src/repositories/mock/mock-game-repository.ts`

- Implement mock `block()`: set block state on internal game state

### 16. `frontend/src/services/game-service.ts`

Add:
```typescript
async block(matchId: string, claimedCharacter: string): Promise<Result<GameState>> {
    return repositories.game.block(matchId, claimedCharacter);
}
```

### 17. `frontend/src/components/game/game-board.tsx`

Replace mock block triggers (lines 177-203) with:
- Derive block eligibility from `game.activeAction` + `canBlock()` + player role
- Show block button when eligible player sees a blockable action
- On block submit: call `GameService.block(matchId, claimedCharacter)`
- Update `activeBlock` from backend state (`game.pendingAction.blockerUserId`)
- Block challenge uses existing `GameService.challenge()`

## Test Plan (16 Tests)

| # | Test | Description |
|---|------|-------------|
| 1 | `foreignAid_canBeBlocked_byMinister` | Perform FA, block with minister → block recorded |
| 2 | `steal_canBeBlocked_byDalal` | Perform Steal, block with dalal → block recorded |
| 3 | `steal_canBeBlocked_byAmla` | Perform Steal, block with amla → block recorded |
| 4 | `assassinate_canBeBlocked_byGoyenda` | Perform Assassinate, block with goyenda → block recorded |
| 5 | `income_cannotBeBlocked` | Perform Income, attempt block → ACTION_NOT_BLOCKABLE |
| 6 | `tax_cannotBeBlocked` | Perform Tax, attempt block → ACTION_NOT_BLOCKABLE |
| 7 | `exchange_cannotBeBlocked` | Perform Exchange, attempt block → ACTION_NOT_BLOCKABLE |
| 8 | `coup_noPendingAction` | No pending action → NO_PENDING_ACTION |
| 9 | `bluffBlock_isAllowed` | Block with character not held → still accepted (no ownership check) |
| 10 | `blockClaim_canBeChallenged_bluff` | Block with bluff, challenge succeeds → block removed, blocker loses influence |
| 11 | `invalidBlockingCharacter_rejected` | Block FA with GOYENDA → INVALID_BLOCKING_CHARACTER |
| 12 | `eliminatedPlayer_cannotBlock` | Eliminated player attempts block → PLAYER_ELIMINATED |
| 13 | `invalidBlocker_notInMatch` | Non-match player attempts block → PLAYER_NOT_IN_MATCH |
| 14 | `duplicateBlock_rejected` | Two blocks on same action → DUPLICATE_BLOCK |
| 15 | `blockClaim_canBeChallenged_truthful` | Block with truth, challenge fails → block stands, challenger loses |
| 16 | `fullFlow_actionThenBlock` | Steal → block → resolve blocked → verify action cancelled |

## Limitations (Document in PR)

1. **No polling/WebSocket**: Block state only visible to players on next state refresh; no real-time push.
2. **One block per action**: A second block attempt is rejected (DUPLICATE_BLOCK).
3. **One block challenge per block**: A second block challenge is rejected.
4. **Challenge window then block window**: For claim-based actions (Tax, Steal, Exchange, Assassinate), the challenge window must close before the block window opens. The actor must resolve after challenge resolution before a block can be submitted. (This matches the spec flow.)
5. **ActionResolver NOT implemented**: Block Manager records the block and handles challenges. Final resolution (coin transfer, card elimination) is still handled by the existing resolve* endpoints.
6. **WinnerManager NOT implemented**: No win condition checking.
7. **Block claim challenge visibility**: Only the challenger sees the verdict immediately; other players see it on next refetch.
