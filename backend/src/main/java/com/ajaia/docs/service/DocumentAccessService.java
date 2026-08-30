package com.ajaia.docs.service;

import com.ajaia.docs.domain.AccessLevel;
import com.ajaia.docs.domain.AppUser;
import com.ajaia.docs.domain.Document;
import com.ajaia.docs.domain.ShareRole;
import com.ajaia.docs.repo.DocumentRepository;
import com.ajaia.docs.repo.DocumentShareRepository;
import com.ajaia.docs.web.ForbiddenException;
import com.ajaia.docs.web.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * The single place that decides what a user may do with a document.
 * Controllers never compare owner ids themselves, they ask here, so the rule
 * cannot drift between endpoints.
 */
@Service
@Transactional(readOnly = true)
public class DocumentAccessService {

    private final DocumentRepository documents;
    private final DocumentShareRepository shares;

    public DocumentAccessService(DocumentRepository documents, DocumentShareRepository shares) {
        this.documents = documents;
        this.shares = shares;
    }

    public AccessLevel accessFor(AppUser user, Document document) {
        if (Objects.equals(document.getOwner().getId(), user.getId())) {
            return AccessLevel.OWNER;
        }
        return shares.findByDocumentAndUser(document, user)
                .map(share -> share.getRole() == ShareRole.EDITOR ? AccessLevel.EDITOR : AccessLevel.VIEWER)
                .orElse(AccessLevel.NONE);
    }

    public Document requireReadable(Long documentId, AppUser user) {
        Document document = load(documentId);
        if (!accessFor(user, document).canRead()) {
            throw new NotFoundException("Document not found");
        }
        return document;
    }

    public Document requireWritable(Long documentId, AppUser user) {
        Document document = load(documentId);
        AccessLevel access = accessFor(user, document);
        if (!access.canRead()) {
            throw new NotFoundException("Document not found");
        }
        if (!access.canWrite()) {
            throw new ForbiddenException("You have view only access to this document");
        }
        return document;
    }

    public Document requireOwner(Long documentId, AppUser user) {
        Document document = load(documentId);
        AccessLevel access = accessFor(user, document);
        if (!access.canRead()) {
            throw new NotFoundException("Document not found");
        }
        if (!access.canManage()) {
            throw new ForbiddenException("Only the document owner can do this");
        }
        return document;
    }

    /**
     * The owner is joined in rather than left lazy, because every caller reads
     * the owner straight afterwards to decide access or to build a response.
     */
    private Document load(Long documentId) {
        return documents.findByIdWithOwner(documentId)
                .orElseThrow(() -> new NotFoundException("Document not found"));
    }
}
