package com.ajaia.docs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A malformed request is the caller's mistake, not a server fault. Each of these
 * used to come back as a 500 because the catch all handler swallowed Spring's
 * own typed exceptions before they could be mapped.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "alice@ajaia.test")
    void aPathIdThatIsNotANumberIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/documents/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "alice@ajaia.test")
    void anUnknownApiPathIsNotFound() throws Exception {
        mockMvc.perform(get("/api/there-is-no-such-thing"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "alice@ajaia.test")
    void theWrongContentTypeIsUnsupportedMedia() throws Exception {
        mockMvc.perform(patch("/api/documents/1").with(csrf())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("not json"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @WithMockUser(username = "alice@ajaia.test")
    void theWrongHttpMethodIsMethodNotAllowed() throws Exception {
        mockMvc.perform(delete("/api/auth/me").with(csrf()))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @WithMockUser(username = "alice@ajaia.test")
    void bodyThatIsNotJsonIsABadRequest() throws Exception {
        mockMvc.perform(patch("/api/documents/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{this is not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The request body could not be read. Check that it is valid JSON"));
    }

    @Test
    @WithMockUser(username = "alice@ajaia.test")
    void aRoleOutsideTheEnumSaysWhichValuesAreAllowed() throws Exception {
        mockMvc.perform(post("/api/documents/1/shares").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bob@ajaia.test\",\"role\":\"SUPERUSER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("'SUPERUSER' is not valid here. Expected one of: VIEWER, EDITOR"));
    }

    @Test
    @WithMockUser(username = "alice@ajaia.test")
    void anImportWithNoUploadAtAllIsABadRequest() throws Exception {
        mockMvc.perform(post("/api/documents/import").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The upload could not be read. Try selecting the file again"));
    }

    @Test
    @WithMockUser(username = "alice@ajaia.test")
    void anUploadUnderTheWrongFieldNameSaysWhichFieldIsMissing() throws Exception {
        MockMultipartFile wrongName =
                new MockMultipartFile("document", "notes.txt", "text/plain", "hello".getBytes(UTF_8));

        mockMvc.perform(multipart("/api/documents/import").file(wrongName).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing required value: file"));
    }

    @Test
    void anAnonymousCallerIsUnauthorizedRatherThanRedirected() throws Exception {
        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Please sign in"));
    }
}
