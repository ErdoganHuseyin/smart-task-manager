package com.erdoganhuseyin.taskmanager.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Reads the "app.jwt.*" settings from application.yml into a type-safe object.
 *
 * @param secret     key used to sign tokens (HMAC-SHA256). Must be at least 32 bytes.
 * @param expiration how long a token stays valid, e.g. "1h" or "30m"
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration expiration) {
}
