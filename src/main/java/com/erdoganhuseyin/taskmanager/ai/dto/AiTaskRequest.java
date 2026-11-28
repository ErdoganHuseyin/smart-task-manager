package com.erdoganhuseyin.taskmanager.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of POST /api/tasks/ai, e.g. {"text": "Yarın saat 3'te dişçi randevusu, önemli"} */
public record AiTaskRequest(@NotBlank @Size(max = 1000) String text) {
}
