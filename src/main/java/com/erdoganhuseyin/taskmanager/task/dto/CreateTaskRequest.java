package com.erdoganhuseyin.taskmanager.task.dto;

import com.erdoganhuseyin.taskmanager.task.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateTaskRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        TaskPriority priority,
        Instant dueDate
) {
}
