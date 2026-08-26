package com.dsgp.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standard error response body returned by the DSGP platform for all
 * API error conditions.
 *
 * <p>Used by {@link GlobalExceptionHandler} for every exception type.
 * Fields with {@code null} values are excluded from the JSON response
 * via {@code @JsonInclude(NON_NULL)}.
 *
 * <p>Example response:
 * <pre>{@code
 * {
 *   "timestamp": "2026-08-24T23:00:00",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Validation failed",
 *   "path": "/api/v1/beneficiaries",
 *   "fieldErrors": [
 *     { "field": "aadhaarNumber", "message": "must match \\d{12}" }
 *   ]
 * }
 * }</pre>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

    /** Present only for validation errors ({@code 400}) with field-level details. */
    private List<FieldError> fieldErrors;

    /**
     * Individual field-level validation error detail.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldError {
        private String field;
        private String message;
    }
}

