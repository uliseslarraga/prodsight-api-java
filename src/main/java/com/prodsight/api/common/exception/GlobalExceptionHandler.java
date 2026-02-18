package com.prodsight.api.common.exception;

import com.prodsight.api.common.api.ApiErrorEnvelope;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ApiErrorEnvelope> handleNotFound(NotFoundException ex, HttpServletRequest req) {
    return error(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), req, null);
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ApiErrorEnvelope> handleConflict(ConflictException ex, HttpServletRequest req) {
    return error(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), req, null);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorEnvelope> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
    Map<String, Object> details = new HashMap<>();
    Map<String, String> fieldErrors = new HashMap<>();
    for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
      fieldErrors.put(fe.getField(), fe.getDefaultMessage());
    }
    details.put("fieldErrors", fieldErrors);
    return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", req, details);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiErrorEnvelope> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
    return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), req, null);
  }

  @ExceptionHandler(ErrorResponseException.class)
  public ResponseEntity<ApiErrorEnvelope> handleErrorResponse(ErrorResponseException ex, HttpServletRequest req) {
    HttpStatus status = (HttpStatus) ex.getStatusCode();
    return error(status, "ERROR", ex.getMessage(), req, null);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorEnvelope> handleGeneric(Exception ex, HttpServletRequest req) {
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", ex.getMessage(), req, null);
  }

  private ResponseEntity<ApiErrorEnvelope> error(
      HttpStatus status,
      String code,
      String message,
      HttpServletRequest req,
      Map<String, Object> details
  ) {
    String traceId = req.getHeader("X-Request-Id"); // if you propagate one; later use tracing MDC
    var body = new ApiErrorEnvelope(new ApiErrorEnvelope.ApiError(code, message, traceId, details));
    return ResponseEntity.status(status).body(body);
  }
}
