package com.ajaia.docs;

import com.ajaia.docs.domain.AppUser;
import com.ajaia.docs.domain.Document;
import com.ajaia.docs.domain.DocumentShare;
import com.ajaia.docs.domain.ShareRole;
import com.ajaia.docs.repo.AppUserRepository;
import com.ajaia.docs.repo.DocumentRepository;
import com.ajaia.docs.repo.DocumentShareRepository;
import com.ajaia.docs.repo.DocumentVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The sharing rules are the part of this app that is easiest to get wrong, so
 * they are covered end to end through the real HTTP layer.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DocumentAccessTest {

    private static final String OWNER = "owner@test.local";
    private static final String EDITOR = "editor@test.local";
    private static final String VIEWER = "viewer@test.local";
    private static final String STRANGER = "stranger@test.local";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private DocumentRepository documents;

    @Autowired
    private DocumentShareRepository shares;

    @Autowired
    private DocumentVersionRepository versions;

    @Autowired
    private PasswordEncoder encoder;

    private Long documentId;

    @BeforeEach
    void setUp() {
        // Versions and shares point at documents, so they go first.
        versions.deleteAll();
        shares.deleteAll();
        documents.deleteAll();

        AppUser owner = user(OWNER, "Olivia Owner");
        AppUser editor = user(EDITOR, "Eli Editor");
        AppUser viewer = user(VIEWER, "Vera Viewer");
        user(STRANGER, "Sam Stranger");

        Document document = documents.save(new Document("Team charter", "<p>Draft</p>", owner));
        shares.save(new DocumentShare(document, editor, ShareRole.EDITOR));
        shares.save(new DocumentShare(document, viewer, ShareRole.VIEWER));
        documentId = document.getId();
    }

    private AppUser user(String email, String name) {
        return users.findByEmailIgnoreCase(email)
                .orElseGet(() -> users.save(new AppUser(email, name, encoder.encode("demo123"))));
    }

    @Test
    @WithMockUser(username = OWNER)
    void ownerCanReadAndEdit() throws Exception {
        mockMvc.perform(get("/api/documents/" + documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access").value("OWNER"))
                .andExpect(jsonPath("$.title").value("Team charter"));

        mockMvc.perform(patch("/api/documents/" + documentId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Team charter v2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Team charter v2"));
    }

    @Test
    @WithMockUser(username = VIEWER)
    void viewerCanReadButNotEdit() throws Exception {
        mockMvc.perform(get("/api/documents/" + documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access").value("VIEWER"));

        mockMvc.perform(patch("/api/documents/" + documentId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentHtml\":\"<p>Sneaky edit</p>\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = EDITOR)
    void editorCanEditButCannotDeleteOrReshare() throws Exception {
        mockMvc.perform(patch("/api/documents/" + documentId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentHtml\":\"<p>Updated by the editor</p>\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentHtml").value("<p>Updated by the editor</p>"));

        mockMvc.perform(delete("/api/documents/" + documentId).with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/documents/" + documentId + "/shares").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + STRANGER + "\",\"role\":\"VIEWER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = STRANGER)
    void unrelatedUserGetsNotFoundRatherThanForbidden() throws Exception {
        // 404 instead of 403 so the API does not confirm that the document exists.
        mockMvc.perform(get("/api/documents/" + documentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = OWNER)
    void sharingPutsTheDocumentInTheOtherUserSharedList() throws Exception {
        mockMvc.perform(post("/api/documents/" + documentId + "/shares").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + STRANGER + "\",\"role\":\"VIEWER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("VIEWER"));

        mockMvc.perform(get("/api/documents").with(asUser(STRANGER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owned").isEmpty())
                .andExpect(jsonPath("$.sharedWithMe[0].id").value(documentId.intValue()))
                .andExpect(jsonPath("$.sharedWithMe[0].access").value("VIEWER"));
    }

    private static RequestPostProcessor asUser(String email) {
        return SecurityMockMvcRequestPostProcessors.user(email);
    }
}
