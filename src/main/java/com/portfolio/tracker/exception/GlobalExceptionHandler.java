package com.portfolio.tracker.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized exception handling for the entire REST API.
 *
 * @RestControllerAdvice intercepts exceptions thrown by any @RestController
 * and converts them into structured JSON error responses.
 *
 * This avoids repeating try-catch blocks in every controller and ensures
 * ALL errors come back in the same JSON shape:
 *   {
 *     "timestamp": "...",
 *     "status":    404,
 *     "error":     "Not Found",
 *     "message":   "Client not found with id: 99"
 *   }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 404 Not Found ─────────────────────────────────────────────

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleClientNotFound(ClientNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AssetNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAssetNotFound(AssetNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // ── 400 Bad Request ───────────────────────────────────────────

    @ExceptionHandler(InsufficientHoldingsException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientHoldings(InsufficientHoldingsException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ClientInactiveException.class)
    public ResponseEntity<Map<String, Object>> handleClientInactive(ClientInactiveException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handles Bean Validation failures (@Valid on request DTOs).
     * Returns a map of field → error message so the caller knows exactly
     * which fields were invalid.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status",    HttpStatus.BAD_REQUEST.value());
        body.put("error",     "Validation Failed");
        body.put("fields",    fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    // ── 500 Internal Server Error (catch-all) ─────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR,
                          "An unexpected error occurred: " + ex.getMessage());
    }

    // ── Helper ────────────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> buildError(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status",    status.value());
        body.put("error",     status.getReasonPhrase());
        body.put("message",   message);
        return ResponseEntity.status(status).body(body);
    }
}
