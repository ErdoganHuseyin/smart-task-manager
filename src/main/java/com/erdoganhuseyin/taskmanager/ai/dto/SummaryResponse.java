package com.erdoganhuseyin.taskmanager.ai.dto;

/**
 * Body of GET /api/tasks/summary.
 *
 * @param summary   the text Claude wrote (or a fixed text when there is nothing to summarize)
 * @param taskCount how many tasks the summary is based on
 */
public record SummaryResponse(String summary, int taskCount) {
}
