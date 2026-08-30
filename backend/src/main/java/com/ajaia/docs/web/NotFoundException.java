package com.ajaia.docs.web;

/**
 * Thrown when a resource does not exist, and also when the current user has no
 * access to it at all. Both cases return 404 so that the API does not reveal
 * which document ids are in use.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
