package com.ajaia.docs.web;

/**
 * Thrown when the user can see a document but is not allowed to perform the
 * action, for example a viewer trying to save an edit.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
