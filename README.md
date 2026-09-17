# রাজনীতি (Rajneeti)

> **A Real-Time Multiplayer Bluff Strategy Game**  
> *“ক্ষমতার খেলায় সত্য নয়, বুদ্ধিই শেষ কথা।”*

---

## 1. Project Overview

**রাজনীতি (Rajneeti)** is a real-time multiplayer bluff and deduction strategy card game inspired by classic hidden-role games (such as *Coup*), reimagined in a dramatic political theme. 

In Rajneeti, 2 to 6 players compete for absolute political dominance. Each player starts with 2 face-down influence cards representing their secret political allies and 2 coins. Through bluffs, challenges, counter-claims, and strategic maneuvers, players attempt to strip rival politicians of their influence. When a player loses all their influence, they are eliminated. The last politician standing claims ultimate power.

> **Disclaimer:** This game is entirely fictional and does not represent any real political party, politician, government institution, or real-world political event.

---

## 2. Features

- **Authoritative Game Engine**: Strict server-enforced state machine managing turns, action declaration, challenge windows, block windows, and resolution.
- **Hidden Card Confidentiality**: Cryptographic privacy at the data layer. Opponent cards are never serialized or sent over WebSocket / REST payloads.
- **Full Bluff & Counter-Claim Mechanic**: Players can claim any character power regardless of actual hand holdings. Opponents can challenge claims or stage counter-blocks.
- **Real-Time STOMP over SockJS**: Dual-stream WebSocket architecture combining public neutral board state (`/topic/matches/{matchId}`) and personalized private queues (`/user/queue/game`).
- **Resilient Realtime Synchronization**: Strict monotonic `stateVersion` tracking, duplicate suppression, out-of-order drop, and automatic forward-gap detection with idempotent resync requests (`/app/matches/{matchId}/sync`).
- **AI Bot Player System**: Intelligent bot players powered by external LLM chat-completions APIs with a 3-key rotation manager, cooldowns, and automatic failover to a deterministic heuristic strategy.
- **Full Social & Meta System**: User registration, JWT authentication with refresh tokens, player profiles, win/loss stats, match history, and global ELO leaderboard.
- **Production-Ready Containerization**: Multi-stage Dockerfiles for frontend and backend with non-root security, plus orchestrating `docker-compose.yml`.

---

## 3. Game Rules

- **Players**: 2 to 6 players per match.
- **Starting Conditions**: Each player begins with 2 face-down Influence Cards and 2 Coins.
- **Deck**: Exactly 15 cards (5 unique characters × 3 copies each).
- **Turns**: Deterministic clockwise turn order. If an active player holds **10 or more coins** at the start of their turn, they **must** perform a Coup.
- **Elimination**: When a player loses their last influence card, their status changes to `ELIMINATED` and they are skipped on subsequent turns.
- **Victory Condition**: When exactly one active player remains, the game immediately ends and the winner is declared.

### Action Flow Diagram

```
Actor Declares Action
        │
        ▼
   Claim-Based?
   ├── YES ──► Challenge Window Opens (Opponents may challenge character claim)
   │               ├── Challenge Succeeds: Actor loses influence, Action cancelled
   │               └── Challenge Fails: Challenger loses influence, Action proceeds
   └── NO (Income, Foreign Aid, Coup)
        │
        ▼
   Blockable?
   ├── YES ──► Block Window Opens (Opponents may claim blocking character)
   │               └── Opponent Blocks ──► Block Challenge Window Opens
   │                       ├── Block Challenge Succeeds: Blocker loses influence, Action proceeds
   │                       └── Block Challenge Fails: Challenger loses influence, Action blocked
   └── NO
        │
        ▼
   Action Resolves & Turn Advances
```

---

## 4. Characters & Abilities

