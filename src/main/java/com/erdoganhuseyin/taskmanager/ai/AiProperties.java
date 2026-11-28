package com.erdoganhuseyin.taskmanager.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.ZoneId;

/**
 * Reads the "app.ai.*" settings from application.yml.
 *
 * @param apiKey    Anthropic API key. Comes from the ANTHROPIC_API_KEY environment variable, never from code.
 *                  May be empty: the app still starts, only the AI endpoints answer 503.
 * @param model     which Claude model to call, e.g. "claude-opus-5-5"
 * @param timeZone  the user's time zone. Claude needs it to turn "tomorrow at 3" into an exact moment.
 * @param fallbacks if true, a request Claude's safety checks decline is retried on another model by Anthropic
 */
@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(String apiKey, String model, ZoneId timeZone, boolean fallbacks) {

    /** Haiku models have no server-side fallback: sending "fallbacks" to them is rejected (HTTP 400). */
    public boolean useFallbacks() {
        return fallbacks && model != null && !model.startsWith("claude-haiku");
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
