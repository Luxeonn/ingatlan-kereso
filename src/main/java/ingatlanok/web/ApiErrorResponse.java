package ingatlanok.web;

import java.io.Serializable;
import java.time.Instant;

/**
 * Standard RFC-compliant API error response payload (#401 / JAVA-024).
 */
public record ApiErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path)
    implements Serializable {

  public static ApiErrorResponse of(int status, String error, String message, String path) {
    return new ApiErrorResponse(Instant.now(), status, error, message, path);
  }
}
