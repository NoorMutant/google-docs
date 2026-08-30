package com.ajaia.docs.repo;

import com.ajaia.docs.domain.Attachment;
import com.ajaia.docs.domain.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByDocumentOrderByUploadedAtDesc(Document document);

    void deleteByDocument(Document document);
}
