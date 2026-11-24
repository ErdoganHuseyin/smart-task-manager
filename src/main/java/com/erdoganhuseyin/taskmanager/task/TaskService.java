package com.erdoganhuseyin.taskmanager.task;

import com.erdoganhuseyin.taskmanager.task.dto.CreateTaskRequest;
import com.erdoganhuseyin.taskmanager.task.dto.TaskResponse;
import com.erdoganhuseyin.taskmanager.task.dto.UpdateStatusRequest;
import com.erdoganhuseyin.taskmanager.task.dto.UpdateTaskRequest;
import com.erdoganhuseyin.taskmanager.user.User;
import com.erdoganhuseyin.taskmanager.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    public Page<TaskResponse> findAll(Long userId, TaskStatus status, Pageable pageable) {
        Page<Task> tasks = (status == null)
                ? taskRepository.findByOwnerId(userId, pageable)
                : taskRepository.findByOwnerIdAndStatus(userId, status, pageable);
        return tasks.map(TaskResponse::from);
    }

    public TaskResponse findById(Long userId, Long id) {
        return TaskResponse.from(getTask(userId, id));
    }

    @Transactional
    public TaskResponse create(Long userId, CreateTaskRequest request) {
        // getReferenceById does not run a SELECT; it only needs the id to fill tasks.user_id.
        User owner = userRepository.getReferenceById(userId);
        Task task = new Task(owner, request.title(), request.description(), request.priority(), request.dueDate());
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse update(Long userId, Long id, UpdateTaskRequest request) {
        Task task = getTask(userId, id);
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
        // The entity is "managed", so Hibernate would write the changes on commit anyway.
        // We flush now so @PreUpdate runs and the response contains the new updatedAt.
        return TaskResponse.from(taskRepository.saveAndFlush(task));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Task task = getTask(userId, id);
        taskRepository.delete(task);
    }

    @Transactional
    public TaskResponse updateStatus(Long userId, Long id, UpdateStatusRequest request) {
        Task task = getTask(userId, id);
        task.setStatus(request.status());
        return TaskResponse.from(taskRepository.saveAndFlush(task));
    }

    private Task getTask(Long userId, Long id) {
        return taskRepository.findByIdAndOwnerId(id, userId).orElseThrow(() -> new TaskNotFoundException(id));
    }
}
