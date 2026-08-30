package com.ajaia.docs.repo;

import com.ajaia.docs.domain.AppUser;
import com.ajaia.docs.domain.Document;
import com.ajaia.docs.domain.DocumentShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentShareRepository extends JpaRepository<DocumentShare, Long> {

    Optional<DocumentShare> findByDocumentAndUser(Document document, AppUser user);

    List<DocumentShare> findByDocument(Document document);

    void deleteByDocument(Document document);
}
