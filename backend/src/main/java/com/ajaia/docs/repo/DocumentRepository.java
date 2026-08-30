package com.ajaia.docs.repo;

import com.ajaia.docs.domain.AppUser;
import com.ajaia.docs.domain.Document;
import com.ajaia.docs.repo.projection.DocumentListRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * The dashboard shows a title, an owner and a timestamp. Selecting the whole
     * entity would pull every document's full HTML body across the wire to build
     * that, and would then need a second query per row to resolve the lazy owner.
     * These two queries fetch exactly the columns the list renders, in one round
     * trip each.
     */
    @Query("""
            select new com.ajaia.docs.repo.projection.DocumentListRow(
                d.id, d.title, o.id, o.email, o.displayName, d.updatedAt, null)
            from Document d
            join d.owner o
            where d.owner = :user
            order by d.updatedAt desc
            """)
    List<DocumentListRow> findOwnedRows(@Param("user") AppUser user);

    @Query("""
            select new com.ajaia.docs.repo.projection.DocumentListRow(
                d.id, d.title, o.id, o.email, o.displayName, d.updatedAt, s.role)
            from DocumentShare s
            join s.document d
            join d.owner o
            where s.user = :user
            order by d.updatedAt desc
            """)
    List<DocumentListRow> findSharedRows(@Param("user") AppUser user);

    /**
     * Used when a single document is opened. The owner is fetched in the same
     * query because every caller reads the owner name straight afterwards.
     */
    @Query("select d from Document d join fetch d.owner where d.id = :id")
    Optional<Document> findByIdWithOwner(@Param("id") Long id);
}
