package com.erdoganhuseyin.taskmanager.task;

import com.erdoganhuseyin.taskmanager.security.CurrentUser;
import com.erdoganhuseyin.taskmanager.task.dto.CreateTaskRequest;
import com.erdoganhuseyin.taskmanager.task.dto.TaskResponse;
import com.erdoganhuseyin.taskmanager.task.dto.UpdateStatusRequest;
import com.erdoganhuseyin.taskmanager.task.dto.UpdateTaskRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private static final Set<String> SORTABLE_FIELDS =
            Set.of("title", "status", "priority", "dueDate", "createdAt", "updatedAt");

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public Page<TaskResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) TaskStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            String property = order.getProperty();
            if (!SORTABLE_FIELDS.contains(property)) {
                throw new InvalidSortException(property);
            }
        }
        return taskService.findAll(CurrentUser.id(jwt), status, pageable);
    }

    @GetMapping("/{id}")
    public TaskResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return taskService.findById(CurrentUser.id(jwt), id);
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody CreateTaskRequest request) {
        TaskResponse created = taskService.create(CurrentUser.id(jwt), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public TaskResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                               @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.update(CurrentUser.id(jwt), id, request);
    }

    @PatchMapping("/{id}/status")
    public TaskResponse updateStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                     @Valid @RequestBody UpdateStatusRequest request) {
        return taskService.updateStatus(CurrentUser.id(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        taskService.delete(CurrentUser.id(jwt), id);
    }
}
