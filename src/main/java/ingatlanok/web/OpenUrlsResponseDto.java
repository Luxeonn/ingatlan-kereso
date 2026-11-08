package ingatlanok.web;

import java.io.Serializable;

/**
 * Strongly typed response for POST /api/open-urls (#401 / JAVA-017).
 */
public record OpenUrlsResponseDto(boolean success, int count) implements Serializable {

  public static OpenUrlsResponseDto of(boolean success, int count) {
    return new OpenUrlsResponseDto(success, count);
  }
}