| Character | Action | Action Benefit | Block Capability | Challengeable? |
| :--- | :--- | :--- | :--- | :--- |
| **Minister** (মন্ত্রী) | **Tax** | Collects +3 coins from the treasury. | **Blocks Foreign Aid** | Yes |
| **Ghatok** (ঘাতক) | **Assassinate** | Pay 3 coins; target player loses 1 influence card. | *None* | Yes |
| **Dalal** (দালাল) | **Steal** | Steals up to 2 coins from a targeted rival. | **Blocks Steal** | Yes |
| **Amla** (আমলা) | **Exchange** | Draws 2 cards from deck, chooses 2 to keep, returns 2. | **Blocks Steal** | Yes |
| **Goyenda** (গোয়েন্দা) | *None* | No active action. | **Blocks Assassination** | Yes |

### General Actions (No Character Claim Required)

- **Income**: Take +1 coin from the treasury. Cannot be blocked. Cannot be challenged.
- **Foreign Aid**: Take +2 coins from the treasury. Blockable by Minister. Cannot be challenged.
- **Coup**: Pay 7 coins; target player immediately loses 1 influence card. Cannot be blocked. Cannot be challenged.

---

## 5. Tech Stack

### Backend
- **Java 21** (Eclipse Adoptium LTS)
- **Spring Boot 3.3.x**
- **Spring Security 6 & JJWT 0.12** (Stateless authentication, BCrypt hashing)
- **Spring Data JPA & Hibernate 6**
- **Spring WebSocket & STOMP Message Broker**
- **MySQL 8.x** (Production) & **H2 In-Memory** (Unit/Integration Test profile)
- **Maven 3.9+**

### Frontend
- **Next.js 16 (App Router, Turbopack, Standalone Output)**
- **React 19 & TypeScript 5**
- **Zustand 5** (State Management)
- **Tailwind CSS v4**
- **@stomp/stompjs & sockjs-client**
- **Lucide React** (Icons)

### Infrastructure & DevOps
- **Docker & Docker Compose**
- **Multi-Stage Builds** with unprivileged runtime users (`rajneeti` / `nextjs`)

---

## 6. Architecture

```
                       Browser Clients (2-6 Players)
                                    │
                         REST API   │   WebSocket / STOMP
                                    ▼
                     ┌──────────────────────────────┐
                     │     Spring Boot Gateway      │
                     │  - Security & JWT Filter     │
                     │  - WebSocket Interceptors    │
                     └──────────────┬───────────────┘
                                    │
                 ┌──────────────────┼──────────────────┐
                 ▼                  ▼                  ▼
        ┌─────────────────┐ ┌──────────────┐ ┌─────────────────┐
        │  Meta Services  │ │  Game Engine │ │   Bot Service   │
        │ - Auth / Profile│ │ - Turn Mgr   │ │ - Decision Ctx  │
        │ - Rooms / Match │ │ - Card Mgr   │ │ - AI Key Rotator│
        │ - Leaderboards  │ │ - Action Res │ │ - Heuristics    │
        └────────┬────────┘ └───────┬──────┘ └────────┬────────┘
                 │                  │                 │
                 ▼                  ▼                 ▼
          MySQL Database       GameStore        External LLM
         (JPA Repositories)   (In-Memory)      (3-Key Failover)
```

---

## 7. Project Structure

```
rajneeti/
├── Dockerfile                  # Production multi-stage Dockerfile for backend
├── docker-compose.yml          # Orchestration: MySQL + Backend + Frontend
├── pom.xml                     # Maven build configuration
├── src/
│   ├── main/
│   │   ├── java/com/rajneeti/
│   │   │   ├── bot/            # AI bot decision engine, key rotator, fallbacks
│   │   │   ├── config/         # Security, WebSocket, CORS configuration
│   │   │   ├── controller/     # REST and WebSocket STOMP controllers
│   │   │   ├── dto/            # Request/Response data transfer objects
│   │   │   ├── entity/         # JPA entities (User, Room, Match, Stats)
│   │   │   ├── game/           # GameEngine, CardManager, TurnManager, GameStore
│   │   │   ├── repository/     # Spring Data JPA repositories
│   │   │   ├── security/       # JWT token provider and authentication filters
│   │   │   └── service/        # Domain business logic implementations
│   │   └── resources/
│   │       ├── application.yml         # Base configuration
│   │       ├── application-dev.yml     # Development profile
│   │       ├── application-prod.yml    # Production profile
│   │       └── db/schema.sql           # Initial database schema
│   └── test/                           # 420+ automated unit & integration tests
└── frontend/
    ├── Dockerfile              # Next.js standalone containerization
    ├── package.json            # Frontend dependencies and scripts
    ├── next.config.ts          # Next.js configuration (standalone output)
    └── src/
        ├── app/                # App router pages (lobby, rooms, game, stats)
        ├── components/         # Game board, cards, modals, interactive UI
        ├── lib/                # STOMP client, API client, utilities
        ├── repositories/       # REST repositories
        ├── store/              # Zustand global client stores
        └── types/              # TypeScript definitions
```

