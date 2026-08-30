package com.ajaia.docs.service;

import com.ajaia.docs.domain.Attachment;
import com.ajaia.docs.domain.AppUser;
import com.ajaia.docs.domain.Document;
import com.ajaia.docs.repo.AttachmentRepository;
import com.ajaia.docs.web.BadRequestException;
import com.ajaia.docs.web.NotFoundException;
import com.ajaia.docs.web.dto.AttachmentView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class AttachmentService {

    // Single source of truth, shared with the column length on the entity.
    public static final long MAX_ATTACHMENT_BYTES = Attachment.MAX_BYTES;

    private final AttachmentRepository attachments;
    private final DocumentAccessService access;

    public AttachmentService(AttachmentRepository attachments, DocumentAccessService access) {
        this.attachments = attachments;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<AttachmentView> list(Long documentId, AppUser user) {
        Document document = access.requireReadable(documentId, user);
        List<AttachmentView> result = new ArrayList<>();
        for (Attachment attachment : attachments.findByDocumentOrderByUploadedAtDesc(document)) {
            result.add(AttachmentView.of(attachment));
        }
        return result;
    }

    public AttachmentView add(Long documentId, MultipartFile file, AppUser user) {
        Document document = access.requireWritable(documentId, user);

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Pick a file to upload");
        }
        if (file.getSize() > MAX_ATTACHMENT_BYTES) {
            throw new BadRequestException("Attachments are limited to 5 MB");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("The file could not be read");
        }

        String contentType = StringUtils.hasText(file.getContentType())
                ? file.getContentType()
                : "application/octet-stream";

        Attachment attachment = new Attachment(document, safeFilename(file), contentType, bytes);
        attachments.save(attachment);
        return AttachmentView.of(attachment);
    }

    @Transactional(readOnly = true)
    public Attachment download(Long documentId, Long attachmentId, AppUser user) {
        Document document = access.requireReadable(documentId, user);
        Attachment attachment = attachments.findById(attachmentId)
                .orElseThrow(() -> new NotFoundException("Attachment not found"));
        // Guard against an attachment id from another document being passed in.
        if (!Objects.equals(attachment.getDocument().getId(), document.getId())) {
            throw new NotFoundException("Attachment not found");
        }
        return attachment;
    }

    public void delete(Long documentId, Long attachmentId, AppUser user) {
        access.requireWritable(documentId, user);
        Attachment attachment = download(documentId, attachmentId, user);
        attachments.delete(attachment);
    }

    /**
     * Browsers can send a full path as the filename. Only the last segment is
     * kept so nothing path shaped is ever stored or echoed back.
     */
    private String safeFilename(MultipartFile file) {
        String raw = file.getOriginalFilename();
        if (!StringUtils.hasText(raw)) {
            return "attachment";
        }
        String name = Paths.get(raw.replace('\\', '/')).getFileName().toString();
        return name.isBlank() ? "attachment" : name;
    }
}
