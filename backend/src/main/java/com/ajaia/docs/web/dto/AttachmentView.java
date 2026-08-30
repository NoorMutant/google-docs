package com.ajaia.docs.web.dto;

import com.ajaia.docs.domain.Attachment;

import java.time.Instant;

public record AttachmentView(
        Long id,
        String filename,
        String contentType,
        long sizeBytes,
        Instant uploadedAt) {

    public static AttachmentView of(Attachment attachment) {
        return new AttachmentView(
                attachment.getId(),
                attachment.getFilename(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getUploadedAt());
    }
}
