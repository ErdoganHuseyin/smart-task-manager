package com.erdoganhuseyin.taskmanager.ai;

/** The AI service cannot be used right now (no API key, network error, rate limit...). Becomes HTTP 503. */
public class AiUnavailableException extends RuntimeException {

    public AiUnavailableException(String message) {
        super(message);
    }

    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
