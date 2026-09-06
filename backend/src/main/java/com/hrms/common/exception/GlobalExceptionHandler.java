package com.hrms.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private String cid() { return MDC.get("correlationId"); }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
    Map<String, String> details = new HashMap<>();
    for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
      details.put(fe.getField(), fe.getDefaultMessage());
    }
    return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", details, req);
  }

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ErrorResponse> handleApi(ApiException ex, HttpServletRequest req) {
    return build(ex.getStatus(), ex.getCode(), ex.getMessage(), null, req);
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleBadCreds(BadCredentialsException ex, HttpServletRequest req) {
    return build(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password", null, req);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
    return build(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You do not have permission to perform this action", null, req);
  }

  @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
  public ResponseEntity<ErrorResponse> handleUnreadable(Exception ex, HttpServletRequest req) {
    return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Malformed request body or parameter", null, req);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex, HttpServletRequest req) {
    return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found", null, req);
  }

  @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrity(Exception ex, HttpServletRequest req) {
    return build(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION", "The operation conflicts with existing data", null, req);
  }

  @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
  public ResponseEntity<ErrorResponse> handleOptimisticLock(Exception ex, HttpServletRequest req) {
    return build(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION", "The record was modified by someone else. Please retry.", null, req);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
    org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class).error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred. Please try again later.", null, req);
  }

  private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message, Object details, HttpServletRequest req) {
    return ResponseEntity.status(status)
        .body(ErrorResponse.of(code, message, details, req.getRequestURI(), cid()));
  }
}
