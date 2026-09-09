package com.rajneeti.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for updating player profile editable fields (username and avatarUrl).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Size(max = 255, message = "Avatar URL must not exceed 255 characters")
    @Pattern(
            regexp = "^(https?://.+|)$",
            message = "Avatar URL must be a valid HTTP/HTTPS URL or left empty"
    )
    private String avatarUrl;
}