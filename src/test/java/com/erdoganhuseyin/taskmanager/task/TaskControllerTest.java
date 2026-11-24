package com.erdoganhuseyin.taskmanager.task;

import com.erdoganhuseyin.taskmanager.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer test: starts only the controller layer (no database).
 * Checks HTTP concerns: status codes, validation, security and the JSON error format.
 *
 * .with(jwt()) pretends the request carries a valid token, without creating a real one.
 * Here the token's subject (the user id) is "1".
 */
@WebMvcTest(TaskController.class)
@Import(SecurityConfig.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @Test
    void createReturns400WhenTitleIsBlank() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .with(jwt().jwt(token -> token.subject("1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "  "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void updateStatusReturns400WhenStatusIsMissing() throws Exception {
        mockMvc.perform(patch("/api/tasks/1/status")
                        .with(jwt().jwt(token -> token.subject("1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.status").exists());
    }

    @Test
    void getReturns404WhenTaskDoesNotExist() throws Exception {
        when(taskService.findById(1L, 99L)).thenThrow(new TaskNotFoundException(99L));

        mockMvc.perform(get("/api/tasks/99").with(jwt().jwt(token -> token.subject("1"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Task not found: 99"));
    }

    @Test
    void returns401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized());
    }
}
