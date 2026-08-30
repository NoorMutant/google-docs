package com.ajaia.docs.web;

import com.ajaia.docs.domain.AppUser;
import com.ajaia.docs.domain.Document;
import com.ajaia.docs.service.CurrentUserService;
import com.ajaia.docs.service.DocumentAccessService;
import com.ajaia.docs.service.DocumentService;
import com.ajaia.docs.service.VersionService;
import com.ajaia.docs.web.dto.DocumentDetail;
import com.ajaia.docs.web.dto.VersionDetail;
import com.ajaia.docs.web.dto.VersionSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/documents/{documentId}/versions")
public class VersionController {

    private final VersionService versions;
    private final DocumentService documents;
    private final DocumentAccessService access;
    private final CurrentUserService currentUser;

    public VersionController(VersionService versions,
                             DocumentService documents,
                             DocumentAccessService access,
                             CurrentUserService currentUser) {
        this.versions = versions;
        this.documents = documents;
        this.access = access;
        this.currentUser = currentUser;
    }

    /** Anyone who can open the document can see its history. */
    @GetMapping
    public List<VersionSummary> list(@PathVariable Long documentId) {
        AppUser user = currentUser.require();
        Document document = access.requireReadable(documentId, user);
        return versions.list(document);
    }

    @GetMapping("/{versionId}")
    public VersionDetail get(@PathVariable Long documentId, @PathVariable Long versionId) {
        AppUser user = currentUser.require();
        Document document = access.requireReadable(documentId, user);
        return versions.get(document, versionId);
    }

    /** Restoring changes the document, so it needs write access, not just read. */
    @PostMapping("/{versionId}/restore")
    public DocumentDetail restore(@PathVariable Long documentId, @PathVariable Long versionId) {
        return documents.restoreVersion(documentId, versionId, currentUser.require());
    }
}
