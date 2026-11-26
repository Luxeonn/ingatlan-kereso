package ingatlanok.web;

/**
 * Strongly typed response for POST /api/items/viewed (#401 / JAVA-017).
 */
public record MarkViewedResponseDto(boolean success, boolean marked, String url) {

  public static MarkViewedResponseDto of(boolean success, boolean marked, String url) {
    return new MarkViewedResponseDto(success, marked, url);
  }
}
