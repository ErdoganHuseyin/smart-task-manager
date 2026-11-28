package com.erdoganhuseyin.taskmanager.ai;

import com.erdoganhuseyin.taskmanager.ai.dto.SummaryResponse;
import com.erdoganhuseyin.taskmanager.task.Task;
import com.erdoganhuseyin.taskmanager.task.TaskPriority;
import com.erdoganhuseyin.taskmanager.task.TaskRepository;
import com.erdoganhuseyin.taskmanager.task.TaskService;
import com.erdoganhuseyin.taskmanager.task.TaskStatus;
import com.erdoganhuseyin.taskmanager.task.dto.CreateTaskRequest;
import com.erdoganhuseyin.taskmanager.task.dto.TaskResponse;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The AI features. It asks {@link AiClient} for help, then uses the normal task code to save.
 *
 * Note what is NOT here: no Anthropic SDK classes. This service only knows the AiClient interface.
 */
@Service
public class AiTaskService {

    static final String NO_TASKS_SUMMARY = "Bu hafta için görev yok. Yeni bir görev ekleyerek başlayabilirsin.";
    private static final int TITLE_MAX = 200;
    private static final int DESCRIPTION_MAX = 2000;

    private final AiClient aiClient;
    private final TaskService taskService;
    private final TaskRepository taskRepository;
    private final AiProperties properties;
    private final Clock clock;

    public AiTaskService(AiClient aiClient, TaskService taskService, TaskRepository taskRepository,
                         AiProperties properties, Clock clock) {
        this.aiClient = aiClient;
        this.taskService = taskService;
        this.taskRepository = taskRepository;
        this.properties = properties;
        this.clock = clock;
    }

    /** "Yarın saat 3'te dişçi randevusu, önemli" -> a saved task. */
    public TaskResponse createFromText(Long userId, String text) {
        // No @Transactional here on purpose: we must not hold a database transaction open
        // while waiting seconds for Claude. taskService.create opens its own short transaction.
        ParsedTask parsed = aiClient.parseTask(text, now());

        if (parsed.title() == null || parsed.title().isBlank()) {
            throw new AiUnusableAnswerException("Could not find a task in this text");
        }
        CreateTaskRequest request = new CreateTaskRequest(
                cut(parsed.title().strip(), TITLE_MAX),
                blankToNull(cut(parsed.description(), DESCRIPTION_MAX)),
                parsed.priority() != null ? parsed.priority() : TaskPriority.MEDIUM,
                parseDueDate(parsed.dueDate()));
        return taskService.create(userId, request);
    }

    /** A short summary of this week: open tasks, plus tasks finished in the last 7 days. */
    public SummaryResponse weeklySummary(Long userId) {
        Instant weekAgo = clock.instant().minus(Duration.ofDays(7));
        List<Task> tasks = taskRepository.findTop100ByOwnerIdOrderByUpdatedAtDesc(userId).stream()
                .filter(task -> task.getStatus() != TaskStatus.DONE || task.getUpdatedAt().isAfter(weekAgo))
                .toList();

        // Nothing to summarize: answer ourselves. Calling Claude here would cost money for nothing.
        if (tasks.isEmpty()) {
            return new SummaryResponse(NO_TASKS_SUMMARY, 0);
        }
        String summary = aiClient.summarizeTasks(toPlainText(tasks), now());
        return new SummaryResponse(summary, tasks.size());
    }

    private ZonedDateTime now() {
        return ZonedDateTime.now(clock).withZoneSameInstant(properties.timeZone());
    }

    /** One line per task, e.g. "- [TODO] Dişçi randevusu (öncelik: HIGH, son tarih: 2026-11-29T15:00+03:00)" */
    private String toPlainText(List<Task> tasks) {
        return tasks.stream()
                .map(task -> "- [%s] %s (öncelik: %s, son tarih: %s)".formatted(
                        task.getStatus(),
                        task.getTitle(),
                        task.getPriority(),
                        task.getDueDate() == null ? "yok"
                                : task.getDueDate().atZone(properties.timeZone())
                                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)))
                .collect(Collectors.joining("\n"));
    }

    /** Claude sends a date as text. If it is empty or not a valid date, we keep the task without one. */
    static Instant parseDueDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value.strip()).toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String cut(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
