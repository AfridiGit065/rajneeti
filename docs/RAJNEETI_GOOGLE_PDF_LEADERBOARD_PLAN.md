# RAJNEETI — Google Login + Match History PDF + Elimination-Order Leaderboard

> Verified implementation plan (anchored to the current `com.rajneeti` codebase at `D:\rajneeti`).
> Each slice below is byte-anchored; every edit is followed by `mvn -q -o compile` (backend) / `npm run typecheck && lint && build` (frontend) before the next slice.

## Reference facts (already verified)

| Seam | File | Event |
|---|---|---|
| Match persistence | `entity\MatchPlayer.java` | `elimination_order` column at lines 102–103; `finalRank` at 105–111; `eliminatedAt`/`coinsAtEnd` verified |
| Winner finish | `service\impl\WinnerManager.java` | `finishGame(winner)` — per-player snapshot (`finalRank`, `coinsAtEnd`), `match.setWinner`, `MatchStatus.FINISHED`, `MatchHistory.saveAll` |
| Elimination order | `WinnerManager.syncPlayerStates` | `setFinalRank((int) activeAfter + 1)` (winner = 1, first-eliminated = last) |
| Statistics init | `AuthServiceImpl.register` / `BotUserService` | zeroed row; **never updated post-match** (no seam) |
| Leaderboard init | same two | zeroed row; **never updated post-match** |
| Auth | `service\impl\AuthServiceImpl` | `register`, `login`, `refreshToken`, `logout`, `getCurrentUser` |
| Security | `security\SecurityConfig` | `PUBLIC_URLS` (`/api/auth/{register,login,refresh}` + `/api/v1/auth/*` + `/ws/**` etc.) — Google endpoint **not yet** in list |
| PDF | `pom.xml` | **No PDF library** (OpenPDF/iText absent), no Flyway (migration = `JPA_DDL_AUTO`) |

## Feature 1 — Google Login

- Backend: `AuthServiceImpl.googleLogin(GoogleLoginRequest)` — verify Google **ID token server-side** (do **not** trust client-provided profile data); find-or-create `User` by **verified email**; link account by email when a same-email row exists; issue the **same JWT** as normal login.
- `User.java`: add `googleId`, `googleEmail`, `provider` (enum ProviderType), `displayName`, `providerAvatarUrl` (nullable).
- `SecurityConfig.PUBLIC_URLS`: add `/api/auth/google` + `/api/v1/auth/google`.
- Frontend: "Continue with Google" button on login page; on success reuse `auth-store` token persistence (same JWT storage — no new auth path).
- Error states: cancelled login / invalid/expired token / account-create failure / backend-auth failure / network — clean user messages, **no internal detail leak**.
- Migration: `JPA_DDL_AUTO` (validate) — new columns via existing ddl strategy; `ddl-auto` update in dev docker.

## Feature 2 — Match History PDF

- Dependency: add **OpenPDF** (`com.github.librepdf:openpdf`) to `pom.xml`.
- Endpoint: `GET /api/matches/{matchId}/history/pdf` — Spring Security `authenticated()`; **participation check** (must be a `MatchPlayer` in that match); returns `application/pdf` blob.
- Authorization: `@AuthenticationPrincipal UserPrincipal`; verify `matchPlayerRepository.findByMatchIdAndUserId`.
- Content (Rajneeti dark-emerald/gold theme, **no hidden cards** — only public info): Match ID, date, duration, number of players, winner, final coins+influence, **FINAL STANDINGS (placement order)**, **ELIMINATION ORDER**, **MATCH SUMMARY** (from `GameLogEntry`/`MatchHistory`).
- Never include: hidden/private cards, tokens, private websocket data.
- Frontend: PDF download button in match-history card + profile match history + game-over standings.

## Feature 3 — Elimination-Order Leaderboard (placement-smart)

- Backend already stores `finalRank` (placement: 1=winner … N=first-eliminated) + `eliminationOrder` + `MatchHistory.finalRank`.
- **Gap**: `Statistics`/`Leaderboard` rows are never updated post-match.
- Add: in `WinnerManager.finishGame`, after the per-player snapshot — placement-aware increment:
  - `Statistics.totalMatches += 1`; if `finalRank == 1` → `wins += 1` else `losses += 1`; `totalCoinsEarned/Spent` update; recompute `winRate`.
  - `Leaderboard.rating/rank` placement-adapted (2nd/3rd place ≠ plain loss).
- UI: Game Over screen shows final standings **ranked by placement**; match history + profile show placement badge; game log lists eliminations with their order ("Player F lost final influence — eliminated #1").

## Order of implementation (user-chosen)

1. **Google Login** — backend verify + User columns + endpoint + frontend button.
2. **Match History PDF** — OpenPDF dep + authz endpoint + themed PDF + frontend download.
3. **Leaderboard placement-wise** — Statistics/Leaderboard post-match update + game-over/profile/history placement UI.

## Gate

- Backend: `mvn -q -o compile` (offline-ish, no network dye PDF **antobihin offline dep `openpdf` jar repository-e thakbe kinna verify** — kichu project `local-repo` diye `-o` compile hoy; OpenPDF first run-e `mvn dependency:resolve` lagbe).
- Frontend: `npm run typecheck && npm run lint && npm run build`.
</content>
