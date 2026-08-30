package com.ajaia.docs;

import com.ajaia.docs.config.DataSeeder;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {

    private static final String SEEDED_EMAIL = "alice@ajaia.test";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void signingInWithASeededAccountReturnsTheUserAndKeepsTheSession() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(SEEDED_EMAIL, DataSeeder.DEMO_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(SEEDED_EMAIL))
                .andReturn();

        HttpSession session = login.getRequest().getSession(false);

        mockMvc.perform(get("/api/auth/me").session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Alice Bennett"));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(SEEDED_EMAIL, "not-the-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email or password is not correct"));
    }

    @Test
    void unknownEmailGetsTheSameMessageAsAWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("nobody@ajaia.test", DataSeeder.DEMO_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email or password is not correct"));
    }

    @Test
    void protectedEndpointsRequireASession() throws Exception {
        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isUnauthorized());
    }

    private String body(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }
}
