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

/**
 * A snapshot of a document as it stood after a save.
 *
 * Versions are append only. Restoring an old version does not delete anything,
 * it writes that content back onto the document, which produces a new version
 * in turn. That way you can always undo a restore.
 */
@Entity
@Table(name = "document_version")
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(nullable = false)
    private int versionNumber;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contentHtml;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "saved_by_id", nullable = false)
    private AppUser savedBy;

    @Column(nullable = false)
    private Instant savedAt = Instant.now();

    /** Set when the version came from restoring an earlier one. */
    private Integer restoredFromVersion;

    protected DocumentVersion() {
    }

    public DocumentVersion(Document document, int versionNumber, AppUser savedBy) {
        this.document = document;
        this.versionNumber = versionNumber;
        this.savedBy = savedBy;
        this.title = document.getTitle();
        this.contentHtml = document.getContentHtml();
    }

    /**
     * Used when a save lands inside the coalescing window, so a burst of typing
     * becomes one entry in the history instead of dozens.
     */
    public void refreshFrom(Document document) {
        this.title = document.getTitle();
        this.contentHtml = document.getContentHtml();
        this.savedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Document getDocument() {
        return document;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getContentHtml() {
        return contentHtml;
    }

    public AppUser getSavedBy() {
        return savedBy;
    }

    public Instant getSavedAt() {
        return savedAt;
    }

    public Integer getRestoredFromVersion() {
        return restoredFromVersion;
    }

    public void setRestoredFromVersion(Integer restoredFromVersion) {
        this.restoredFromVersion = restoredFromVersion;
    }
}
