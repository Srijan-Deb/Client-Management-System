package com.cms.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when request payload or business-rule validation fails.
 * Maps to HTTP 422 Unprocessable Entity (preferred over 400 for
 * semantic validation failures, as the request was well-formed but
 * violates a business rule).
 * <p>
 * For simple structural validation failures (missing fields, wrong type),
 * Spring's default {@code MethodArgumentNotValidException} (400) is fine.
 * Use this exception for business-rule violations only.
 * <p>
 * Usage examples:
 * <pre>
 *     throw new ValidationException("INVALID_DATE_RANGE",
 *         "Contract end date must be after start date");
 *
 *     throw new ValidationException("NEGATIVE_AMOUNT",
 *         "Invoice amount must be greater than zero");
 * </pre>
 */
public class ValidationException extends CmsException {

    public ValidationException(String errorCode, String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, errorCode, message);
    }

    public ValidationException(String errorCode, String message, Throwable cause) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, errorCode, message, cause);
    }
}
