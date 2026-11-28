package com.erdoganhuseyin.taskmanager.ai;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClaudeAiClientTest {

    @Test
    void withoutApiKeyItFailsClearlyInsteadOfCallingClaude() {
        AiProperties noKey = new AiProperties("", "claude-opus-5-5", ZoneId.of("Europe/Istanbul"), true);
        ClaudeAiClient client = new ClaudeAiClient(noKey);

        assertThatThrownBy(() -> client.parseTask("Yarın dişçi", ZonedDateTime.now()))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("ANTHROPIC_API_KEY");
    }
}
