package com.erdoganhuseyin.taskmanager.ai;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** Plain unit tests: no Spring, no database, no Claude. */
class AiTaskServiceTest {

    @Test
    void parseDueDateConvertsOffsetTimeToInstant() {
        assertThat(AiTaskService.parseDueDate("2026-11-29T15:00:00+03:00"))
                .isEqualTo(Instant.parse("2026-11-29T12:00:00Z"));
    }

    @Test
    void parseDueDateReturnsNullForEmptyOrInvalidText() {
        assertThat(AiTaskService.parseDueDate("")).isNull();
        assertThat(AiTaskService.parseDueDate(null)).isNull();
        assertThat(AiTaskService.parseDueDate("yarın")).isNull();
    }
}
