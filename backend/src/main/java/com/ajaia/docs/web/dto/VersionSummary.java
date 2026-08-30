package com.ajaia.docs.web.dto;

import com.ajaia.docs.repo.projection.VersionListRow;

import java.time.Instant;

public record VersionSummary(
        Long id,
        int versionNumber,
        String title,
        UserSummary savedBy,
        Instant savedAt,
        Integer restoredFromVersion,
        boolean current) {

    public static VersionSummary of(VersionListRow row, boolean current) {
        return new VersionSummary(
                row.id(),
                row.versionNumber(),
                row.title(),
                new UserSummary(row.savedById(), row.savedByEmail(), row.savedByDisplayName()),
                row.savedAt(),
                row.restoredFromVersion(),
                current);
    }
}
