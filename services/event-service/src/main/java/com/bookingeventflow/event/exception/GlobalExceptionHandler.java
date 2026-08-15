package com.bookingeventflow.event.exception;

import com.bookingeventflow.common.pagination.CursorDecodingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // =========================================================
    // DOMAIN EXCEPTIONS
    // =========================================================

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEventNotFound(
            EventNotFoundException ex,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidEventStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEventState(
            InvalidEventStateException ex,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request
        );
    }

    // =========================================================
    // PAGINATION
    // =========================================================

    /**
     * Handles invalid, malformed or corrupted pagination cursors.
     *
     * A cursor is client-provided input, therefore a decoding failure
     * is treated as a BAD_REQUEST rather than a server error.
     */
    @ExceptionHandler(CursorDecodingException.class)
    public ResponseEntity<ErrorResponse> handleCursorDecoding(
            CursorDecodingException ex,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                "Invalid pagination cursor.",
                request
        );
    }

    // =========================================================
    // CONCURRENCY
    // =========================================================

    /**
     * Handles optimistic locking conflicts.
     *
     * This occurs when another transaction has modified the same
     * event between the read and update operation.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(
            OptimisticLockingFailureException ex,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.CONFLICT,
                "The event was modified by another request. "
                        + "Please reload the event and try again.",
                request
        );
    }

    // =========================================================
    // REQUEST VALIDATION
    // =========================================================

    /**
     * Handles validation failures on @RequestBody objects.
     *
     * Example:
     *
     * {
     *     "name": ""
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, List<String>> errors =
                ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .collect(Collectors.groupingBy(
                                FieldError::getField,
                                Collectors.mapping(
                                        this::resolveValidationMessage,
                                        Collectors.toList()
                                )
                        ));

        return error(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request,
                errors
        );
    }

    /**
     * Handles validation failures on controller parameters.
     *
     * Example:
     *
     * @Min(1)
     * @Max(100)
     * int limit
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        Map<String, List<String>> errors =
                ex.getConstraintViolations()
                        .stream()
                        .collect(Collectors.groupingBy(
                                this::extractParameterName,
                                Collectors.mapping(
                                        ConstraintViolation::getMessage,
                                        Collectors.toList()
                                )
                        ));

        return error(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request,
                errors
        );
    }

    // =========================================================
    // REQUEST FORMAT
    // =========================================================

    /**
     * Handles malformed JSON and invalid JSON values.
     *
     * Examples:
     *
     * - malformed JSON
     * - invalid UUID
     * - invalid LocalDateTime
     * - invalid enum value
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                "Request body is invalid or malformed.",
                request
        );
    }

    /**
     * Handles invalid request parameter types.
     *
     * Example:
     *
     * GET /api/v1/events?limit=abc
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                "Invalid value for parameter '"
                        + ex.getName()
                        + "'.",
                request
        );
    }

    // =========================================================
    // DATABASE
    // =========================================================

    /**
     * Handles database constraint violations.
     *
     * The underlying database exception is intentionally not exposed
     * to the API client.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.CONFLICT,
                "The request could not be completed "
                        + "because it violates a data constraint.",
                request
        );
    }

    // =========================================================
    // UNEXPECTED ERRORS
    // =========================================================

    /**
     * Last-resort exception handler.
     *
     * Do not expose the underlying exception message to clients.
     * The exception should be logged by the application's
     * observability/logging layer.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error(
                "Unhandled exception while processing {} {}",
                request.getMethod(),
                request.getRequestURI(),
                ex
        );

        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.",
                request
        );
    }

    // =========================================================
    // RESPONSE FACTORY
    // =========================================================

    private ResponseEntity<ErrorResponse> error(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        return error(
                status,
                message,
                request,
                Map.of()
        );
    }

    private ResponseEntity<ErrorResponse> error(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, List<String>> errors
    ) {
        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                errors
        );

        return ResponseEntity
                .status(status)
                .body(response);
    }

    // =========================================================
    // VALIDATION HELPERS
    // =========================================================

    private String resolveValidationMessage(FieldError error) {
        return error.getDefaultMessage() != null
                ? error.getDefaultMessage()
                : "Invalid value";
    }

    private String extractParameterName(
            ConstraintViolation<?> violation
    ) {
        String path = violation.getPropertyPath().toString();

        int lastDot = path.lastIndexOf('.');

        return lastDot >= 0 && lastDot < path.length() - 1
                ? path.substring(lastDot + 1)
                : path;
    }
}