package com.erdoganhuseyin.taskmanager.ai;

import com.erdoganhuseyin.taskmanager.AuthTestHelper;
import com.erdoganhuseyin.taskmanager.TestcontainersConfiguration;
import com.erdoganhuseyin.taskmanager.task.TaskPriority;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The AI endpoints, end to end, with a real database but a FAKE AiClient.
 *
 * {@code @MockitoBean} swaps the real ClaudeAiClient for a Mockito mock inside the Spring context.
 * We decide what "Claude" answers, so the tests are free, fast, repeatable and need no API key.
 * What we test is OUR code: validation, saving, error mapping, and not calling the AI when not needed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AiTaskApiIntegrationTest {

    private static final String SENTENCE = "Yarın saat 3'te dişçi randevusu, önemli";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiClient aiClient;

    private String token;

    @BeforeEach
    void logIn() throws Exception {
        token = AuthTestHelper.registerAndLogin(mockMvc);
    }

    @Test
    void createFromTextSavesTheTaskClaudeFound() throws Exception {
        when(aiClient.parseTask(eq(SENTENCE), any()))
                .thenReturn(new ParsedTask("Dişçi randevusu", "", TaskPriority.HIGH, "2026-11-29T15:00:00+03:00"));

        String body = mockMvc.perform(post("/api/tasks/ai")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text": "%s"}
                                """.formatted(SENTENCE)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.title").value("Dişçi randevusu"))
                .andExpect(jsonPath("$.description").doesNotExist()) // "" from Claude is stored as null
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.dueDate").value("2026-11-29T12:00:00Z")) // 15:00 Istanbul = 12:00 UTC
                .andReturn().getResponse().getContentAsString();

        // It is a normal task now: the usual endpoint finds it.
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();
        mockMvc.perform(get("/api/tasks/{id}", id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
    }

    @Test
    void createFromTextReturns400AndSkipsAiWhenTextIsBlank() throws Exception {
        mockMvc.perform(post("/api/tasks/ai")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text": "   "}
                                """))
                .andExpect(status().isBadRequest());

        // Validation stops the request before it reaches Claude, so a bad request costs nothing.
        verify(aiClient, never()).parseTask(anyString(), any());
    }

    @Test
    void createFromTextReturns401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/tasks/ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text": "%s"}
                                """.formatted(SENTENCE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createFromTextReturns422WhenClaudeFindsNoTask() throws Exception {
        when(aiClient.parseTask(anyString(), any()))
                .thenReturn(new ParsedTask("", "", TaskPriority.MEDIUM, ""));

        mockMvc.perform(post("/api/tasks/ai")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text": "merhaba"}
                                """))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void createFromTextReturns503WhenClaudeIsUnavailable() throws Exception {
        when(aiClient.parseTask(anyString(), any()))
                .thenThrow(new AiUnavailableException("AI is not configured"));

        mockMvc.perform(post("/api/tasks/ai")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text": "%s"}
                                """.formatted(SENTENCE)))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void summaryWithNoTasksDoesNotCallClaude() throws Exception {
        mockMvc.perform(get("/api/tasks/summary").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskCount").value(0))
                .andExpect(jsonPath("$.summary").value(AiTaskService.NO_TASKS_SUMMARY));

        verify(aiClient, never()).summarizeTasks(anyString(), any());
    }

    @Test
    void summarySendsOnlyMyTasksToClaude() throws Exception {
        createTask(token, "Benim görevim");
        String otherToken = AuthTestHelper.registerAndLogin(mockMvc);
        createTask(otherToken, "Başkasının görevi");

        when(aiClient.summarizeTasks(anyString(), any())).thenReturn("Bu hafta 1 açık görevin var.");

        mockMvc.perform(get("/api/tasks/summary").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskCount").value(1))
                .andExpect(jsonPath("$.summary").value("Bu hafta 1 açık görevin var."));

        // Claude saw my task in the list it got...
        verify(aiClient).summarizeTasks(contains("Benim görevim"), any());
        // ...and never someone else's.
        verify(aiClient, never()).summarizeTasks(contains("Başkasının görevi"), any());
    }

    private void createTask(String authHeader, String title) throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "%s"}
                                """.formatted(title)))
                .andExpect(status().isCreated());
    }
}
