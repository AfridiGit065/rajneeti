-- =============================================================================
-- RAJNEETI (রাজনীতি) - Database Schema
-- Target: MySQL 8.x / InnoDB / utf8mb4
-- Database: rajneeti_db
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `rajneeti_db`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `rajneeti_db`;

-- -----------------------------------------------------------------------------
-- 1. Users Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `users` (
    `id`            VARCHAR(36)     NOT NULL,
    `username`      VARCHAR(50)     NOT NULL,
    `email`         VARCHAR(100)    NOT NULL,
    `password`      VARCHAR(255)    NOT NULL,
    `avatar_url`    VARCHAR(255)    NULL,
    `rating`        INT             NOT NULL DEFAULT 1000,
    `total_matches` INT             NOT NULL DEFAULT 0,
    `wins`          INT             NOT NULL DEFAULT 0,
    `losses`        INT             NOT NULL DEFAULT 0,
    `created_at`    DATETIME(6)     NOT NULL,
    `updated_at`    DATETIME(6)     NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_users_username` UNIQUE (`username`),
    CONSTRAINT `uk_users_email` UNIQUE (`email`),
    INDEX `idx_users_username` (`username`),
    INDEX `idx_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 2. Rooms Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `rooms` (
    `id`            VARCHAR(36)     NOT NULL,
    `room_code`     VARCHAR(10)     NOT NULL,
    `host_id`       VARCHAR(36)     NOT NULL,
    `status`        VARCHAR(20)     NOT NULL DEFAULT 'WAITING',
    `max_players`   INT             NOT NULL DEFAULT 6,
    `created_at`    DATETIME(6)     NOT NULL,
    `updated_at`    DATETIME(6)     NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_rooms_room_code` UNIQUE (`room_code`),
    CONSTRAINT `fk_rooms_host` FOREIGN KEY (`host_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    INDEX `idx_rooms_room_code` (`room_code`),
    INDEX `idx_rooms_status` (`status`),
    INDEX `idx_rooms_host_id` (`host_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 3. Room Players Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `room_players` (
    `id`            VARCHAR(36)     NOT NULL,
    `room_id`       VARCHAR(36)     NOT NULL,
    `user_id`       VARCHAR(36)     NOT NULL,
    `seat_number`   INT             NOT NULL,
    `ready`         BOOLEAN         NOT NULL DEFAULT FALSE,
    `joined_at`     DATETIME(6)     NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_room_players_room` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_room_players_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `uk_room_players_room_user` UNIQUE (`room_id`, `user_id`),
    CONSTRAINT `uk_room_players_room_seat` UNIQUE (`room_id`, `seat_number`),
    INDEX `idx_room_players_room_id` (`room_id`),
    INDEX `idx_room_players_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 4. Matches Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `matches` (
    `id`            VARCHAR(36)     NOT NULL,
    `room_id`       VARCHAR(36)     NULL,
    `status`        VARCHAR(20)     NOT NULL DEFAULT 'CREATED',
    `winner_id`     VARCHAR(36)     NULL,
    `started_at`    DATETIME(6)     NULL,
    `ended_at`      DATETIME(6)     NULL,
    `created_at`    DATETIME(6)     NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_matches_room` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_matches_winner` FOREIGN KEY (`winner_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    INDEX `idx_matches_status` (`status`),
    INDEX `idx_matches_room_id` (`room_id`),
    INDEX `idx_matches_winner_id` (`winner_id`),
    INDEX `idx_matches_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 5. Match Players Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `match_players` (
    `id`            VARCHAR(36)     NOT NULL,
    `match_id`      VARCHAR(36)     NOT NULL,
    `user_id`       VARCHAR(36)     NOT NULL,
    `seat_number`   INT             NOT NULL,
    `final_rank`    INT             NULL,
    `coins_at_end`  INT             NOT NULL DEFAULT 0,
    `eliminated`    BOOLEAN         NOT NULL DEFAULT FALSE,
    `eliminated_at` DATETIME(6)     NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_match_players_match` FOREIGN KEY (`match_id`) REFERENCES `matches` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_match_players_user`  FOREIGN KEY (`user_id`)  REFERENCES `users`   (`id`) ON DELETE CASCADE,
    CONSTRAINT `uk_match_players_match_user` UNIQUE (`match_id`, `user_id`),
    CONSTRAINT `uk_match_players_match_seat` UNIQUE (`match_id`, `seat_number`),
    INDEX `idx_match_players_match_id` (`match_id`),
    INDEX `idx_match_players_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 6. Leaderboard Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `leaderboard` (
    `id`            VARCHAR(36)     NOT NULL,
    `user_id`       VARCHAR(36)     NOT NULL,
    `rating`        INT             NOT NULL DEFAULT 1000,
    `wins`          INT             NOT NULL DEFAULT 0,
    `losses`        INT             NOT NULL DEFAULT 0,
    `total_matches` INT             NOT NULL DEFAULT 0,
    `rank`          INT             NULL,
    `updated_at`    DATETIME(6)     NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_leaderboard_user` UNIQUE (`user_id`),
    CONSTRAINT `fk_leaderboard_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    INDEX `idx_leaderboard_rating` (`rating`),
    INDEX `idx_leaderboard_rank` (`rank`),
    INDEX `idx_leaderboard_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 7. Statistics Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `statistics` (
    `id`                 VARCHAR(36)     NOT NULL,
    `user_id`            VARCHAR(36)     NOT NULL,
    `total_matches`      INT             NOT NULL DEFAULT 0,
    `wins`               INT             NOT NULL DEFAULT 0,
    `losses`             INT             NOT NULL DEFAULT 0,
    `win_rate`           DOUBLE          NOT NULL DEFAULT 0.0,
    `total_coins_earned` BIGINT          NOT NULL DEFAULT 0,
    `total_coins_spent`  BIGINT          NOT NULL DEFAULT 0,
    `updated_at`         DATETIME(6)     NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_statistics_user` UNIQUE (`user_id`),
    CONSTRAINT `fk_statistics_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    INDEX `idx_statistics_user_id` (`user_id`),
    INDEX `idx_statistics_win_rate` (`win_rate`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 8. Match Histories Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `match_histories` (
    `id`            VARCHAR(36)     NOT NULL,
    `match_id`      VARCHAR(36)     NOT NULL,
    `user_id`       VARCHAR(36)     NOT NULL,
    `final_rank`    INT             NULL,
    `coins_at_end`  INT             NOT NULL DEFAULT 0,
    `eliminated`    BOOLEAN         NOT NULL DEFAULT FALSE,
    `created_at`    DATETIME(6)     NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_match_histories_match` FOREIGN KEY (`match_id`) REFERENCES `matches` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_match_histories_user`  FOREIGN KEY (`user_id`)  REFERENCES `users`   (`id`) ON DELETE CASCADE,
    CONSTRAINT `uk_match_history_match_user` UNIQUE (`match_id`, `user_id`),
    INDEX `idx_match_history_user_id` (`user_id`),
    INDEX `idx_match_history_match_id` (`match_id`),
    INDEX `idx_match_history_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;