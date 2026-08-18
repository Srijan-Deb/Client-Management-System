package com.cms.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base runtime exception for all CMS-specific errors.
 * <p>
 * Every domain exception should extend this class so that a single
 * {@code @ControllerAdvice} / {@code @RestControllerAdvice} can handle
 * all CMS errors uniformly and map them to the correct HTTP status code.
 * <p>
 * Design notes:
 * <ul>
 *   <li>Carries an {@link HttpStatus} so the global handler doesn't need
 *       a giant if-else chain to determine the response code.</li>
 *   <li>Extends {@link RuntimeException} â€” callers are not forced to
 *       declare checked exceptions in method signatures.</li>
 * </ul>
 */
public class CmsException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    /**
     * @param status    HTTP status that should be returned to the caller
     * @param errorCode Machine-readable error code (e.g. "CLIENT_NOT_FOUND")
     * @param message   Human-readable message
     */
    public CmsException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    /**
     * @param status    HTTP status
     * @param errorCode Machine-readable error code
     * @param message   Human-readable message
     * @param cause     Wrapped cause
     */
    public CmsException(HttpStatus status, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
