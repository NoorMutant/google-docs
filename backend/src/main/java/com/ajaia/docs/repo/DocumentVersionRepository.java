package com.ajaia.docs.repo;

import com.ajaia.docs.domain.Document;
import com.ajaia.docs.domain.DocumentVersion;
import com.ajaia.docs.repo.projection.VersionListRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {

    /**
     * The panel lists versions without their contents. Selecting whole entities
     * here would load every historical copy of the document just to draw a list
     * of dates.
     */
    @Query("""
            select new com.ajaia.docs.repo.projection.VersionListRow(
                v.id, v.versionNumber, v.title, u.id, u.email, u.displayName,
                v.savedAt, v.restoredFromVersion)
            from DocumentVersion v
            join v.savedBy u
            where v.document = :document
            order by v.versionNumber desc
            """)
    List<VersionListRow> findRowsFor(@Param("document") Document document);

    Optional<DocumentVersion> findFirstByDocumentOrderByVersionNumberDesc(Document document);

    List<DocumentVersion> findByDocumentOrderByVersionNumberDesc(Document document);

    void deleteByDocument(Document document);
}
