package com.ajaia.docs.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "attachment")
public class Attachment {

    /** Matches the limit enforced in AttachmentService. */
    public static final int MAX_BYTES = 5 * 1024 * 1024;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    /**
     * Files are small and capped at 5 MB, so keeping the bytes in the row avoids
     * needing object storage for this exercise.
     *
     * Deliberately not @Lob. On Postgres, Hibernate maps a @Lob byte[] to an oid,
     * which stores the bytes in pg_largeobject rather than in the row. Deleting
     * the row then leaves the object behind forever, because Postgres does not
     * cascade large object deletion. Declaring an explicit length gives bytea on
     * Postgres and VARBINARY on H2, both of which are deleted with the row.
     */
    @Column(nullable = false, length = MAX_BYTES)
    private byte[] data;

    @Column(nullable = false)
    private Instant uploadedAt = Instant.now();

    protected Attachment() {
    }

    public Attachment(Document document, String filename, String contentType, byte[] data) {
        this.document = document;
        this.filename = filename;
        this.contentType = contentType;
        this.data = data;
        this.sizeBytes = data.length;
    }

    public Long getId() {
        return id;
    }

    public Document getDocument() {
        return document;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public byte[] getData() {
        return data;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
