package ingatlanok.core.dedup;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ingatlanok.common.SlugUtils;

/**
 * Extracts and cleans normalized street stems from listing text and URLs for {@link LocationConflictDetector}.
 */
final class StreetLocationExtractor {

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

  private StreetLocationExtractor() {}

  static String extract(
      String title, String location, String details, String url, String normalizedCity) {
    String combined = (title + " " + location + " " + details).trim();
    String found = findStreetInText(combined, normalizedCity);
    if (found != null) {
      return found;
    }
    return findStreetInText(extractUrlPathText(url), normalizedCity);
  }

  static String extractUrlPathText(String url) {
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
      if (QuarterLocationExtractor.isQuarterTokenMatch(token, q)) {
        return true;
      }
    }
    return false;
  }
}
