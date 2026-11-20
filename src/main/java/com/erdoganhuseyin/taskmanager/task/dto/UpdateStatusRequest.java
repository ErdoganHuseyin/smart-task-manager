package com.erdoganhuseyin.taskmanager.task.dto;

import com.erdoganhuseyin.taskmanager.task.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UpdateStatusRequest {
@NotNull
    TaskStatus status;

    public TaskStatus getStatus() {
        return status;
    }


}
