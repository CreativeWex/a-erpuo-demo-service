package com.example.orders.api;

import com.example.orders.service.OrderNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
    return build(HttpStatus.BAD_REQUEST, "Validation failed");
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
    return build(HttpStatus.BAD_REQUEST, "Malformed or missing request body");
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    return build(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + ex.getName() + "'");
  }

  @ExceptionHandler(OrderNotFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(OrderNotFoundException ex) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  private static ResponseEntity<ApiError> build(HttpStatus status, String message) {
    return ResponseEntity.status(status)
        .body(new ApiError(status.value(), status.getReasonPhrase(), message));
  }
}
