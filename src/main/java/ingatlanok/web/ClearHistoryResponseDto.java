package ingatlanok.web;

import java.io.Serializable;

/**
 * Strongly typed response for POST /api/history/clear (#401 / JAVA-017).
 */
public record ClearHistoryResponseDto(boolean success, String clearedCity, Boolean clearedAll)
    implements Serializable {

  public static ClearHistoryResponseDto forCity(String city) {
    return new ClearHistoryResponseDto(true, city, null);
  }

  public static ClearHistoryResponseDto forAll() {
    return new ClearHistoryResponseDto(true, null, true);
  }
}
