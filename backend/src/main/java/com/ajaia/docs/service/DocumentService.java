package com.ajaia.docs.service;

import com.ajaia.docs.domain.AccessLevel;
import com.ajaia.docs.domain.AppUser;
import com.ajaia.docs.domain.Document;
import com.ajaia.docs.domain.DocumentVersion;
import com.ajaia.docs.repo.AttachmentRepository;
import com.ajaia.docs.repo.DocumentRepository;
import com.ajaia.docs.repo.DocumentShareRepository;
import com.ajaia.docs.web.dto.DocumentDetail;
import com.ajaia.docs.web.dto.DocumentSummary;
import com.ajaia.docs.web.dto.UpdateDocumentRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class DocumentService {

    static final String DEFAULT_TITLE = "Untitled document";

    private final DocumentRepository documents;
    private final DocumentShareRepository shares;
    private final AttachmentRepository attachments;
    private final DocumentAccessService access;
    private final HtmlSanitizer sanitizer;
    private final VersionService versions;

    public DocumentService(DocumentRepository documents,
                           DocumentShareRepository shares,
                           AttachmentRepository attachments,
                           DocumentAccessService access,
                           HtmlSanitizer sanitizer,
                           VersionService versions) {
        this.documents = documents;
        this.shares = shares;
        this.attachments = attachments;
        this.access = access;
        this.sanitizer = sanitizer;
        this.versions = versions;
    }

    @Transactional(readOnly = true)
    public List<DocumentSummary> listOwned(AppUser user) {
        return documents.findOwnedRows(user).stream().map(DocumentSummary::of).toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentSummary> listSharedWith(AppUser user) {
        // The role comes back with the row, so there is no per document lookup.
        return documents.findSharedRows(user).stream().map(DocumentSummary::of).toList();
    }

    @Transactional(readOnly = true)
    public DocumentDetail get(Long documentId, AppUser user) {
        Document document = access.requireReadable(documentId, user);
        return DocumentDetail.of(document, access.accessFor(user, document));
    }

    public DocumentDetail create(String title, String contentHtml, AppUser owner) {
        String safeTitle = StringUtils.hasText(title) ? title.trim() : DEFAULT_TITLE;
        Document document = new Document(safeTitle, sanitizer.clean(contentHtml), owner);
        documents.save(document);
        versions.recordInitial(document, owner);
        return DocumentDetail.of(document, AccessLevel.OWNER);
    }

    public DocumentDetail update(Long documentId, UpdateDocumentRequest request, AppUser user) {
        Document document = access.requireWritable(documentId, user);

        boolean changed = false;

        if (request.title() != null) {
            String trimmed = request.title().trim();
            // An empty title box should not leave the document nameless.
            String next = trimmed.isEmpty() ? DEFAULT_TITLE : trimmed;
            if (!Objects.equals(next, document.getTitle())) {
                document.setTitle(next);
                changed = true;
            }
        }
        if (request.contentHtml() != null) {
            String next = sanitizer.clean(request.contentHtml());
            if (!Objects.equals(next, document.getContentHtml())) {
                document.setContentHtml(next);
                changed = true;
            }
        }

        if (changed) {
            // Explicit save so the updated timestamp is written even when only
            // one field changed inside the same transaction.
            documents.save(document);
            versions.recordSave(document, user);
        }

        return DocumentDetail.of(document, access.accessFor(user, document));
    }

    /**
     * Writes an older version back onto the document. Nothing is deleted, the
     * restore itself becomes the newest version, so it can be undone the same way.
     */
    public DocumentDetail restoreVersion(Long documentId, Long versionId, AppUser user) {
        Document document = access.requireWritable(documentId, user);
        DocumentVersion version = versions.require(document, versionId);

        document.setTitle(version.getTitle());
        document.setContentHtml(version.getContentHtml());
        documents.save(document);

        versions.recordRestore(document, user, version.getVersionNumber());

        return DocumentDetail.of(document, access.accessFor(user, document));
    }

    public void delete(Long documentId, AppUser user) {
        Document document = access.requireOwner(documentId, user);
        versions.deleteAllFor(document);
        attachments.deleteByDocument(document);
        shares.deleteByDocument(document);
        documents.delete(document);
    }
}
