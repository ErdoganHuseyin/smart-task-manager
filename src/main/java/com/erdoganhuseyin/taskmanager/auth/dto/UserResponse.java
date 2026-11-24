package com.erdoganhuseyin.taskmanager.auth.dto;

import com.erdoganhuseyin.taskmanager.user.User;

// Never return the User entity itself: it contains the password hash.
public record UserResponse(Long id, String email) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail());
    }
}
