package com.rajneeti.mapper;

import com.rajneeti.dto.auth.UserResponse;
import com.rajneeti.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper for transforming {@link User} entity to {@link UserResponse} DTO.
 */
@Component
public class UserMapper {

    public UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .rating(user.getRating())
                .totalMatches(user.getTotalMatches())
                .wins(user.getWins())
                .losses(user.getLosses())
                .createdAt(user.getCreatedAt())
                .build();
    }
}