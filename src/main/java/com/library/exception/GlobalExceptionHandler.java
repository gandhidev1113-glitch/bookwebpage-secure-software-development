package com.library.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() == null
                                ? "Invalid value"
                                : error.getDefaultMessage(),
                        (first, second) -> first,
                        LinkedHashMap::new
                ));

        return build(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request.getRequestURI(),
                fieldErrors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (first, second) -> first,
                        LinkedHashMap::new
                ));

        return build(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request.getRequestURI(),
                fieldErrors
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON request",
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.BAD_REQUEST,
                "Invalid request parameter type",
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiError> handleBusinessExceptions(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        String message = ex.getMessage() == null || ex.getMessage().isBlank()
                ? "Invalid request"
                : ex.getMessage();

        return build(
                HttpStatus.BAD_REQUEST,
                message,
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiError> handleUsernameNotFound(
            UsernameNotFoundException ex,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.NOT_FOUND,
                "User not found",
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.FORBIDDEN,
                "Access denied",
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.NOT_FOUND,
                "Resource not found",
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                request.getRequestURI(),
                null
        );
    }

    private ResponseEntity<ApiError> build(
            HttpStatus status,
            String message,
            String path,
            Map<String, String> fieldErrors
    ) {
        Map<String, String> safeFieldErrors =
                (fieldErrors == null || fieldErrors.isEmpty()) ? null : fieldErrors;

        ApiError apiError = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                safeFieldErrors
        );
        return ResponseEntity.status(status).body(apiError);
    }
}
