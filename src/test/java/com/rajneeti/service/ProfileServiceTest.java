package com.rajneeti.service;

import com.rajneeti.dto.profile.ProfileResponse;
import com.rajneeti.dto.profile.StatisticsResponse;
import com.rajneeti.dto.profile.UpdateProfileRequest;
import com.rajneeti.entity.Statistics;
import com.rajneeti.entity.User;
import com.rajneeti.exception.ResourceNotFoundException;
import com.rajneeti.exception.UsernameAlreadyExistsException;
import com.rajneeti.repository.StatisticsRepository;
import com.rajneeti.repository.UserRepository;
import com.rajneeti.service.impl.ProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StatisticsRepository statisticsRepository;

    @InjectMocks
    private ProfileServiceImpl profileService;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .id(testUserId)
                .username("testplayer")
                .email("test@rajneeti.com")
                .avatarUrl("https://example.com/avatar.png")
                .rating(1200)
                .totalMatches(10)
                .wins(7)
                .losses(3)
                .build();
    }

    @Test
    @DisplayName("Get Profile - Success")
    void getProfile_Success() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        ProfileResponse response = profileService.getProfile(testUserId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(testUserId);
        assertThat(response.getUsername()).isEqualTo("testplayer");
        assertThat(response.getEmail()).isEqualTo("test@rajneeti.com");
        assertThat(response.getAvatarUrl()).isEqualTo("https://example.com/avatar.png");
        assertThat(response.getRating()).isEqualTo(1200);
        assertThat(response.getTotalMatches()).isEqualTo(10);
        assertThat(response.getWins()).isEqualTo(7);
        assertThat(response.getLosses()).isEqualTo(3);
        assertThat(response.getWinRate()).isEqualTo(70.0);
    }

    @Test
    @DisplayName("Get Profile - Fails when user not found")
    void getProfile_UserNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getProfile(unknownId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }

    @Test
    @DisplayName("Update Profile - Success")
    void updateProfile_Success() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .username("newname")
                .avatarUrl("https://example.com/newavatar.png")
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("newname")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        ProfileResponse response = profileService.updateProfile(testUserId, request);

        assertThat(response).isNotNull();
        assertThat(testUser.getUsername()).isEqualTo("newname");
        assertThat(testUser.getAvatarUrl()).isEqualTo("https://example.com/newavatar.png");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Update Profile - Fails when new username is already taken")
    void updateProfile_DuplicateUsername() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .username("existinguser")
                .avatarUrl(null)
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThatThrownBy(() -> profileService.updateProfile(testUserId, request))
                .isInstanceOf(UsernameAlreadyExistsException.class)
                .hasMessageContaining("existinguser");
    }

    @Test
    @DisplayName("Get Statistics - Success")
    void getStatistics_Success() {
        Statistics stats = Statistics.builder()
                .user(testUser)
                .totalMatches(10)
                .wins(7)
                .losses(3)
                .winRate(70.0)
                .totalCoinsEarned(500L)
                .totalCoinsSpent(200L)
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(statisticsRepository.findByUserId(testUserId)).thenReturn(Optional.of(stats));

        StatisticsResponse response = profileService.getStatistics(testUserId);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(testUserId);
        assertThat(response.getUsername()).isEqualTo("testplayer");
        assertThat(response.getRating()).isEqualTo(1200);
        assertThat(response.getTotalMatches()).isEqualTo(10);
        assertThat(response.getWins()).isEqualTo(7);
        assertThat(response.getLosses()).isEqualTo(3);
        assertThat(response.getWinRate()).isEqualTo(70.0);
        assertThat(response.getTotalCoinsEarned()).isEqualTo(500L);
        assertThat(response.getTotalCoinsSpent()).isEqualTo(200L);
    }
}