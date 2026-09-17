package com.rajneeti.bot.service;

import com.rajneeti.dto.websocket.RoomPayload;
import com.rajneeti.dto.websocket.WebSocketEventType;
import com.rajneeti.entity.Leaderboard;
import com.rajneeti.entity.Room;
import com.rajneeti.entity.RoomPlayer;
import com.rajneeti.entity.Statistics;
import com.rajneeti.entity.User;
import com.rajneeti.repository.LeaderboardRepository;
import com.rajneeti.repository.RoomPlayerRepository;
import com.rajneeti.repository.StatisticsRepository;
import com.rajneeti.repository.UserRepository;
import com.rajneeti.websocket.WebSocketEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Module 25 — creates the AI bot accounts behind "add bots to room".
 *
 * <p>Bots are first-class {@link User}s (flagged {@code isBot=true}), so every
 * downstream subsystem — rooms, matches, leaderboard, statistics, game state —
 * treats them exactly like humans. They never log in: a dummy random encoded
 * password is stored purely to satisfy the NOT NULL password column. Each bot
 * is seated immediately with {@code ready=true} so the host only has to ready
 * up and start.
 *
 * <p>Seat allocation: the room host keeps seat 1; bots take seats 2..N.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BotUserService {

    private static final String[] NAME_POOL = {
            "Ayesha", "Arif", "Zara", "Kabir", "Nadia",
            "Rafi", "Sana", "Tariq", "Maya", "Danish"};

    private final UserRepository userRepository;
    private final LeaderboardRepository leaderboardRepository;
    private final StatisticsRepository statisticsRepository;
    private final RoomPlayerRepository roomPlayerRepository;
    private final PasswordEncoder passwordEncoder;
    private final WebSocketEventPublisher webSocketEventPublisher;

    /**
     * Seeds {@code botCount} ready-to-play AI bots into the given room.
     *
     * @param room        the room the bots join (host already on seat 1)
     * @param botCount    number of bots to add
     * @param difficulty  difficulty name (EASY / MEDIUM / HARD)
     * @param personality personality name (BALANCED / AGGRESSIVE / ...)
     * @return the created {@link RoomPlayer} rows (seat 2..botCount+1)
     */
    @Transactional
    public List<RoomPlayer> seedBots(Room room, int botCount,
                                     String difficulty, String personality) {
        List<RoomPlayer> created = new ArrayList<>(botCount);
        List<RoomPlayer> current = roomPlayerRepository.findByRoomIdOrderBySeatNumberAsc(room.getId());
        int nextSeat = current.stream()
                .mapToInt(RoomPlayer::getSeatNumber)
                .max()
                .orElse(0) + 1;

        for (int i = 0; i < botCount; i++) {
            String baseName = NAME_POOL[(nextSeat + i) % NAME_POOL.length];
            User bot = createBotUser(baseName, difficulty, personality);

            RoomPlayer botPlayer = roomPlayerRepository.save(RoomPlayer.builder()
                    .room(room)
                    .user(bot)
                    .seatNumber(nextSeat + i)
                    .ready(true)
                    .build());

            created.add(botPlayer);

            webSocketEventPublisher.publishToRoom(room.getId(), WebSocketEventType.JOIN_ROOM,
                    bot.getId(), RoomPayload.builder()
                            .playerId(bot.getId())
                            .username(bot.getUsername())
                            .seatNumber(botPlayer.getSeatNumber())
                            .ready(true)
                            .host(false)
                            .isBot(true)
                            .playerCount(current.size() + created.size() + 1)
                            .build());

            log.info("Bot '{}' (seat {}) joined room '{}'",
                    bot.getUsername(), botPlayer.getSeatNumber(), room.getRoomCode());
        }
        return created;
    }

    private User createBotUser(String baseName, String difficulty, String personality) {
        String username;
        String email;
        int suffix = 0;
        do {
            suffix++;
            username = baseName + "Bot" + suffix;
            email = "bot-" + UUID.randomUUID().toString().substring(0, 8) + "@rajneeti.bot";
        } while (userRepository.existsByUsername(username));

        User bot = userRepository.save(User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .isBot(true)
                .botDifficulty(difficulty)
                .botPersonality(personality)
                .rating(1000)
                .totalMatches(0)
                .wins(0)
                .losses(0)
                .build());

        leaderboardRepository.save(Leaderboard.builder()
                .user(bot)
                .rating(1000)
                .wins(0)
                .losses(0)
                .totalMatches(0)
                .build());

        statisticsRepository.save(Statistics.builder()
                .user(bot)
                .totalMatches(0)
                .wins(0)
                .losses(0)
                .winRate(0.0)
                .totalCoinsEarned(0L)
                .totalCoinsSpent(0L)
                .build());

        return bot;
    }
}