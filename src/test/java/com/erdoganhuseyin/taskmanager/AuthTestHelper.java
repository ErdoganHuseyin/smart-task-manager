package com.erdoganhuseyin.taskmanager;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Registers a fresh user and logs in, so integration tests can call protected endpoints.
 * Each call uses a new random email, so tests never clash with each other's data.
 */
public final class AuthTestHelper {

    private AuthTestHelper() {
    }

    /** Returns a ready-to-use "Authorization" header value: "Bearer eyJ...". */
    public static String registerAndLogin(MockMvc mockMvc) throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        String credentials = """
                {"email": "%s", "password": "secret-password"}
                """.formatted(email);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials))
                .andExpect(status().isCreated());

        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return "Bearer " + JsonPath.read(body, "$.accessToken");
    }
}
