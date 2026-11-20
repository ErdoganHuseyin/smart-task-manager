package com.erdoganhuseyin.taskmanager.task;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    // Spring Data builds the SQL from the method name:
    // SELECT * FROM tasks WHERE status = ? (with paging)
    Page<Task> findByStatus(TaskStatus status, Pageable pageable);
}
