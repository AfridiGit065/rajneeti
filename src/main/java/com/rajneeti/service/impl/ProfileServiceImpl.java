package com.rajneeti.service.impl;

import com.rajneeti.dto.profile.ProfileResponse;
import com.rajneeti.dto.profile.StatisticsResponse;
import com.rajneeti.dto.profile.UpdateProfileRequest;
import com.rajneeti.entity.Statistics;
import com.rajneeti.entity.User;
import com.rajneeti.exception.ResourceNotFoundException;
import com.rajneeti.exception.UsernameAlreadyExistsException;
import com.rajneeti.repository.StatisticsRepository;
import com.rajneeti.repository.UserRepository;
import com.rajneeti.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of {@link ProfileService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final StatisticsRepository statisticsRepository;

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID userId) {
        User user = findUserById(userId);
        return mapToProfileResponse(user);
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUserById(userId);

        String newUsername = request.getUsername().trim();

        // Check if username changed and is unique
        if (!newUsername.equalsIgnoreCase(user.getUsername())) {
            if (userRepository.existsByUsername(newUsername)) {
                throw new UsernameAlreadyExistsException("Username '%s' is already taken.".formatted(newUsername));
            }
            log.info("User '{}' updated username to '{}'", user.getUsername(), newUsername);
            user.setUsername(newUsername);
        }

        // Update avatar URL (sanitizing blank to null)
        String newAvatarUrl = request.getAvatarUrl();
        if (newAvatarUrl != null) {
            newAvatarUrl = newAvatarUrl.trim();
            if (newAvatarUrl.isEmpty()) {
                newAvatarUrl = null;
            }
        }
        user.setAvatarUrl(newAvatarUrl);

        User updatedUser = userRepository.save(user);
        log.info("Profile updated successfully for user ID: {}", userId);

        return mapToProfileResponse(updatedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public StatisticsResponse getStatistics(UUID userId) {
        User user = findUserById(userId);

        Statistics stats = statisticsRepository.findByUserId(userId).orElse(null);

        Integer totalMatches = (stats != null) ? stats.getTotalMatches() : user.getTotalMatches();
        Integer wins = (stats != null) ? stats.getWins() : user.getWins();
        Integer losses = (stats != null) ? stats.getLosses() : user.getLosses();
        Double winRate = (stats != null) ? stats.getWinRate() : calculateWinRate(wins, totalMatches);
        Long coinsEarned = (stats != null) ? stats.getTotalCoinsEarned() : 0L;
        Long coinsSpent = (stats != null) ? stats.getTotalCoinsSpent() : 0L;

        return StatisticsResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .rating(user.getRating())
                .totalMatches(totalMatches)
                .wins(wins)
                .losses(losses)
                .winRate(winRate)
                .totalCoinsEarned(coinsEarned)
                .totalCoinsSpent(coinsSpent)
                .build();
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private ProfileResponse mapToProfileResponse(User user) {
        Double winRate = calculateWinRate(user.getWins(), user.getTotalMatches());

        return ProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .rating(user.getRating())
                .totalMatches(user.getTotalMatches())
                .wins(user.getWins())
                .losses(user.getLosses())
                .winRate(winRate)
                .build();
    }

    private Double calculateWinRate(Integer wins, Integer totalMatches) {
        if (totalMatches != null && totalMatches > 0 && wins != null) {
            return Math.round(((double) wins / totalMatches * 100.0) * 100.0) / 100.0;
        }
        return 0.0;
    }
}