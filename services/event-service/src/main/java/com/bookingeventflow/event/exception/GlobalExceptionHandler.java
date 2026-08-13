package com.bookingeventflow.event.exception;

import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEventNotFound(
            EventNotFoundException ex,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request,
                Map.of()
        );
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(
            OptimisticLockingFailureException ex,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.CONFLICT,
                "The event was modified by another request. Please reload the event and try again.",
                request,
                Map.of()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
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
                                        error -> error.getDefaultMessage() != null
                                                ? error.getDefaultMessage()
                                                : "Invalid value",
                                        Collectors.toList()
                                )
                        ));

        return build(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request,
                errors
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.CONFLICT,
                "The request could not be completed because it violates a data constraint.",
                request,
                Map.of()
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request,
                Map.of()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception ex,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.",
                request,
                Map.of()
        );
    }

    private ResponseEntity<ErrorResponse> build(
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


    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.BAD_REQUEST,
                "Invalid value for parameter '" + ex.getName() + "'.",
                request,
                Map.of()
        );
    }
}