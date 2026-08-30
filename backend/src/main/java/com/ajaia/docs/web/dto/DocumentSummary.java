package com.ajaia.docs.web.dto;

import com.ajaia.docs.domain.AccessLevel;
import com.ajaia.docs.domain.ShareRole;
import com.ajaia.docs.repo.projection.DocumentListRow;

import java.time.Instant;

/**
 * Used for the dashboard lists. Content is left out on purpose, and the query
 * behind it does not select the content column either, so a long document costs
 * nothing extra to list.
 */
public record DocumentSummary(
        Long id,
        String title,
        UserSummary owner,
        AccessLevel access,
        Instant updatedAt) {

    /** A row with no share role is one the caller owns. */
    public static DocumentSummary of(DocumentListRow row) {
        return new DocumentSummary(
                row.id(),
                row.title(),
                new UserSummary(row.ownerId(), row.ownerEmail(), row.ownerDisplayName()),
                accessFrom(row.shareRole()),
                row.updatedAt());
    }

    private static AccessLevel accessFrom(ShareRole role) {
        if (role == null) {
            return AccessLevel.OWNER;
        }
        return role == ShareRole.EDITOR ? AccessLevel.EDITOR : AccessLevel.VIEWER;
    }
}
