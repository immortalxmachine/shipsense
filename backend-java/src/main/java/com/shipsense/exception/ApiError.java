package com.shipsense.exception;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Uniform JSON error body returned for every handled exception.
 *
 * @param timestamp   when the error occurred
 * @param status      HTTP status code
 * @param error       HTTP reason phrase
 * @param message     human-readable description
 * @param path        request URI that failed
 * @param fieldErrors field -> message map for validation failures (may be null)
 */
public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
}
