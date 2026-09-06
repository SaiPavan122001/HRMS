package com.hrms.common.exception;

import java.time.Instant;

public record ErrorResponse(
    String code,
    String message,
    Object details,
    String path,
    String correlationId,
    Instant timestamp
) {
  public static ErrorResponse of(String code, String message, Object details, String path, String correlationId) {
    return new ErrorResponse(code, message, details, path, correlationId, Instant.now());
  }
}
