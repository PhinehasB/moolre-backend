package com.project.klare_server.common.error;

import com.project.klare_server.common.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex, HttpServletRequest request) {
        ErrorCode code = ex.getErrorCode();
        return build(code, code.name(), ex.getMessage(), null, request, ex, code.status().is5xxServerError());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.name(), "Access denied", null, request, ex, false);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return build(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.name(), "Authentication required", null, request, ex, false);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<ApiError.FieldViolation> violations = ex.getConstraintViolations().stream()
                .map(v -> new ApiError.FieldViolation(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return build(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.name(), "Request validation failed", violations, request, ex, false);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex, HttpServletRequest request) {
        return build(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.name(), "An unexpected error occurred", null, request, ex, true);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ApiError.FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldViolation)
                .toList();
        String traceId = newTraceId();
        ApiError error = ApiError.of(ErrorCode.VALIDATION_ERROR.name(), "Request validation failed", violations, traceId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String traceId = newTraceId();
        ApiError error = ApiError.of(ErrorCode.MALFORMED_REQUEST.name(), "Malformed request body", traceId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String traceId = newTraceId();
        ApiError error = ApiError.of(ErrorCode.UNSUPPORTED_MEDIA_TYPE.name(), "Unsupported media type", traceId);
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(ApiResponse.failure(error));
    }

    private ApiError.FieldViolation toFieldViolation(FieldError fieldError) {
        return new ApiError.FieldViolation(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private ResponseEntity<ApiResponse<Void>> build(
            ErrorCode code,
            String errorCode,
            String message,
            List<ApiError.FieldViolation> violations,
            HttpServletRequest request,
            Exception ex,
            boolean serverError) {
        String traceId = newTraceId();
        if (serverError) {
            log.error("traceId={} method={} path={} error={}", traceId, request.getMethod(), request.getRequestURI(), ex.toString(), ex);
        } else {
            log.warn("traceId={} method={} path={} code={} message={}", traceId, request.getMethod(), request.getRequestURI(), errorCode, message);
        }
        ApiError error = ApiError.of(errorCode, message, violations, traceId);
        return ResponseEntity.status(code.status()).body(ApiResponse.failure(error));
    }

    private String newTraceId() {
        return UUID.randomUUID().toString();
    }
}
