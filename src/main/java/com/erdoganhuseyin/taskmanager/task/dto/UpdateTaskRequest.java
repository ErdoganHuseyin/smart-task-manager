package com.erdoganhuseyin.taskmanager.task.dto;

import com.erdoganhuseyin.taskmanager.task.TaskPriority;
import com.erdoganhuseyin.taskmanager.task.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record UpdateTaskRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @NotNull TaskStatus status,
        @NotNull TaskPriority priority,
        Instant dueDate
) {
}
