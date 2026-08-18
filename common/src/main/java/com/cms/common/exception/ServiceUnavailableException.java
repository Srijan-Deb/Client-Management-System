package com.cms.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a downstream service (e.g. Account Service) is unreachable or returns
 * an error that prevents the current operation from completing.
 * Maps to HTTP 503 Service Unavailable.
 *
 * <p>When this exception is thrown inside a {@code @Transactional} method,
 * Spring rolls back the entire transaction â€” no partial state is persisted.
 */
public class ServiceUnavailableException extends CmsException {

    public ServiceUnavailableException(String errorCode, String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, errorCode, message);
    }

    public ServiceUnavailableException(String errorCode, String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, errorCode, message, cause);
    }
}
