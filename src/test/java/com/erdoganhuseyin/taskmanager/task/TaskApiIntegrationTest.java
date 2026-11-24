package com.erdoganhuseyin.taskmanager.task;

import com.erdoganhuseyin.taskmanager.AuthTestHelper;
import com.erdoganhuseyin.taskmanager.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test: the whole application against a real PostgreSQL (Testcontainers).
 * Walks through the full life of a task: create, read, update, list, delete.
 * Every request sends a real JWT, obtained by registering and logging in.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TaskApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String token;

    @BeforeEach
    void logIn() throws Exception {
        token = AuthTestHelper.registerAndLogin(mockMvc);
    }

    @Test
    void fullTaskLifecycle() throws Exception {
        // Create
        long id = createTask(token, "Prepare for interview");

        // Read
        mockMvc.perform(get("/api/tasks/{id}", id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Prepare for interview"));

        // Update
        mockMvc.perform(put("/api/tasks/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Prepare for interview", "status": "DONE", "priority": "HIGH"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        // Patch only the status
        mockMvc.perform(patch("/api/tasks/{id}/status", id)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "IN_PROGRESS"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // List with a status filter
        mockMvc.perform(get("/api/tasks").param("status", "IN_PROGRESS").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %d)]".formatted(id)).exists());

        // Delete, then it is gone
        mockMvc.perform(delete("/api/tasks/{id}", id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/tasks/{id}", id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());
    }

    @Test
    void usersCannotSeeEachOthersTasks() throws Exception {
        long id = createTask(token, "Ali's private task");
        String otherToken = AuthTestHelper.registerAndLogin(mockMvc);

        // The other user gets 404, not 403: we do not even admit that the task exists.
        mockMvc.perform(get("/api/tasks/{id}", id).header(HttpHeaders.AUTHORIZATION, otherToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/tasks/{id}", id).header(HttpHeaders.AUTHORIZATION, otherToken))
                .andExpect(status().isNotFound());

        // And the task is not in their list.
        mockMvc.perform(get("/api/tasks").header(HttpHeaders.AUTHORIZATION, otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %d)]".formatted(id)).doesNotExist());

        // The owner still sees it.
        mockMvc.perform(get("/api/tasks/{id}", id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
    }

    @Test
    void listReturns400WhenSortPropertyIsInvalid() throws Exception {
        mockMvc.perform(get("/api/tasks").param("sort", "foo").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/tasks").param("sort", "[\"string\"]").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns401WithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/tasks").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    private long createTask(String authHeader, String title) throws Exception {
        String body = mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "%s", "priority": "HIGH", "dueDate": "2026-12-01T09:00:00Z"}
                                """.formatted(title)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
