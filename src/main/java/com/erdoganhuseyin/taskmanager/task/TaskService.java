package com.erdoganhuseyin.taskmanager.task;

import com.erdoganhuseyin.taskmanager.task.dto.CreateTaskRequest;
import com.erdoganhuseyin.taskmanager.task.dto.TaskResponse;
import com.erdoganhuseyin.taskmanager.task.dto.UpdateStatusRequest;
import com.erdoganhuseyin.taskmanager.task.dto.UpdateTaskRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public Page<TaskResponse> findAll(TaskStatus status, Pageable pageable) {
        Page<Task> tasks = (status == null)
                ? taskRepository.findAll(pageable)
                : taskRepository.findByStatus(status, pageable);
        return tasks.map(TaskResponse::from);
    }

    public TaskResponse findById(Long id) {
        return TaskResponse.from(getTask(id));
    }

    @Transactional
    public TaskResponse create(CreateTaskRequest request) {
        Task task = new Task(request.title(), request.description(), request.priority(), request.dueDate());
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse update(Long id, UpdateTaskRequest request) {
        Task task = getTask(id);
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
    public void delete(Long id) {
        Task task = getTask(id);
        taskRepository.delete(task);
    }

    private Task getTask(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    @Transactional
    public TaskResponse updateStatus(Long id, UpdateStatusRequest request) {
        Task task = getTask(id);
        task.setStatus(request.getStatus());
        return TaskResponse.from(taskRepository.saveAndFlush(task));

        // 1. görevi bul (sınıfta bunun için hazır bir private metot var)
        // 2. status'unu değiştir
        // 3. kaydet ve TaskResponse döndür (update metoduna bak)
    }
}
