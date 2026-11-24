package com.erdoganhuseyin.taskmanager.task;

import com.erdoganhuseyin.taskmanager.task.dto.CreateTaskRequest;
import com.erdoganhuseyin.taskmanager.task.dto.TaskResponse;
import com.erdoganhuseyin.taskmanager.user.User;
import com.erdoganhuseyin.taskmanager.user.UserRepository;
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

    private static final Long USER_ID = 1L;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void createUsesMediumPriorityWhenNoneGiven() {
        when(userRepository.getReferenceById(USER_ID)).thenReturn(new User("ali@example.com", "hash"));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.create(USER_ID, new CreateTaskRequest("Write README", null, null, null));

        assertThat(response.title()).isEqualTo("Write README");
        assertThat(response.priority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void findByIdThrowsWhenTaskDoesNotExist() {
        when(taskRepository.findByIdAndOwnerId(42L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findById(USER_ID, 42L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessage("Task not found: 42");
    }

    @Test
    void deleteDoesNothingWhenTaskDoesNotExist() {
        when(taskRepository.findByIdAndOwnerId(42L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.delete(USER_ID, 42L)).isInstanceOf(TaskNotFoundException.class);
        verify(taskRepository, never()).delete(any());
    }
}
