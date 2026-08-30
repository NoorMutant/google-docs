package com.ajaia.docs.service;

import com.ajaia.docs.domain.AppUser;
import com.ajaia.docs.domain.Document;
import com.ajaia.docs.domain.DocumentVersion;
import com.ajaia.docs.repo.DocumentVersionRepository;
import com.ajaia.docs.repo.projection.VersionListRow;
import com.ajaia.docs.web.NotFoundException;
import com.ajaia.docs.web.dto.VersionDetail;
import com.ajaia.docs.web.dto.VersionSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class VersionService {

    /**
     * Autosave fires every time typing pauses. Writing a version per save would
     * bury the useful history under hundreds of entries, so consecutive saves by
     * the same person inside this window update one entry instead of adding new
     * ones. This is the same idea as the way Google Docs groups edits.
     */
    static final Duration COALESCE_WINDOW = Duration.ofMinutes(2);

    private final DocumentVersionRepository versions;

    public VersionService(DocumentVersionRepository versions) {
        this.versions = versions;
    }

    /** Called after a document is created or imported. */
    public void recordInitial(Document document, AppUser author) {
        versions.save(new DocumentVersion(document, 1, author));
    }

    /**
     * Called after a document is saved. Either extends the version that is still
     * inside the window or starts a new one.
     */
    public void recordSave(Document document, AppUser author) {
        DocumentVersion latest = versions.findFirstByDocumentOrderByVersionNumberDesc(document).orElse(null);

        if (latest == null) {
            versions.save(new DocumentVersion(document, 1, author));
            return;
        }

        if (isInsideWindow(latest, author)) {
            latest.refreshFrom(document);
            versions.save(latest);
            return;
        }

        versions.save(new DocumentVersion(document, latest.getVersionNumber() + 1, author));
    }

    private boolean isInsideWindow(DocumentVersion latest, AppUser author) {
        boolean sameAuthor = Objects.equals(latest.getSavedBy().getId(), author.getId());
        boolean recent = Duration.between(latest.getSavedAt(), Instant.now()).compareTo(COALESCE_WINDOW) < 0;
        // A restore point is always kept as its own entry so the trail of who
        // rolled back what stays readable.
        return sameAuthor && recent && latest.getRestoredFromVersion() == null;
    }

    /**
     * A restore always gets its own entry, never coalesced into a nearby edit,
     * so the history reads as a clear trail of who rolled back to what.
     */
    public void recordRestore(Document document, AppUser author, int restoredFrom) {
        int next = versions.findFirstByDocumentOrderByVersionNumberDesc(document)
                .map(version -> version.getVersionNumber() + 1)
                .orElse(1);
        DocumentVersion version = new DocumentVersion(document, next, author);
        version.setRestoredFromVersion(restoredFrom);
        versions.save(version);
    }

    @Transactional(readOnly = true)
    public List<VersionSummary> list(Document document) {
        List<VersionListRow> rows = versions.findRowsFor(document);
        List<VersionSummary> result = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            // Newest first, so the first row is the live version.
            result.add(VersionSummary.of(rows.get(i), i == 0));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public VersionDetail get(Document document, Long versionId) {
        DocumentVersion version = versions.findById(versionId)
                .orElseThrow(() -> new NotFoundException("Version not found"));
        if (!Objects.equals(version.getDocument().getId(), document.getId())) {
            throw new NotFoundException("Version not found");
        }
        return VersionDetail.of(version);
    }

    @Transactional(readOnly = true)
    public DocumentVersion require(Document document, Long versionId) {
        DocumentVersion version = versions.findById(versionId)
                .orElseThrow(() -> new NotFoundException("Version not found"));
        if (!Objects.equals(version.getDocument().getId(), document.getId())) {
            throw new NotFoundException("Version not found");
        }
        return version;
    }

    public void deleteAllFor(Document document) {
        versions.deleteByDocument(document);
    }
}
