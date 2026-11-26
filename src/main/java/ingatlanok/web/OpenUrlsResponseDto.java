package ingatlanok.web;

/**
 * Strongly typed response for POST /api/open-urls (#401 / JAVA-017).
 */
public record OpenUrlsResponseDto(boolean success, int count) {

  public static OpenUrlsResponseDto of(boolean success, int count) {
    return new OpenUrlsResponseDto(success, count);
  }
}
