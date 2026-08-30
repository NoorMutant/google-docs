package com.ajaia.docs.web.dto;

import jakarta.validation.constraints.Size;

public record CreateDocumentRequest(
        @Size(max = 200, message = "Title cannot be longer than 200 characters") String title) {
}