---

## 8. Backend Setup

### Prerequisites
- JDK 21 (Eclipse Adoptium Recommended)
- Apache Maven 3.9+
- MySQL 8.x (or run using `-Ph2` in-memory profile)

### Configuration
Copy `.env.example` to `.env` in the root directory:
```bash
cp .env.example .env
```

Ensure `JWT_SECRET` is generated:
```bash
openssl rand -base64 48
```

---

## 9. Frontend Setup

### Prerequisites
- Node.js 20+ (Node 22 LTS or 24 recommended)
- npm 10+

### Configuration
Copy `frontend/.env.local.example` to `frontend/.env.local`:
```bash
cp frontend/.env.local.example frontend/.env.local
```

Install dependencies:
```bash
cd frontend
npm install
```

---

## 10. Database Setup

Using MySQL CLI or phpMyAdmin / MySQL Workbench:
```sql
CREATE DATABASE rajneeti_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'rajneeti_user'@'%' IDENTIFIED BY 'changeme';
GRANT ALL PRIVILEGES ON rajneeti_db.* TO 'rajneeti_user'@'%';
FLUSH PRIVILEGES;
```

When running in development, Hibernate auto-updates tables via `spring.jpa.hibernate.ddl-auto: update`.

---

## 11. Environment Variables Reference

| Variable | Default | Description |
| :--- | :--- | :--- |
| `SERVER_PORT` | `8080` | Backend HTTP listening port |
| `DB_HOST` | `localhost` | MySQL host address |
| `DB_PORT` | `3306` | MySQL port |
| `DB_NAME` | `rajneeti_db` | MySQL database name |
| `DB_USERNAME` | `rajneeti_user`| Database username |
| `DB_PASSWORD` | `changeme` | Database password |
| `JWT_SECRET` | *(required)* | Base64 HMAC-SHA256 signing secret (≥32 bytes) |
| `JWT_EXPIRATION_MS` | `86400000` | Access token lifespan (24h) |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` | Refresh token lifespan (7 days) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Allowed CORS origins (comma-separated) |
| `WS_ALLOWED_ORIGINS` | `http://localhost:3000` | Allowed WebSocket origins |
| `AI_PROVIDER` | `openai` | LLM provider (`openai`) |
| `AI_BASE_URL` | `https://api.openai.com/v1` | LLM endpoint URL |
| `AI_MODEL` | `gpt-4o-mini` | Chat completions model |
| `AI_API_KEY_1` | *optional* | Primary AI API key |
| `AI_API_KEY_2` | *optional* | Secondary AI API key (rotation/failover) |
| `AI_API_KEY_3` | *optional* | Tertiary AI API key (rotation/failover) |
| `AI_USE_KEY_ROTATION` | `true` | Enables round-robin key selection |
| `BOT_USE_LLM` | `true` | Toggle external LLM vs deterministic heuristic |
| `NEXT_PUBLIC_API_URL` | `http://localhost:8080` | Frontend backend API target |
| `NEXT_PUBLIC_WS_URL` | `ws://localhost:8080/ws` | Frontend WebSocket endpoint |

---

## 12. Running Locally

