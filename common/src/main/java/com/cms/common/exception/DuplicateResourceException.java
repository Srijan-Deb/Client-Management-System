package com.cms.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an attempt is made to create a resource that already exists
 * (e.g. duplicate email, duplicate invoice number).
 * Maps to HTTP 409 Conflict.
 * <p>
 * Usage examples:
 * <pre>
 *     throw new DuplicateResourceException("DUPLICATE_EMAIL",
 *         "A client with email '" + email + "' already exists");
 * </pre>
 */
public class DuplicateResourceException extends CmsException {

    public DuplicateResourceException(String errorCode, String message) {
        super(HttpStatus.CONFLICT, errorCode, message);
    }

    public DuplicateResourceException(String errorCode, String message, Throwable cause) {
        super(HttpStatus.CONFLICT, errorCode, message, cause);
    }
}
