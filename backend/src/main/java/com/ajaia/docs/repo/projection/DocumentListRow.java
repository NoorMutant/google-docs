package com.ajaia.docs.repo.projection;

import com.ajaia.docs.domain.ShareRole;

import java.time.Instant;

/**
 * Exactly the columns the dashboard renders. Keeping this separate from the
 * entity is what stops a list of documents from dragging every document body
 * out of the database.
 *
 * shareRole is null for documents the caller owns.
 */
public record DocumentListRow(
        Long id,
        String title,
        Long ownerId,
        String ownerEmail,
        String ownerDisplayName,
        Instant updatedAt,
        ShareRole shareRole) {
}
