package com.erdoganhuseyin.taskmanager.ai;

import java.time.ZonedDateTime;

/**
 * Everything our app needs from an AI model, and nothing more.
 *
 * Why an interface? The rest of the code depends on this, not on Claude directly.
 * In tests we replace it with a fake (Mockito), so tests are free, fast and need no API key.
 * The real implementation is {@link ClaudeAiClient}.
 */
public interface AiClient {

    /** Turns a sentence like "Yarın saat 3'te dişçi randevusu, önemli" into task fields. */
    ParsedTask parseTask(String text, ZonedDateTime now);

    /** Writes a short summary of the given task list (already formatted as plain text). */
    String summarizeTasks(String taskList, ZonedDateTime now);
}
