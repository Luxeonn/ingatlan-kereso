package ingatlanok.web;

/**
 * Strongly typed response for POST /api/history/clear (#401 / JAVA-017).
 */
public record ClearHistoryResponseDto(boolean success, String clearedCity, Boolean clearedAll) {

  public static ClearHistoryResponseDto forCity(String city) {
    return new ClearHistoryResponseDto(true, city, null);
  }

  public static ClearHistoryResponseDto forAll() {
    return new ClearHistoryResponseDto(true, null, true);
  }
}
