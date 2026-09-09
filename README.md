# RAJNEETI – রাজনীতি

> **Real-Time Multiplayer Bluff Strategy Game** | Spring Boot Backend Foundation 

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Package Responsibilities](#package-responsibilities)
- [Quick Start](#quick-start)
  - [Prerequisites](#prerequisites)
  - [1. Configure MySQL](#1-configure-mysql)
  - [2. Set Environment Variables](#2-set-environment-variables)
  - [3. Run the Backend](#3-run-the-backend)
  - [4. Run with Docker Compose](#4-run-with-docker-compose)
- [Test the Health Endpoint](#test-the-health-endpoint)
- [API Response Structure](#api-response-structure)
- [Environment Variables Reference](#environment-variables-reference)
- [Module Roadmap](#module-roadmap)

---

## Overview

RAJNEETI is a fictional real-time multiplayer bluff strategy game built as a final-year project.
This repository contains the **Module 01 backend foundation** – all infrastructure, configuration,
security scaffolding, and API contracts are in place. No gameplay logic has been implemented yet.

> **Disclaimer:** This game is entirely fictional and does not represent any real political party,
> politician, government institution, or real-world political event.

---

## Tech Stack

| Layer            | Technology                          |
|------------------|-------------------------------------|
| Language         | Java 21                             |
| Framework        | Spring Boot 3.3.x                   |
| Build            | Maven 3.9+                          |
| Database         | MySQL 8.x                           |
| ORM              | Spring Data JPA + Hibernate         |
| Security         | Spring Security 6 + JWT (JJWT 0.12) |
| Real-Time        | WebSocket + STOMP (SockJS)          |
| Object Mapping   | MapStruct 1.6                       |
| Boilerplate      | Lombok                              |
| Validation       | Jakarta Bean Validation             |
| Containerisation | Docker + Docker Compose             |

---

## Project Structure

```
rajneeti/
├── src/
│   ├── main/
│   │   ├── java/com/rajneeti/
│   │   │   ├── RajneetiApplication.java     ← Entry point
│   │   │   ├── controller/                  ← REST controllers
│   │   │   ├── service/                     ← Business logic
│   │   │   ├── repository/                  ← Spring Data JPA repos
│   │   │   ├── entity/                      ← JPA entities
│   │   │   ├── dto/                         ← Request/response DTOs
│   │   │   ├── mapper/                      ← MapStruct mappers
│   │   │   ├── security/                    ← JWT filter, config, entry point
│   │   │   ├── config/                      ← CORS, WebSocket, properties
│   │   │   ├── exception/                   ← Custom exceptions + global handler
│   │   │   ├── game/                        ← (Module 02+) Game engine
│   │   │   ├── websocket/                   ← WebSocket event listeners
│   │   │   └── util/                        ← Shared utilities / constants
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/com/rajneeti/
│           └── RajneetiApplicationTests.java
├── .env.example
├── .gitignore
├── .dockerignore
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

---

## Package Responsibilities

| Package       | Responsibility                                                                 |
|---------------|--------------------------------------------------------------------------------|
| `controller`  | Handles HTTP requests; thin layer that delegates to `service`                  |
| `service`     | Business logic; orchestrates `repository` and domain operations                |
| `repository`  | Spring Data JPA interfaces for database access                                 |
| `entity`      | JPA-managed database entities                                                  |
| `dto`         | Data Transfer Objects for request/response payloads (no entities exposed)      |
| `mapper`      | MapStruct interfaces that convert between `entity` ↔ `dto`                    |
| `security`    | JWT token provider, authentication filter, entry point, Spring Security config |
| `config`      | CORS, WebSocket, and typed `@ConfigurationProperties` classes                  |
| `exception`   | Custom exception classes + `@RestControllerAdvice` global handler              |
| `game`        | Game engine, room management, card logic (Module 02+)                          |
| `websocket`   | WebSocket lifecycle listeners and STOMP message controllers                    |
| `util`        | Stateless utility classes (constants, date helpers)                            |

---

## Quick Start

### Prerequisites

- Java 21 (e.g., Eclipse Temurin, Amazon Corretto)
- Maven 3.9+
- MySQL 8.x **or** Docker Desktop

---

### 1. Configure MySQL

```sql
-- Run once as root
CREATE DATABASE rajneeti_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'rajneeti_user'@'%' IDENTIFIED BY 'your_strong_password';
GRANT ALL PRIVILEGES ON rajneeti_db.* TO 'rajneeti_user'@'%';
FLUSH PRIVILEGES;
```

---

### 2. Set Environment Variables

```bash
# Copy the example and fill in real values
cp .env.example .env
```

Edit `.env`:

```dotenv
DB_HOST=localhost
DB_PORT=3306
DB_NAME=rajneeti_db
DB_USERNAME=rajneeti_user
DB_PASSWORD=your_strong_password
JWT_SECRET=<base64-encoded-256-bit-random-secret>
JPA_DDL_AUTO=update
```

Generate a JWT secret:

```bash
openssl rand -base64 32
```

---

### 3. Run the Backend (Local – Maven)

**Linux / macOS:**

```bash
export $(grep -v '^#' .env | xargs)
mvn spring-boot:run
```

**Windows (PowerShell):**

```powershell
Get-Content .env | ForEach-Object {
  if ($_ -match '^([^#][^=]+)=(.+)$') {
    [System.Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
  }
}
mvn spring-boot:run
```

Or use your IDE (IntelliJ IDEA / VS Code) with the `.env` plugin to load variables automatically.

---

### 4. Run with Docker Compose

```bash
# Build and start MySQL + App together
docker compose up --build

# Background mode
docker compose up --build -d

# Stop
docker compose down

# Wipe volumes (reset DB)
docker compose down -v
```

---

## Test the Health Endpoint

```bash
curl -s http://localhost:8080/api/health | python -m json.tool
```

**Expected response:**

```json
{
  "success": true,
  "message": "Operation successful",
  "data": {
    "status": "UP",
    "application": "RAJNEETI",
    "timestamp": "2024-01-01T00:00:00Z"
  },
  "timestamp": "2024-01-01T00:00:00Z"
}
```

---

## API Response Structure

**Success:**

```json
{
  "success": true,
  "message": "Operation successful",
  "data": { },
  "timestamp": "2024-01-01T00:00:00Z"
}
```

**Validation Error (400):**

```json
{
  "status": 400,
  "error": "VALIDATION_FAILED",
  "message": "Request validation failed.",
  "path": "/api/v1/auth/register",
  "timestamp": "2024-01-01T00:00:00Z",
  "errors": [
    { "field": "email", "message": "must not be blank" }
  ]
}
```

---

## Environment Variables Reference

| Variable                  | Default                  | Description                                |
|---------------------------|--------------------------|--------------------------------------------|
| `SERVER_PORT`             | `8080`                   | HTTP port                                  |
| `SPRING_PROFILES_ACTIVE`  | `dev`                    | Active Spring profile                      |
| `DB_HOST`                 | `localhost`              | MySQL host                                 |
| `DB_PORT`                 | `3306`                   | MySQL port                                 |
| `DB_NAME`                 | `rajneeti_db`            | MySQL database name                        |
| `DB_USERNAME`             | `rajneeti_user`          | MySQL username                             |
| `DB_PASSWORD`             | *(required)*             | MySQL password                             |
| `JPA_DDL_AUTO`            | `validate`               | Hibernate DDL mode (`update` for dev)      |
| `JPA_SHOW_SQL`            | `false`                  | Log SQL statements                         |
| `JWT_SECRET`              | *(required)*             | Base64 HMAC-SHA256 key ≥32 bytes           |
| `JWT_EXPIRATION_MS`       | `86400000` (24 h)        | Access token TTL                           |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` (7 d)     | Refresh token TTL                          |
| `CORS_ALLOWED_ORIGINS`    | `http://localhost:3000`  | Comma-separated front-end origins          |
| `WS_ALLOWED_ORIGINS`      | `http://localhost:3000`  | WebSocket handshake allowed origins        |
| `LOG_LEVEL_APP`           | `DEBUG`                  | Log level for `com.rajneeti`               |

---

## Module Roadmap

| Module | Feature                                          | Status         |
|--------|--------------------------------------------------|----------------|
| 01     | Backend foundation (this module)                 | ✅ Complete     |
| 02     | User entity, registration, JWT auth endpoints    | 🔜 Next         |
| 03     | Game room creation, lobby management             | 📋 Planned      |
| 04     | WebSocket game events, STOMP messaging           | 📋 Planned      |
| 05     | Character abilities, card logic, game engine     | 📋 Planned      |
| 06     | Leaderboard, statistics, player profiles         | 📋 Planned      |
| 07     | Frontend (React / Next.js)                       | 📋 Planned      |
