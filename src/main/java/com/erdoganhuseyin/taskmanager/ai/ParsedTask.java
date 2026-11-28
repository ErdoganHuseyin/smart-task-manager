package com.erdoganhuseyin.taskmanager.ai;

import com.erdoganhuseyin.taskmanager.task.TaskPriority;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * The shape we ask Claude to answer in (structured output).
 * The SDK turns this record into a JSON schema, and Claude's answer is guaranteed to match it.
 * The descriptions below are part of the schema, so Claude reads them like instructions.
 */
public record ParsedTask(
        @JsonPropertyDescription("Short task title, at most 200 characters, in the user's language")
        String title,

        @JsonPropertyDescription("Extra details from the sentence, or an empty string if there are none")
        String description,

        @JsonPropertyDescription("HIGH if the user says it is important or urgent, LOW if it can wait, otherwise MEDIUM")
        TaskPriority priority,

        @JsonPropertyDescription("When the task is due, as ISO-8601 with offset (e.g. 2026-11-29T15:00:00+03:00), "
                + "or an empty string if the sentence gives no time")
        String dueDate
) {
}
