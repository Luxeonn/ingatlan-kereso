package ingatlanok.core.dedup;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ingatlanok.common.SlugUtils;
import ingatlanok.model.IngatlanItem;

/**
 * Detects location conflicts and alignments across districts, quarters, and street names.
 */
public final class LocationConflictDetector {

  private static final Pattern BP_DISTRICT_ROMAN_PATTERN =
      Pattern.compile(
          "(?iuU)\\b(XXIII|XXII|XXI|XX|XIX|XVIII|XVII|XVI|XV|XIV|XIII|XII|XI|X|IX|VIII|VII|VI|V|IV|III|II|I)"
              + "\\.?\\s*(?:kerület|ker\\b|kerületben)");

  private static final Pattern BP_DISTRICT_ARABIC_PATTERN =
      Pattern.compile("(?iuU)\\b([1-9]|1[0-9]|2[0-3])\\.?\\s*(?:kerület|ker\\b|kerületben)");

  private static final Pattern BP_DISTRICT_URL_PATTERN =
      Pattern.compile("(?iuU)(?:budapest|bp)[-_/]?([1-9]|1[0-9]|2[0-3])[-_]?(?:kerület|ker)");

  private LocationConflictDetector() {}

  /**
   * Structured location details extracted from an IngatlanItem.
   */
  public record ExtractedLocation(
      Integer district,
      String quarter,
      String street) {

    public boolean hasDistrict() {
      return district != null && district > 0;
    }

    public boolean hasQuarter() {
      return quarter != null && !quarter.isBlank();
    }

    public boolean hasStreet() {
      return street != null && !street.isBlank();
    }
  }

  /**
   * Extracts location signals (Budapest district, city quarter, street name) from an item.
   */
  public static ExtractedLocation extractLocation(IngatlanItem item, String normalizedCity) {
    if (item == null) {
      return new ExtractedLocation(null, null, null);
    }

    String title = item.getTitle() != null ? item.getTitle() : "";
    String location = item.getLocation() != null ? item.getLocation() : "";
    String details = extractRawDetails(item);
    String url = item.getUrl() != null ? item.getUrl() : "";
    String combined = (title + " " + location + " " + details + " " + url).trim();

    Integer district = null;
    boolean isBudapest = "budapest".equals(normalizedCity);
    if (isBudapest) {
      district = extractBudapestDistrict(combined);
    }

    String quarter = isBudapest
        ? null
        : QuarterLocationExtractor.extract(location, title, details, url, normalizedCity);
    String street = StreetLocationExtractor.extract(title, location, details, url, normalizedCity);

    return new ExtractedLocation(district, quarter, street);
  }

  private static String extractRawDetails(IngatlanItem item) {
    String details = item.getDetails() != null ? item.getDetails() : "";
    if (item.getDetailsInfo() != null && item.getDetailsInfo().rawText() != null) {
      String raw = item.getDetailsInfo().rawText();
      if (!details.contains(raw)) {
        details = (details + " " + raw).trim();
      }
    }
    return details;
  }

  /**
   * Checks whether two items have conflicting location sub-units.
   */
  public static boolean hasConflict(ExtractedLocation locA, ExtractedLocation locB, String normalizedCity) {
    if (locA == null || locB == null) {
      return false;
    }
    return hasDistrictConflict(locA, locB, normalizedCity)
        || hasQuarterConflict(locA, locB)
        || hasStreetConflict(locA, locB);
  }

  private static boolean isConflictingText(String a, String b) {
    return !a.equals(b) && !a.contains(b) && !b.contains(a);
  }

  private static boolean hasDistrictConflict(ExtractedLocation a, ExtractedLocation b, String city) {
    return "budapest".equals(city)
        && a.hasDistrict()
        && b.hasDistrict()
        && !a.district().equals(b.district());
  }

  private static boolean hasQuarterConflict(ExtractedLocation a, ExtractedLocation b) {
    return a.hasQuarter() && b.hasQuarter() && isConflictingText(a.quarter(), b.quarter());
  }

  private static boolean hasStreetConflict(ExtractedLocation a, ExtractedLocation b) {
    return a.hasStreet() && b.hasStreet() && isConflictingText(a.street(), b.street());
  }

  /**
   * Checks if two locations refer to the same street (allowing prefix/full-name overlap).
   */
  public static boolean hasSameStreet(ExtractedLocation locA, ExtractedLocation locB) {
    return locA != null && locB != null
        && locA.hasStreet() && locB.hasStreet()
        && !isConflictingText(locA.street(), locB.street());
  }

  /**
   * Checks if two locations refer to the same district or city quarter.
   */
  public static boolean hasSameSubArea(ExtractedLocation locA, ExtractedLocation locB) {
    if (locA == null || locB == null) {
      return false;
    }
    if (locA.hasDistrict() && locB.hasDistrict() && locA.district().equals(locB.district())) {
      return true;
    }
    return locA.hasQuarter() && locB.hasQuarter()
        && !isConflictingText(locA.quarter(), locB.quarter());
  }

  /**
   * Checks if two items have positive affirmative location matching (same district, quarter, or street).
   */
  public static boolean hasAffirmativeMatch(ExtractedLocation locA, ExtractedLocation locB) {
    return hasSameStreet(locA, locB) || hasSameSubArea(locA, locB);
  }

  private static Integer extractBudapestDistrict(String text) {
    if (text == null || text.isBlank()) {
      return null;
    }

    Matcher mRoman = BP_DISTRICT_ROMAN_PATTERN.matcher(text);
    if (mRoman.find()) {
      Integer d = romanToNumber(mRoman.group(1));
      if (d != null) {
        return d;
      }
    }

    Integer arabic = parseDistrictNumber(BP_DISTRICT_ARABIC_PATTERN.matcher(text));
    if (arabic != null) {
      return arabic;
    }

    Integer urlDistrict = parseDistrictNumber(BP_DISTRICT_URL_PATTERN.matcher(text));
    if (urlDistrict != null) {
      return urlDistrict;
    }

    return findNamedDistrict(text);
  }

  private static Integer parseDistrictNumber(Matcher matcher) {
    if (matcher.find()) {
      try {
        int d = Integer.parseInt(matcher.group(1));
        if (d >= 1 && d <= 23) {
          return d;
        }
      } catch (NumberFormatException ignored) {
        // Fall through
      }
    }
    return null;
  }

  private static Integer findNamedDistrict(String text) {
    String slug = SlugUtils.toSlug(text);
    for (Map.Entry<String, Integer> entry : LocationDictionaries.BP_DISTRICT_NAMES.entrySet()) {
      if (slug.contains(entry.getKey())) {
        return entry.getValue();
      }
    }
    return null;
  }

  private static Integer romanToNumber(String roman) {
    if (roman == null) {
      return null;
    }
    int index = LocationDictionaries.ROMAN_DISTRICTS.indexOf(roman.toUpperCase(Locale.ROOT));
    return index >= 0 ? index + 1 : null;
  }
}
