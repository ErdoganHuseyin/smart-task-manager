package com.erdoganhuseyin.taskmanager.task;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    // "OwnerId" means owner.id, so Spring Data writes: SELECT ... FROM tasks WHERE user_id = ?
    Page<Task> findByOwnerId(Long ownerId, Pageable pageable);

    // WHERE user_id = ? AND status = ?  (served by the idx_tasks_user_status index)
    Page<Task> findByOwnerIdAndStatus(Long ownerId, TaskStatus status, Pageable pageable);

    // A task is found only if it belongs to this user. Someone else's task looks like "not found".
    Optional<Task> findByIdAndOwnerId(Long id, Long ownerId);
}
