package com.ajaia.docs.web.dto;

import com.ajaia.docs.domain.AccessLevel;
import com.ajaia.docs.domain.Document;

import java.time.Instant;

public record DocumentDetail(
        Long id,
        String title,
        String contentHtml,
        UserSummary owner,
        AccessLevel access,
        Instant createdAt,
        Instant updatedAt) {

    public static DocumentDetail of(Document document, AccessLevel access) {
        return new DocumentDetail(
                document.getId(),
                document.getTitle(),
                document.getContentHtml(),
                UserSummary.of(document.getOwner()),
                access,
                document.getCreatedAt(),
                document.getUpdatedAt());
    }
}
