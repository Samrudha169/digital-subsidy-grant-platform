package com.dsgp.config;

import com.dsgp.beneficiary.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for the DSGP platform REST API.
 *
 * <p>Centralises all exception-to-HTTP-response mapping using
 * {@code @RestControllerAdvice}. Returns a consistent
 * {@link ApiErrorResponse} body for every error case.
 *
 * <p>As new modules are implemented, their module-specific exceptions
 * should be added here to maintain a single, consistent error contract
 * across the entire API.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── Beneficiary Module Exceptions ─────────────────────────────────────────

    @ExceptionHandler(BeneficiaryNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleBeneficiaryNotFound(
            BeneficiaryNotFoundException ex, HttpServletRequest request) {
        log.warn("Beneficiary not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(build(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), null));
    }

    @ExceptionHandler(DuplicateAadhaarException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateAadhaar(
            DuplicateAadhaarException ex, HttpServletRequest request) {
        log.warn("Duplicate Aadhaar registration attempt: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(build(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), null));
    }

    @ExceptionHandler(DuplicateMobileException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateMobile(
            DuplicateMobileException ex, HttpServletRequest request) {
        log.warn("Duplicate mobile registration attempt: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(build(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), null));
    }

    @ExceptionHandler(DocumentUploadException.class)
    public ResponseEntity<ApiErrorResponse> handleDocumentUpload(
            DocumentUploadException ex, HttpServletRequest request) {
        // If the exception wraps an IOException it is a filesystem failure → 500;
        // otherwise it is a bad-request condition (empty file, wrong MIME type) → 400.
        if (ex.getCause() instanceof java.io.IOException) {
            log.error("Document upload I/O error: {}", ex.getMessage(), ex.getCause());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(),
                            request.getRequestURI(), null));
        }
        log.warn("Document upload validation error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(build(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null));
    }

    @ExceptionHandler(InvalidDocumentTypeException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidDocumentType(
            InvalidDocumentTypeException ex, HttpServletRequest request) {
        log.warn("Invalid document type: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(build(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null));
    }

    @ExceptionHandler(DuplicateDocumentTypeException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateDocumentType(
            DuplicateDocumentTypeException ex, HttpServletRequest request) {
        log.warn("Duplicate document type upload attempt: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(build(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), null));
    }

    // ── Validation Exceptions ─────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> ApiErrorResponse.FieldError.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());

        log.warn("Validation failed for request to {}: {} field errors",
                request.getRequestURI(), fieldErrors.size());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(build(HttpStatus.BAD_REQUEST, "Validation failed",
                        request.getRequestURI(), fieldErrors));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("File upload size exceeded: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(build(HttpStatus.PAYLOAD_TOO_LARGE,
                        "File size exceeds the maximum allowed limit of 5MB",
                        request.getRequestURI(), null));
    }

    /**
     * Handles invalid path-variable or request-parameter type conversions
     * (e.g., an unrecognised enum value such as {@code /status/UNKNOWN}).
     * Returns 400 Bad Request.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = String.format("Invalid value '%s' for parameter '%s'",
                ex.getValue(), ex.getName());
        if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
            message += ". Allowed values: "
                    + java.util.Arrays.toString(ex.getRequiredType().getEnumConstants());
        }
        log.warn("Type mismatch at {}: {}", request.getRequestURI(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(build(HttpStatus.BAD_REQUEST, message, request.getRequestURI(), null));
    }

    // ── Catch-All ─────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneral(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(build(HttpStatus.INTERNAL_SERVER_ERROR,
                        "An unexpected error occurred. Please contact support.",
                        request.getRequestURI(), null));
    }

    // ── Builder Helper ────────────────────────────────────────────────────────

    private ApiErrorResponse build(HttpStatus status, String message, String path,
                                   List<ApiErrorResponse.FieldError> fieldErrors) {
        return ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .fieldErrors(fieldErrors)
                .build();
    }
}
