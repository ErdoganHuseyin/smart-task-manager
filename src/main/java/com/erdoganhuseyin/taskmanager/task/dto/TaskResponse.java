package com.erdoganhuseyin.taskmanager.task.dto;

import com.erdoganhuseyin.taskmanager.task.Task;
import com.erdoganhuseyin.taskmanager.task.TaskPriority;
import com.erdoganhuseyin.taskmanager.task.TaskStatus;

import java.time.Instant;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        Instant dueDate,
        Instant createdAt,
        Instant updatedAt
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
