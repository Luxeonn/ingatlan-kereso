package ingatlanok.core.dedup;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ingatlanok.common.CityUtils;
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

  private static final Pattern STREET_NAME_PATTERN =
      Pattern.compile(
          "(?iuU)([A-ZÁÉÍÓÖŐÚÜŰa-záéíóöőúüű0-9.\\- ]{2,40}?)\\s+"
              + "(utcájában|utcában|utcán|utcája|utcai(?!\\s+kert\\b)|utca|u\\.|u\\b"
              + "|útban|úton|útja|úti|út|ut"
              + "|téren|téri|tere|tér|ter"
              + "|körúton|körúti|korut|körút"
              + "|fasoron|fasori|fasor"
              + "|sétányon|sétányi|setany|sétány"
              + "|soron|sori|sora|sor"
              + "|dűlőben|dűlőn|dűlői|dűlő|dulo"
              + "|közben|közi|köz|koz)\\b");

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

    String quarter = extractQuarter(location, title, details, url, normalizedCity, isBudapest);
    String street = extractStreet(title, location, details, url, normalizedCity);

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

  private static String extractQuarter(
      String location, String title, String details, String url,
      String normalizedCity, boolean isBudapest) {
    if (isBudapest) {
      return null;
    }
    String commaQuarter = extractCommaQuarter(location, normalizedCity, false);
    if (commaQuarter != null) {
      return commaQuarter;
    }
    String titleQuarter = extractCommaQuarter(title, normalizedCity, true);
    if (titleQuarter != null) {
      return titleQuarter;
    }
    String textSlug = SlugUtils.toSlug(
        (details + " " + location + " " + title + " " + extractUrlPathText(url)).trim());
    for (String compound : LocationDictionaries.COMPOUND_QUARTERS) {
      if (textSlug.equals(compound)
          || textSlug.startsWith(compound + "-")
          || textSlug.contains("-" + compound)) {
        return normalizeQuarterSynonym(compound);
      }
    }
    for (String q : LocationDictionaries.QUARTERS) {
      if (matchesSlugWordPrefix(textSlug, q)) {
        return normalizeQuarterSynonym(q);
      }
    }
    return null;
  }

  private static String extractCommaQuarter(
      String source, String normalizedCity, boolean requireCityPrefix) {
    if (source == null || !source.contains(",")) {
      return null;
    }
    String[] parts = source.split(",", 2);
    if (parts.length <= 1) {
      return null;
    }
    if (requireCityPrefix && !CityUtils.normalizeCityKey(parts[0].trim()).equals(normalizedCity)) {
      return null;
    }
    String suffix = requireCityPrefix
        ? parts[1].trim().split("\\s+[-|]\\s+|,", 2)[0].trim()
        : parts[1].trim();
    if (extractStreet("", suffix, "", "", normalizedCity) != null) {
      return null;
    }
    String q = normalizeQuarterSynonym(SlugUtils.toSlug(suffix));
    if (isQuarterNoise(q, requireCityPrefix) || q.isBlank() || q.equals(normalizedCity)) {
      return null;
    }
    return q;
  }

  private static boolean isQuarterNoise(String slug, boolean strictTitleCheck) {
    if (slug == null || slug.isBlank() || slug.equals("em") || slug.endsWith("-em")) {
      return true;
    }
    for (String token : LocationDictionaries.QUARTER_NOISE_TOKENS) {
      if (slug.contains(token)) {
        return true;
      }
    }
    return strictTitleCheck && hasTitleNonQuarterToken(slug);
  }

  private static boolean hasTitleNonQuarterToken(String slug) {
    if (slug.chars().anyMatch(Character::isDigit)) {
      return true;
    }
    for (String part : slug.split("-")) {
      if (LocationDictionaries.TITLE_COMMA_NON_QUARTER_TOKENS.contains(part)) {
        return true;
      }
    }
    return false;
  }

  private static boolean matchesSlugWordPrefix(String slug, String prefix) {
    if (slug == null || slug.isBlank() || prefix == null || prefix.isBlank()) {
      return false;
    }
    String[] tokens = slug.split("-");
    for (int i = 0; i < tokens.length; i++) {
      if (isQuarterTokenMatch(tokens[i], prefix)
          && !isProximityPostposition(i + 1 < tokens.length ? tokens[i + 1] : null)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isQuarterTokenMatch(String token, String prefix) {
    if (token == null || !token.startsWith(prefix)) {
      return false;
    }
    String suffix = token.substring(prefix.length());
    return !suffix.startsWith("ias") && !suffix.startsWith("ies") && !suffix.startsWith("kozel");
  }

  private static boolean isProximityPostposition(String nextToken) {
    if (nextToken == null || nextToken.isBlank()) {
      return false;
    }
    return nextToken.startsWith("kozel")
        || nextToken.startsWith("mellett")
        || nextToken.startsWith("hatar")
        || nextToken.startsWith("irany");
  }

  private static String normalizeQuarterSynonym(String quarter) {
    if (quarter == null) {
      return null;
    }
    for (Map.Entry<String, String> entry : LocationDictionaries.QUARTER_SYNONYMS) {
      if (quarter.startsWith(entry.getKey())) {
        return entry.getValue();
      }
    }
    return quarter;
  }

  private static String extractStreet(
      String title, String location, String details, String url, String normalizedCity) {
    String combined = (title + " " + location + " " + details).trim();
    String found = findStreetInText(combined, normalizedCity);
    if (found != null) {
      return found;
    }
    return findStreetInText(extractUrlPathText(url), normalizedCity);
  }

  private static String findStreetInText(String text, String normalizedCity) {
    if (text == null || text.isBlank()) {
      return null;
    }
    Matcher m = STREET_NAME_PATTERN.matcher(text);
    while (m.find()) {
      String cleaned = cleanStreetStem(m.group(1), normalizedCity);
      if (cleaned != null) {
        return cleaned;
      }
    }
    return null;
  }

  private static String extractUrlPathText(String url) {
    if (url == null || url.isBlank()) {
      return "";
    }
    int schemeIdx = url.indexOf("://");
    int pathStart = url.indexOf('/', schemeIdx >= 0 ? schemeIdx + 3 : 0);
    if (pathStart < 0) {
      return "";
    }
    return url.substring(pathStart).split("[?#]", 2)[0]
        .replace('/', ' ').replace('-', ' ').replace('_', ' ').trim();
  }

  private static String cleanStreetStem(String rawPrefix, String normalizedCity) {
    String slug = SlugUtils.toSlug(rawPrefix != null ? rawPrefix.trim() : "");
    if (slug.isBlank()) {
      return null;
    }
    String[] tokens = slug.split("-");
    int start = 0;
    while (start < tokens.length - 1 && isLeadingStreetNoise(tokens[start], normalizedCity)) {
      start++;
    }
    if (LocationDictionaries.INVALID_STREET_STEMS.contains(tokens[tokens.length - 1])) {
      return null;
    }
    String stem = String.join("-", Arrays.copyOfRange(tokens, start, tokens.length));
    return stem.length() >= 3 && !LocationDictionaries.INVALID_STREET_STEMS.contains(stem) ? stem : null;
  }

  private static boolean isLeadingStreetNoise(String token, String normalizedCity) {
    if (LocationDictionaries.STREET_LEADING_PREFIXES.contains(token)
        || LocationDictionaries.BP_DISTRICT_NAMES.containsKey(token)) {
      return true;
    }
    if (normalizedCity != null && !normalizedCity.isBlank() && token.startsWith(normalizedCity)) {
      return true;
    }
    for (String q : LocationDictionaries.QUARTERS) {
      if (isQuarterTokenMatch(token, q)) {
        return true;
      }
    }
    return false;
  }

  private static Integer romanToNumber(String roman) {
    if (roman == null) {
      return null;
    }
    int index = LocationDictionaries.ROMAN_DISTRICTS.indexOf(roman.toUpperCase(Locale.ROOT));
    return index >= 0 ? index + 1 : null;
  }
}
