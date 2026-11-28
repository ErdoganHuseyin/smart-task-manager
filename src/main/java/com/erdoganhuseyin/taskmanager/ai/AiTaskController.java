package com.erdoganhuseyin.taskmanager.ai;

import com.erdoganhuseyin.taskmanager.ai.dto.AiTaskRequest;
import com.erdoganhuseyin.taskmanager.ai.dto.SummaryResponse;
import com.erdoganhuseyin.taskmanager.security.CurrentUser;
import com.erdoganhuseyin.taskmanager.task.dto.TaskResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/tasks")
public class AiTaskController {

    private final AiTaskService aiTaskService;

    public AiTaskController(AiTaskService aiTaskService) {
        this.aiTaskService = aiTaskService;
    }

    @PostMapping("/ai")
    public ResponseEntity<TaskResponse> createFromText(@AuthenticationPrincipal Jwt jwt,
                                                       @Valid @RequestBody AiTaskRequest request) {
        TaskResponse created = aiTaskService.createFromText(CurrentUser.id(jwt), request.text());
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/tasks/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    // "/summary" is a fixed path, so Spring picks it before "/{id}" in TaskController.
    @GetMapping("/summary")
    public SummaryResponse weeklySummary(@AuthenticationPrincipal Jwt jwt) {
        return aiTaskService.weeklySummary(CurrentUser.id(jwt));
    }
}
