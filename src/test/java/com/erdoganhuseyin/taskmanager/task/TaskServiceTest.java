package com.erdoganhuseyin.taskmanager.task;

import com.erdoganhuseyin.taskmanager.task.dto.CreateTaskRequest;
import com.erdoganhuseyin.taskmanager.task.dto.TaskResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test: no Spring, no database. The repository is replaced with a Mockito mock,
 * so we test only the business logic in TaskService. Runs in milliseconds.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void createUsesMediumPriorityWhenNoneGiven() {
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.create(new CreateTaskRequest("Write README", null, null, null));

        assertThat(response.title()).isEqualTo("Write README");
        assertThat(response.priority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void findByIdThrowsWhenTaskDoesNotExist() {
        when(taskRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findById(42L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessage("Task not found: 42");
    }

    @Test
    void deleteDoesNothingWhenTaskDoesNotExist() {
        when(taskRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.delete(42L)).isInstanceOf(TaskNotFoundException.class);
        verify(taskRepository, never()).delete(any());
    }
}
