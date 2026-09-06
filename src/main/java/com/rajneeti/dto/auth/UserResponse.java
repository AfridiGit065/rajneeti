package com.rajneeti.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Safe public representation of a user without sensitive credentials.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    private UUID id;
    private String username;
    private String email;
    private String avatarUrl;
    private Integer rating;
    private Integer totalMatches;
    private Integer wins;
    private Integer losses;
    private LocalDateTime createdAt;
}