### Starting Backend:
```bash
# Option 1: Standard with MySQL
mvn spring-boot:run

# Option 2: In-Memory H2 (No MySQL installation required)
mvn spring-boot:run -Ph2
```

### Starting Frontend:
```bash
cd frontend
npm run dev
```

Access the application in your browser at `http://localhost:3000`.

---

## 13. WebSocket Architecture

Rajneeti utilizes STOMP messaging over SockJS:
- **Broker Endpoint**: `/ws` (with SockJS handshake)
- **Application Inbound**: `/app/**`
- **Public Topic Broadcast**: `/topic/matches/{matchId}`
- **Private User Queues**: `/user/queue/game`

### Subscription Access Control
The `WebSocketAuthInterceptor` validates JWTs upon connection and enforces strict subscription authorization:
- A user cannot subscribe to a match topic unless they are an active player in that match.
- A user cannot subscribe to another player's private `/user/` destination.

---

## 14. Realtime Synchronization & stateVersion

Every state mutation bumps `stateVersion` monotonically:
1. `STATE_UPDATED` snapshot sent to `/topic/matches/{matchId}` (neutral viewer, all cards hidden).
2. `PRIVATE_STATE` snapshot sent to `/user/{userId}/queue/game` for each alive player (own cards visible).
3. Clients track local `lastVersion`:
   - If incoming `version < lastVersion`: Dropped as stale.
   - If incoming `version == lastVersion`: Duplicate dropped.
   - If incoming `version == lastVersion + 1`: Applied immediately.
   - If incoming `version > lastVersion + 1`: Gap detected; client emits `/app/matches/{matchId}/sync` with idempotency `requestId`.

---

## 15. AI Bot System & Three-Key Rotation

Rajneeti supports full AI bot players (Human vs Bot, Human vs Multiple Bots, Bot vs Bot).
- Bots participate in all game phases: Income, Tax, Steal, Exchange, Assassinate, Coup, Challenge, and Counter-Block.
- The `AiKeyManager` rotates across up to 3 API keys.
- **Failover**: If Key 1 triggers rate limiting (429) or transient 5xx errors, it is placed in cooldown and Key 2 is tried immediately.
- **Permanent Invalidation**: If a key receives a 401/403 authorization error, it is permanently disabled.
- **Heuristic Fallback**: If all keys fail or no keys are configured, `BotFallbackStrategy` takes over instantly. The game **never** stalls or crashes due to external API failures.

---

## 16. Testing Suite

The repository contains 423 automated tests with 100% pass rate.

### Executing Backend Tests:
```bash
mvn clean test
```

### Executing Frontend Verification:
```bash
cd frontend
npm test            # State-version unit tests
npm run typecheck   # TypeScript validation
npm run lint        # ESLint checking
npm run build       # Next.js production build validation
```

---

## 17. Docker & Containerization

### Production Build via Docker Compose:
```bash
# Validates configuration
docker compose config

# Builds images and launches containers
docker compose up --build -d
```

Containers provisioned:
1. `rajneeti-mysql`: MySQL 8.2 with persistent volumes and healthchecks.
2. `rajneeti-app`: Spring Boot fat JAR running under Alpine JRE as unprivileged user `rajneeti`.
3. `rajneeti-frontend`: Next.js standalone container running under Node 22 Alpine as unprivileged user `nextjs`.

---

## 18. Security Highlights

- **No Plaintext Passwords**: Passwords hashed with BCrypt (strength 12).
- **Stateless Authentication**: JJWT HMAC-SHA256 tokens validated on every REST and WebSocket request.
- **Zero Card Leakage**: Opponents' cards are set to `null` before DTO construction and omitted from JSON output by Jackson serialization.
- **Idempotency Guards**: `DuplicateRequestGuard` prevents double-submissions and concurrent replay attacks.
- **Zero Committed Secrets**: `.env` is gitignored; startup validates secret presence and length.

---

## 19. Contributors & License

- **Author**: Rajneeti Development Team
- **Project**: Module 01–26 Complete Implementation
- **License**: Educational & Showcase Use Only
