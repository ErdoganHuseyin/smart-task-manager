package com.erdoganhuseyin.taskmanager.security;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Small helper: our tokens store the user's id in the standard "sub" (subject) claim.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
