package com.cms.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested resource does not exist in the database.
 * Maps to HTTP 404 Not Found.
 * <p>
 * Usage examples:
 * <pre>
 *     throw new ResourceNotFoundException("CLIENT_NOT_FOUND",
 *         "Client not found with id: " + clientId);
 *
 *     throw new ResourceNotFoundException("INVOICE_NOT_FOUND",
 *         "Invoice " + invoiceNumber + " does not exist");
 * </pre>
 */
public class ResourceNotFoundException extends CmsException {

    public ResourceNotFoundException(String errorCode, String message) {
        super(HttpStatus.NOT_FOUND, errorCode, message);
    }

    public ResourceNotFoundException(String errorCode, String message, Throwable cause) {
        super(HttpStatus.NOT_FOUND, errorCode, message, cause);
    }
}
