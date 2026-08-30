package com.ajaia.docs.domain;

/**
 * VIEWER can open and read a document.
 * EDITOR can also change the title and the content.
 * Only the owner can delete a document or change who it is shared with.
 */
public enum ShareRole {
    VIEWER,
    EDITOR
}
