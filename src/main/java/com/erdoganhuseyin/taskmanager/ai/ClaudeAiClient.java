package com.erdoganhuseyin.taskmanager.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.StructuredOutputConfig;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * The real {@link AiClient}: calls Claude through the official Anthropic Java SDK.
 *
 * This is the only class in the project that knows about the Anthropic SDK.
 * If we ever switch models or SDK versions, this is the only file that changes.
 */
@Component
public class ClaudeAiClient implements AiClient {

    private static final long MAX_TOKENS = 16000L;
    private static final String FALLBACK_BETA = "server-side-fallback-2026-07-01";
    private static final DateTimeFormatter NOW_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX, EEEE");

    private static final String PARSE_SYSTEM_PROMPT = """
            You turn one sentence from a to-do app user into a task.
            The user usually writes in Turkish. Keep the title and description in the user's language.
            The title is short, like a to-do item ("Dişçi randevusu"), not a copy of the whole sentence.
            Put any remaining useful details in the description.
            Resolve relative times ("yarın", "cuma", "haftaya") against the current time given in the message.
            In everyday Turkish, an hour from 1 to 7 without "sabah" usually means the afternoon (saat 3 = 15:00).
            """;

    private static final String SUMMARY_SYSTEM_PROMPT = """
            You write a short weekly summary for a to-do app user, in Turkish.
            Use at most 5 short sentences of plain text, no Markdown and no lists.
            Say what was finished, what is still open, and point out anything overdue or due soon.
            End with one concrete suggestion for what to do next.
            Only use the tasks you are given; do not invent tasks.
            """;

    private final AiProperties properties;
    private final AnthropicClient client; // null when there is no API key

    public ClaudeAiClient(AiProperties properties) {
        this.properties = properties;
        this.client = properties.hasApiKey()
                ? AnthropicOkHttpClient.builder().apiKey(properties.apiKey()).build()
                : null;
    }

    @Override
    public ParsedTask parseTask(String text, ZonedDateTime now) {
        StructuredMessageCreateParams.Builder<ParsedTask> builder = MessageCreateParams.builder()
                .model(properties.model())
                .maxTokens(MAX_TOKENS)
                .system(PARSE_SYSTEM_PROMPT)
                // Structured output: Claude must answer with JSON that matches the ParsedTask record.
                .outputConfig(StructuredOutputConfig.<ParsedTask>builder()
                        .format(ParsedTask.class)
                        .effort(OutputConfig.Effort.LOW) // a simple extraction, no deep thinking needed
                        .build())
                .addUserMessage("Current time: " + now.format(NOW_FORMAT) + "\n\nSentence: " + text);
        addFallbacks(builder);

        StructuredMessage<ParsedTask> response = call(() -> client().messages().create(builder.build()));
        // Always check WHY Claude stopped before reading the answer.
        checkStopReason(response.stopReason());

        return response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(textBlock -> textBlock.text())
                .findFirst()
                .orElseThrow(() -> new AiUnusableAnswerException("Claude returned no task"));
    }

    @Override
    public String summarizeTasks(String taskList, ZonedDateTime now) {
        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(properties.model())
                .maxTokens(MAX_TOKENS)
                .system(SUMMARY_SYSTEM_PROMPT)
                .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
                .addUserMessage("Current time: " + now.format(NOW_FORMAT) + "\n\nTasks:\n" + taskList);
        if (properties.fallbacks()) {
            builder.putAdditionalHeader("anthropic-beta", FALLBACK_BETA)
                    .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"));
        }

        Message response = call(() -> client().messages().create(builder.build()));
        checkStopReason(response.stopReason());

        String summary = response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(textBlock -> textBlock.text())
                .reduce("", String::concat)
                .strip();
        if (summary.isEmpty()) {
            throw new AiUnusableAnswerException("Claude returned an empty summary");
        }
        return summary;
    }

    /**
     * Server-side fallbacks: if Claude's safety checks decline a request, Anthropic re-runs it
     * on a recommended fallback model instead of returning the refusal. It needs a beta header.
     */
    private void addFallbacks(StructuredMessageCreateParams.Builder<?> builder) {
        if (properties.fallbacks()) {
            builder.putAdditionalHeader("anthropic-beta", FALLBACK_BETA)
                    .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"));
        }
    }

    private AnthropicClient client() {
        if (client == null) {
            throw new AiUnavailableException("AI is not configured: set the ANTHROPIC_API_KEY environment variable");
        }
        return client;
    }

    /** Turns SDK errors (network, rate limit, bad key...) into our own exception, so the SDK does not leak out. */
    private static <T> T call(java.util.function.Supplier<T> request) {
        try {
            return request.get();
        } catch (AnthropicException e) {
            throw new AiUnavailableException("Claude could not be reached: " + e.getMessage(), e);
        }
    }

    private static void checkStopReason(Optional<StopReason> stopReason) {
        if (stopReason.isEmpty()) {
            return;
        }
        StopReason reason = stopReason.get();
        if (reason.equals(StopReason.REFUSAL)) {
            throw new AiUnusableAnswerException("Claude declined this request");
        }
        if (reason.equals(StopReason.MAX_TOKENS)) {
            throw new AiUnusableAnswerException("Claude's answer was cut off");
        }
    }
}
