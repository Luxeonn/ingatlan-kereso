package ingatlanok.common;

import java.text.Normalizer;
import java.util.Locale;

public final class SlugUtils {

  private SlugUtils() {}

  public static String stripAccents(String text) {
    if (text == null) {
      return "";
    }
    String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
    return normalized.replaceAll("\\p{M}", "");
  }

  public static String toSlug(String text) {
    if (text == null || text.isBlank()) {
      return "";
    }
    String noAccents = stripAccents(text.trim().toLowerCase(Locale.ROOT));
    String slug = noAccents.replaceAll("[^a-z0-9]+", "-");
    int start = 0;
    while (start < slug.length() && slug.charAt(start) == '-') {
      start++;
    }
    int end = slug.length();
    while (end > start && slug.charAt(end - 1) == '-') {
      end--;
    }
    return slug.substring(start, end);
  }
}
