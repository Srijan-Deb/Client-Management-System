package com.cms.account.exception;

import com.cms.common.exception.DuplicateResourceException;
import com.cms.common.exception.CmsException;
import com.cms.common.exception.ResourceNotFoundException;
import com.cms.common.exception.ServiceUnavailableException;
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
 * Centralized error handler for account-service REST endpoints (Phase 8 â€” updated).
 *
 * <p>All error responses follow the same envelope:
 * <pre>
 * {
 *   "errorCode":  "ACCOUNT_NOT_FOUND",
 *   "message":    "Account not found with id: 42",
 *   "timestamp":  "2026-08-11T06:00:00Z"
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
        return ResponseEntity.status(ex.getStatus())
                .body(errorBody(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {} â€” {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateResourceException ex) {
        log.warn("Duplicate resource: {} â€” {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorBody(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleServiceUnavailable(ServiceUnavailableException ex) {
        log.error("Downstream service unavailable: {} â€” {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorBody(ex.getErrorCode(), ex.getMessage()));
    }

    /**
     * Handles {@code @Valid} bean validation failures.
     * Returns 400 with per-field error messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                        (a, b) -> a
                ));
        log.warn("Validation failed in account-service: {}", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "errorCode",   "VALIDATION_ERROR",
                "message",     "Request validation failed",
                "fieldErrors", fieldErrors,
                "timestamp",   Instant.now().toString()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Unhandled exception in account-service", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody("INTERNAL_ERROR", "An unexpected error occurred"));
    }

    private Map<String, Object> errorBody(String code, String message) {
        return Map.of(
                "errorCode", code,
                "message",   message,
                "timestamp", Instant.now().toString()
        );
    }
}
