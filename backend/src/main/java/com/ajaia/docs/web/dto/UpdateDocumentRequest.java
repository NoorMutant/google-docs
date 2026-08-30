package com.ajaia.docs.web.dto;

import jakarta.validation.constraints.Size;

/**
 * Both fields are optional. The editor sends the title on rename and the
 * content on autosave, so a request usually carries only one of them.
 */
public record UpdateDocumentRequest(
        @Size(max = 200, message = "Title cannot be longer than 200 characters") String title,
        @Size(max = 500_000, message = "Document content is too large") String contentHtml) {
}
