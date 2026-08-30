package com.ajaia.docs.repo.projection;

import java.time.Instant;

/**
 * The history panel lists versions without showing their contents, so the
 * bodies are only loaded when a specific version is opened.
 */
public record VersionListRow(
        Long id,
        int versionNumber,
        String title,
        Long savedById,
        String savedByEmail,
        String savedByDisplayName,
        Instant savedAt,
        Integer restoredFromVersion) {
}
