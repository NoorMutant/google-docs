package com.ajaia.docs.domain;

/**
 * What the current user is allowed to do with a document.
 * NONE means the user should not even be told the document exists.
 */
public enum AccessLevel {
    OWNER,
    EDITOR,
    VIEWER,
    NONE;

    public boolean canRead() {
        return this != NONE;
    }

    public boolean canWrite() {
        return this == OWNER || this == EDITOR;
    }

    public boolean canManage() {
        return this == OWNER;
    }
}
