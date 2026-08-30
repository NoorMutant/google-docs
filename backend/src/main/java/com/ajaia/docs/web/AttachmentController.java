package com.ajaia.docs.web;

import com.ajaia.docs.domain.Attachment;
import com.ajaia.docs.service.AttachmentService;
import com.ajaia.docs.service.CurrentUserService;
import com.ajaia.docs.web.dto.AttachmentView;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/documents/{documentId}/attachments")
public class AttachmentController {

    private final AttachmentService attachments;
    private final CurrentUserService currentUser;

    public AttachmentController(AttachmentService attachments, CurrentUserService currentUser) {
        this.attachments = attachments;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<AttachmentView> list(@PathVariable Long documentId) {
        return attachments.list(documentId, currentUser.require());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentView upload(@PathVariable Long documentId, @RequestParam("file") MultipartFile file) {
        return attachments.add(documentId, file, currentUser.require());
    }

    @GetMapping("/{attachmentId}")
    public ResponseEntity<Resource> download(@PathVariable Long documentId, @PathVariable Long attachmentId) {
        Attachment attachment = attachments.download(documentId, attachmentId, currentUser.require());

        // Attachments are user supplied, so they are always sent as a download
        // rather than rendered inline in the browser.
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(attachment.getFilename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(attachment.getSizeBytes())
                .body(new ByteArrayResource(attachment.getData()));
    }

    @DeleteMapping("/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long documentId, @PathVariable Long attachmentId) {
        attachments.delete(documentId, attachmentId, currentUser.require());
    }
}
