package ingatlanok.core.dedup;

import java.util.Map;

import ingatlanok.common.CityUtils;
import ingatlanok.common.SlugUtils;

/**
 * Extracts and normalizes city quarter sub-area identifiers for {@link LocationConflictDetector}.
 */
final class QuarterLocationExtractor {

  private QuarterLocationExtractor() {}

  static String extract(
      String location, String title, String details, String url, String normalizedCity) {
    String commaQuarter = extractCommaQuarter(location, normalizedCity, false);
    if (commaQuarter != null) {
      return commaQuarter;
    }
    String titleQuarter = extractCommaQuarter(title, normalizedCity, true);
    if (titleQuarter != null) {
      return titleQuarter;
    }
    String textSlug = SlugUtils.toSlug(
        (details + " " + location + " " + title + " "
            + StreetLocationExtractor.extractUrlPathText(url)).trim());
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

  static boolean isQuarterTokenMatch(String token, String prefix) {
    if (token == null || !token.startsWith(prefix)) {
      return false;
    }
    String suffix = token.substring(prefix.length());
    return !suffix.startsWith("ias") && !suffix.startsWith("ies") && !suffix.startsWith("kozel");
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
    if (StreetLocationExtractor.extract("", suffix, "", "", normalizedCity) != null) {
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
}
