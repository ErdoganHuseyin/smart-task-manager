package com.erdoganhuseyin.taskmanager.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        // BCrypt only looks at the first 72 bytes of a password, so we cap it there.
        @NotBlank @Size(min = 8, max = 72) String password
) {
}
