package com.erdoganhuseyin.taskmanager.auth;

import com.erdoganhuseyin.taskmanager.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerThenLoginReturnsToken() throws Exception {
        String email = randomEmail();

        mockMvc.perform(register(email, "secret-password"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value(email))
                // The response must never leak the password or its hash.
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mockMvc.perform(login(email, "secret-password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void registerReturns409WhenEmailIsTaken() throws Exception {
        String email = randomEmail();
        mockMvc.perform(register(email, "secret-password")).andExpect(status().isCreated());

        // Same email in different letter case is still the same account.
        mockMvc.perform(register(email.toUpperCase(), "another-password"))
                .andExpect(status().isConflict());
    }

    @Test
    void registerReturns400ForShortPassword() throws Exception {
        mockMvc.perform(register(randomEmail(), "short"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void loginReturns401ForWrongPasswordOrUnknownEmail() throws Exception {
        String email = randomEmail();
        mockMvc.perform(register(email, "secret-password")).andExpect(status().isCreated());

        mockMvc.perform(login(email, "wrong-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
        mockMvc.perform(login(randomEmail(), "secret-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void meReturnsCurrentUserWhenTokenIsValid() throws Exception {
        String email = randomEmail();
        mockMvc.perform(register(email, "secret-password"))
                .andExpect(status().isCreated());
        String body = mockMvc.perform(login(email, "secret-password"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.accessToken");

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void meReturns401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturns401WithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer bu-gecerli-bir-token-degil"))
                .andExpect(status().isUnauthorized());
    }

    private static String randomEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private static RequestBuilder register(String email, String password) {
        return post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password));
    }

    private static RequestBuilder login(String email, String password) {
        return post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password));
    }
}
