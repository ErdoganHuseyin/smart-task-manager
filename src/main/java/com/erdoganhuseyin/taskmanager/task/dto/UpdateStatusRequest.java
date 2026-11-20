package com.erdoganhuseyin.taskmanager.task.dto;

import com.erdoganhuseyin.taskmanager.task.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest (@NotNull TaskStatus status){
}
