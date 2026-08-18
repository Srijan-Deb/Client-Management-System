package com.cms.client.exception;

import com.cms.common.exception.CmsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralised exception-to-HTTP-response mapper for client-service.
 *
 * <p>Error response envelope:
 * <pre>
 * {
 *   "errorCode": "DUPLICATE_EMAIL",
 *   "message":   "A client with email '...' already exists",
 *   "timestamp": "2025-01-01T00:00:00Z"
 * }
 * </pre>
 *
 * <p>Validation errors additionally include a {@code fieldErrors} map:
 * <pre>
 * {
 *   "errorCode": "VALIDATION_ERROR",
 *   "message":   "Request validation failed",
 *   "fieldErrors": { "email": "must be a valid email" },
 *   "timestamp": "..."
 * }
 * </pre>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles all CMS domain exceptions (404, 409, 503, etc.).
     * The HTTP status is carried by the exception itself â€” no switch-case needed.
     */
    @ExceptionHandler(CmsException.class)
    public ResponseEntity<Map<String, Object>> handleCmsException(CmsException ex) {
        log.warn("[{}] {} â€” {}", ex.getStatus().value(), ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(Map.of(
                "errorCode", ex.getErrorCode(),
                "message",   ex.getMessage(),
                "timestamp", Instant.now().toString()
        ));
    }

    /**
     * Handles {@code @Valid} / {@code @Validated} bean validation failures.
     * Returns 400 with per-field error messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                        (a, b) -> a   // keep first message on duplicate field
                ));

        log.warn("Validation failed: {}", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "errorCode",   "VALIDATION_ERROR",
                "message",     "Request validation failed",
                "fieldErrors", fieldErrors,
                "timestamp",   Instant.now().toString()
        ));
    }

    /** Catch-all for unexpected runtime exceptions â€” returns 500. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "errorCode", "INTERNAL_ERROR",
                "message",   "An unexpected error occurred. Please contact support.",
                "timestamp", Instant.now().toString()
        ));
    }
}
