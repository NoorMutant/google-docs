package com.ajaia.docs.service;

import com.ajaia.docs.domain.AppUser;
import com.ajaia.docs.domain.Document;
import com.ajaia.docs.domain.DocumentShare;
import com.ajaia.docs.domain.ShareRole;
import com.ajaia.docs.repo.AppUserRepository;
import com.ajaia.docs.repo.DocumentShareRepository;
import com.ajaia.docs.web.BadRequestException;
import com.ajaia.docs.web.NotFoundException;
import com.ajaia.docs.web.dto.ShareView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class ShareService {

    private final DocumentShareRepository shares;
    private final AppUserRepository users;
    private final DocumentAccessService access;

    public ShareService(DocumentShareRepository shares,
                        AppUserRepository users,
                        DocumentAccessService access) {
        this.shares = shares;
        this.users = users;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<ShareView> list(Long documentId, AppUser currentUser) {
        // Anyone who can open the document can see who else it is shared with.
        Document document = access.requireReadable(documentId, currentUser);
        List<ShareView> result = new ArrayList<>();
        for (DocumentShare share : shares.findByDocument(document)) {
            result.add(ShareView.of(share));
        }
        return result;
    }

    public ShareView share(Long documentId, String email, ShareRole role, AppUser currentUser) {
        Document document = access.requireOwner(documentId, currentUser);

        AppUser target = users.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new NotFoundException("No user is registered with " + email.trim()));

        if (Objects.equals(target.getId(), document.getOwner().getId())) {
            throw new BadRequestException("This document already belongs to that user");
        }

        // Sharing again with the same person changes their role instead of
        // creating a second row.
        DocumentShare share = shares.findByDocumentAndUser(document, target)
                .orElseGet(() -> new DocumentShare(document, target, role));
        share.setRole(role);
        shares.save(share);
        return ShareView.of(share);
    }

    public void unshare(Long documentId, Long userId, AppUser currentUser) {
        Document document = access.requireOwner(documentId, currentUser);
        AppUser target = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        DocumentShare share = shares.findByDocumentAndUser(document, target)
                .orElseThrow(() -> new NotFoundException("That user does not have access to this document"));
        shares.delete(share);
    }
}
