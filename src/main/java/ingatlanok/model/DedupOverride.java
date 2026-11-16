package ingatlanok.model;

import java.util.Objects;

/**
 * Persistent manual deduplication override between a canonically ordered pair of listing URLs
 * ({@code urlA < urlB}).
 */
public record DedupOverride(
    Long id,
    String urlA,
    String urlB,
    DedupOverrideType type,
    String normalizedCity,
    String createdAt) {

  public DedupOverride(String urlA, String urlB, DedupOverrideType type) {
    this(null, urlA, urlB, type, null, null);
  }

  public record UrlPair(String urlA, String urlB) {}

  public static UrlPair canonicalPair(String firstUrl, String secondUrl) {
    if (firstUrl == null || firstUrl.isBlank() || secondUrl == null || secondUrl.isBlank()) {
      throw new IllegalArgumentException("A felülbíráláshoz két érvényes URL szükséges");
    }
    String trimmedA = firstUrl.trim();
    String trimmedB = secondUrl.trim();
    int cmp = trimmedA.compareTo(trimmedB);
    if (cmp == 0) {
      throw new IllegalArgumentException("Két azonos URL között nem hozható létre felülbírálás");
    }
    return cmp < 0 ? new UrlPair(trimmedA, trimmedB) : new UrlPair(trimmedB, trimmedA);
  }

  public boolean matchesPair(String firstUrl, String secondUrl) {
    if (firstUrl == null || secondUrl == null) {
      return false;
    }
    String trimmedA = firstUrl.trim();
    String trimmedB = secondUrl.trim();
    if (trimmedA.isEmpty() || trimmedB.isEmpty() || trimmedA.equals(trimmedB)) {
      return false;
    }
    String min = trimmedA.compareTo(trimmedB) < 0 ? trimmedA : trimmedB;
    String max = trimmedA.compareTo(trimmedB) < 0 ? trimmedB : trimmedA;
    return Objects.equals(urlA, min) && Objects.equals(urlB, max);
  }
}
