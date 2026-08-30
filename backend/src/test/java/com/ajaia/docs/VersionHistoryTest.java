package com.ajaia.docs;

import com.ajaia.docs.domain.AppUser;
import com.ajaia.docs.domain.Document;
import com.ajaia.docs.domain.DocumentShare;
import com.ajaia.docs.domain.ShareRole;
import com.ajaia.docs.repo.AppUserRepository;
import com.ajaia.docs.repo.DocumentRepository;
import com.ajaia.docs.repo.DocumentShareRepository;
import com.ajaia.docs.repo.DocumentVersionRepository;
import com.ajaia.docs.service.DocumentService;
import com.ajaia.docs.web.dto.UpdateDocumentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VersionHistoryTest {

    private static final String OWNER = "vowner@test.local";
    private static final String EDITOR = "veditor@test.local";
    private static final String VIEWER = "vviewer@test.local";
    private static final String STRANGER = "vstranger@test.local";

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
    private DocumentService documentService;

    @Autowired
    private PasswordEncoder encoder;

    private Long documentId;
    private AppUser owner;

    @BeforeEach
    void setUp() {
        versions.deleteAll();
        shares.deleteAll();
        documents.deleteAll();

        owner = user(OWNER, "Version Owner");
        AppUser editor = user(EDITOR, "Version Editor");
        AppUser viewer = user(VIEWER, "Version Viewer");
        user(STRANGER, "Version Stranger");

        documentId = documentService.create("Release plan", "<p>First draft</p>", owner).id();

        Document document = documents.findById(documentId).orElseThrow();
        shares.save(new DocumentShare(document, editor, ShareRole.EDITOR));
        shares.save(new DocumentShare(document, viewer, ShareRole.VIEWER));
    }

    private AppUser user(String email, String name) {
        return users.findByEmailIgnoreCase(email)
                .orElseGet(() -> users.save(new AppUser(email, name, encoder.encode("demo123"))));
    }

    @Test
    @WithMockUser(username = OWNER)
    void creatingADocumentRecordsAFirstVersion() throws Exception {
        mockMvc.perform(get("/api/documents/" + documentId + "/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].versionNumber").value(1))
                .andExpect(jsonPath("$[0].current").value(true))
                .andExpect(jsonPath("$[0].savedBy.email").value(OWNER));
    }

    @Test
    @WithMockUser(username = OWNER)
    void rapidSavesByTheSamePersonCollapseIntoOneVersion() {
        // Autosave fires on every typing pause. Without coalescing this would be
        // three entries in the history for one sitting.
        documentService.update(documentId, new UpdateDocumentRequest(null, "<p>Second</p>"), owner);
        documentService.update(documentId, new UpdateDocumentRequest(null, "<p>Third</p>"), owner);
        documentService.update(documentId, new UpdateDocumentRequest(null, "<p>Fourth</p>"), owner);

        Document document = documents.findById(documentId).orElseThrow();
        assertThat(versions.findByDocumentOrderByVersionNumberDesc(document)).hasSize(1);
        assertThat(versions.findFirstByDocumentOrderByVersionNumberDesc(document).orElseThrow().getContentHtml())
                .isEqualTo("<p>Fourth</p>");
    }

    @Test
    @WithMockUser(username = OWNER)
    void aDifferentPersonSavingStartsANewVersion() {
        AppUser editor = users.findByEmailIgnoreCase(EDITOR).orElseThrow();

        documentService.update(documentId, new UpdateDocumentRequest(null, "<p>Owner edit</p>"), owner);
        documentService.update(documentId, new UpdateDocumentRequest(null, "<p>Editor edit</p>"), editor);

        Document document = documents.findById(documentId).orElseThrow();
        assertThat(versions.findByDocumentOrderByVersionNumberDesc(document)).hasSize(2);
    }

    @Test
    @WithMockUser(username = OWNER)
    void savingUnchangedContentDoesNotCreateAVersion() {
        documentService.update(documentId, new UpdateDocumentRequest(null, "<p>First draft</p>"), owner);

        Document document = documents.findById(documentId).orElseThrow();
        assertThat(versions.findByDocumentOrderByVersionNumberDesc(document)).hasSize(1);
    }

    @Test
    @WithMockUser(username = OWNER)
    void restoringPutsBackOldContentAndKeepsTheHistory() throws Exception {
        Document document = documents.findById(documentId).orElseThrow();
        Long firstVersionId = versions.findFirstByDocumentOrderByVersionNumberDesc(document)
                .orElseThrow()
                .getId();

        AppUser editor = users.findByEmailIgnoreCase(EDITOR).orElseThrow();
        documentService.update(documentId, new UpdateDocumentRequest(null, "<p>Rewritten</p>"), editor);

        mockMvc.perform(post("/api/documents/" + documentId + "/versions/" + firstVersionId + "/restore")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentHtml").value("<p>First draft</p>"));

        // Nothing is deleted. The restore itself becomes the newest entry, so it
        // can be undone the same way.
        assertThat(versions.findByDocumentOrderByVersionNumberDesc(document)).hasSize(3);
        assertThat(versions.findFirstByDocumentOrderByVersionNumberDesc(document).orElseThrow()
                .getRestoredFromVersion()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = VIEWER)
    void aViewerCanReadTheHistoryButCannotRestore() throws Exception {
        Document document = documents.findById(documentId).orElseThrow();
        Long versionId = versions.findFirstByDocumentOrderByVersionNumberDesc(document).orElseThrow().getId();

        mockMvc.perform(get("/api/documents/" + documentId + "/versions"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/documents/" + documentId + "/versions/" + versionId + "/restore")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = STRANGER)
    void someoneWithoutAccessCannotSeeTheHistory() throws Exception {
        mockMvc.perform(get("/api/documents/" + documentId + "/versions"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = OWNER)
    void aVersionIdFromAnotherDocumentIsRejected() throws Exception {
        Long otherDocumentId = documentService.create("Other", "<p>Other</p>", owner).id();
        Document other = documents.findById(otherDocumentId).orElseThrow();
        Long foreignVersionId = versions.findFirstByDocumentOrderByVersionNumberDesc(other).orElseThrow().getId();

        mockMvc.perform(get("/api/documents/" + documentId + "/versions/" + foreignVersionId))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/documents/" + documentId + "/versions/" + foreignVersionId + "/restore")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = OWNER)
    void deletingADocumentRemovesItsHistory() {
        documentService.delete(documentId, owner);

        assertThat(versions.count()).isZero();
    }
}
