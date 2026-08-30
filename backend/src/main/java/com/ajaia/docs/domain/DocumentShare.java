package com.ajaia.docs.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "document_share",
        uniqueConstraints = @UniqueConstraint(columnNames = {"document_id", "user_id"}))
public class DocumentShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ShareRole role;

    protected DocumentShare() {
    }

    public DocumentShare(Document document, AppUser user, ShareRole role) {
        this.document = document;
        this.user = user;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public Document getDocument() {
        return document;
    }

    public AppUser getUser() {
        return user;
    }

    public ShareRole getRole() {
        return role;
    }

    public void setRole(ShareRole role) {
        this.role = role;
    }
}
