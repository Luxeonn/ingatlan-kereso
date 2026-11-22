package ingatlanok.common;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import ingatlanok.common.city.CityPresets;
import ingatlanok.common.city.CountyDirectory;
import ingatlanok.model.PredefinedCity;

public final class CityUtils {

  private static final int MAX_SETTLEMENT_TOKENS = 3;

  private static final Set<String> NON_SETTLEMENT_TOKENS = Set.of(
      "megye", "varmegye", "kerulet", "utca", "ut", "ter", "korut", "sor", "kozpont",
      "belvaros", "lakotelep", "elado", "kiado", "haz", "lakas", "telek", "ingatlan");

  private CityUtils() {}

  public static String normalizeCityKey(String cityName) {
    if (cityName == null || cityName.isBlank()) {
      return "debrecen";
    }
    String normalized = SlugUtils.toSlug(cityName);
    return normalized.isEmpty() ? "debrecen" : normalized;
  }

  public static List<PredefinedCity> getPredefinedCities() {
    return CityPresets.getPredefinedCities();
  }

  public static Optional<String> getCountySlug(String cityName) {
    return CountyDirectory.getCountySlug(cityName, normalizeCityKey(cityName));
  }

  public static Optional<String> getCountyName(String cityName) {
    return CountyDirectory.getCountyName(cityName, normalizeCityKey(cityName));
  }

  public static boolean isKnownCity(String cityName) {
    if (cityName == null || cityName.isBlank()) {
      return false;
    }
    return CountyDirectory.isKnownCity(normalizeCityKey(cityName));
  }

  public static boolean isSameOrSubLocation(String locA, String locB) {
    if (locA == null || locB == null || locA.isBlank() || locB.isBlank()) {
      return false;
    }
    String slugA = SlugUtils.toSlug(locA);
    String slugB = SlugUtils.toSlug(locB);
    return slugA.equals(slugB) || slugA.contains(slugB) || slugB.contains(slugA);
  }

  public static boolean containsDifferentKnownCity(String textSlug, String targetCitySlug) {
    return CountyDirectory.containsDifferentKnownCity(textSlug, targetCitySlug);
  }

  /**
   * Returns the leading settlement name of a comma-separated address (e.g. {@code "Ebes"} from
   * {@code "Ebes, Kertész utca 4."}) when the first segment looks like a settlement name, or
   * {@code null} when the text has no comma or the first segment is a street, district or county.
   */
  public static String extractLeadingSettlement(String text) {
    if (text == null || text.indexOf(',') <= 0) {
      return null;
    }
    String candidate = text.substring(0, text.indexOf(',')).trim();
    String slug = SlugUtils.toSlug(candidate);
    if (slug.isEmpty() || slug.chars().anyMatch(Character::isDigit)) {
      return null;
    }
    String[] tokens = slug.split("[^a-z]+");
    if (tokens.length > MAX_SETTLEMENT_TOKENS) {
      return null;
    }
    for (String token : tokens) {
      if (NON_SETTLEMENT_TOKENS.contains(token)) {
        return null;
      }
    }
    return candidate;
  }

  public static Map<String, List<String>> getPresets() {
    return CountyDirectory.getPresets();
  }
}
