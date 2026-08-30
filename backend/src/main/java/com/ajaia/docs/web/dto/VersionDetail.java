package com.ajaia.docs.web.dto;

import com.ajaia.docs.domain.DocumentVersion;

import java.time.Instant;

public record VersionDetail(
        Long id,
        int versionNumber,
        String title,
        String contentHtml,
        UserSummary savedBy,
        Instant savedAt) {

    public static VersionDetail of(DocumentVersion version) {
        return new VersionDetail(
                version.getId(),
                version.getVersionNumber(),
                version.getTitle(),
                version.getContentHtml(),
                UserSummary.of(version.getSavedBy()),
                version.getSavedAt());
    }
}
