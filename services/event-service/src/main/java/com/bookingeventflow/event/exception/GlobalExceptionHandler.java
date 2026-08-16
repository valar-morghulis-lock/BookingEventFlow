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
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
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
            EventNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidEventStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEventState(
            InvalidEventStateException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request
        );
    }

    // =========================================================
    // PAGINATION
    // =========================================================

    /**
     * Client-provided cursor is malformed, corrupted, or otherwise
     * cannot be decoded.
     */
    @ExceptionHandler(CursorDecodingException.class)
    public ResponseEntity<ErrorResponse> handleCursorDecoding(
            CursorDecodingException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid pagination cursor.",
                request
        );
    }

    // =========================================================
    // CONCURRENCY
    // =========================================================

    /**
     * Another transaction modified the same event before the current
     * transaction could complete its update.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(
            OptimisticLockingFailureException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
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
     * Handles Bean Validation failures on @RequestBody objects.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, List<String>> validationErrors =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .collect(Collectors.groupingBy(
                                FieldError::getField,
                                Collectors.mapping(
                                        this::resolveValidationMessage,
                                        Collectors.toList()
                                )
                        ));

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request,
                validationErrors
        );
    }

    /**
     * Handles Bean Validation failures on controller parameters.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        Map<String, List<String>> validationErrors =
                exception.getConstraintViolations()
                        .stream()
                        .collect(Collectors.groupingBy(
                                this::extractParameterName,
                                Collectors.mapping(
                                        ConstraintViolation::getMessage,
                                        Collectors.toList()
                                )
                        ));

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request,
                validationErrors
        );
    }

    // =========================================================
    // REQUEST FORMAT
    // =========================================================

    /**
     * Handles malformed JSON and invalid JSON values.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
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
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid value for parameter '"
                        + exception.getName()
                        + "'.",
                request
        );
    }

    // =========================================================
    // RESOURCE HANDLING
    // =========================================================

    /**
     * Handles requests for resources that do not exist.
     *
     * This is particularly relevant for requests such as:
     *
     * GET /favicon.ico when testing API via APIDog, for example :D
     *
     * It should not be treated as an unexpected server error.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Resource not found.",
                request
        );
    }

    // =========================================================
    // DATABASE
    // =========================================================

    /**
     * Handles database constraint violations without exposing
     * database-specific implementation details to API clients.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "The request could not be completed "
                        + "because it violates a data constraint.",
                request
        );
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleClientDisconnect(
            AsyncRequestNotUsableException ex,
            HttpServletRequest request
    ) {
        log.debug(
                "Client disconnected while processing {} {}",
                request.getMethod(),
                request.getRequestURI()
        );
    }

    // =========================================================
    // UNEXPECTED ERRORS
    // =========================================================

    /**
     * Last-resort handler for unexpected application exceptions.
     *
     * The underlying exception is logged but never exposed to the
     * API client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "Unhandled exception while processing {} {}",
                request.getMethod(),
                request.getRequestURI(),
                exception
        );

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.",
                request
        );
    }

    // =========================================================
    // RESPONSE FACTORY
    // =========================================================

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                status,
                message,
                request,
                Map.of()
        );
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
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

    private String resolveValidationMessage(FieldError fieldError) {
        return fieldError.getDefaultMessage() != null
                ? fieldError.getDefaultMessage()
                : "Invalid value";
    }

    private String extractParameterName(
            ConstraintViolation<?> violation
    ) {
        String propertyPath = violation.getPropertyPath().toString();
        int lastDotIndex = propertyPath.lastIndexOf('.');

        if (lastDotIndex >= 0 && lastDotIndex < propertyPath.length() - 1) {
            return propertyPath.substring(lastDotIndex + 1);
        }

        return propertyPath;
    }
}