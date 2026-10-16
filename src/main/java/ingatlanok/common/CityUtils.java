package ingatlanok.common;

import ingatlanok.common.city.CityPresets;
import ingatlanok.common.city.CountyDirectory;

import java.util.*;

/**
 * Településekkel, megyékkel és lokációkkal kapcsolatos segédfüggvények homlokzata (Facade).
 * Delegál a specializált {@link CityPresets} és {@link CountyDirectory} komponensekhez.
 */
public final class CityUtils {

    private CityUtils() {
    }

    public static String normalizeCityKey(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            return "debrecen";
        }
        String normalized = SlugUtils.toSlug(cityName);
        return normalized.isEmpty() ? "debrecen" : normalized;
    }

    public static List<Map<String, String>> getPredefinedCities() {
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

    public static Map<String, List<String>> getPresets() {
        return CountyDirectory.getPresets();
    }
}
