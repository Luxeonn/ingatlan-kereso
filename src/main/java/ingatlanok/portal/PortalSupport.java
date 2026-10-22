package ingatlanok.portal;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import ingatlanok.common.PropertyParser;
import ingatlanok.common.SlugUtils;
import ingatlanok.model.SearchCriteria;

/** Shared modular helper methods for portal URL builders, HTML parsers, and scraper engines. */
public final class PortalSupport {

  private static final DateTimeFormatter TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private PortalSupport() {}

  public static SearchCriteria normalizeCriteria(SearchCriteria criteria) {
    return criteria != null ? criteria : new SearchCriteria();
  }

  public static String resolveCitySlug(SearchCriteria criteria) {
    SearchCriteria safe = normalizeCriteria(criteria);
    String slug = SlugUtils.toSlug(safe.getCity());
    return slug.isEmpty() ? "debrecen" : slug;
  }

  public static String resolveDefaultCity(SearchCriteria criteria) {
    if (criteria != null && criteria.getCity() != null && !criteria.getCity().isBlank()) {
      return criteria.getCity().trim();
    }
    return "Debrecen";
  }

  public static String resolveAsciiCapitalizedCity(String rawCity) {
    String city = (rawCity == null || rawCity.isBlank()) ? "Debrecen" : rawCity.trim();
    String stripped = SlugUtils.stripAccents(city).replaceAll("[^A-Za-z0-9\\-]+", "-");
    String clean = stripped.replaceAll("^-+|-+$", "");
    if (clean.isEmpty()) {
      return "Debrecen";
    }
    return Character.toUpperCase(clean.charAt(0)) + clean.substring(1).toLowerCase(Locale.ROOT);
  }

  public static String resolvePropertyTypeSlug(SearchCriteria criteria) {
    SearchCriteria safe = normalizeCriteria(criteria);
    String rawType =
        safe.getPropertyType() != null
            ? safe.getPropertyType().toLowerCase(Locale.ROOT).trim()
            : "lakas";
    return switch (rawType) {
      case "haz" -> "haz";
      case "telek" -> "telek";
      default -> "lakas";
    };
  }

  public static String resolveUrl(String baseUrl, String rawUrl) {
    if (rawUrl == null || rawUrl.isBlank()) {
      return "";
    }
    String trimmed = rawUrl.trim();
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
      return trimmed;
    }
    if (trimmed.startsWith("//")) {
      return "https:" + trimmed;
    }
    String cleanBase =
        (baseUrl != null && baseUrl.endsWith("/"))
            ? baseUrl.substring(0, baseUrl.length() - 1)
            : (baseUrl != null ? baseUrl : "");
    return cleanBase + (trimmed.startsWith("/") ? "" : "/") + trimmed;
  }

  public static boolean markProcessed(Set<String> processedUrls, String url) {
    if (url == null || url.isBlank()) {
      return false;
    }
    if (processedUrls == null) {
      return true;
    }
    return processedUrls.add(url);
  }

  public static String nowTimestamp() {
    return TIMESTAMP_FORMATTER.format(LocalDateTime.now());
  }

  public static String extractImageWithFallback(Element element, String baseUrl) {
    if (element == null) {
      return null;
    }
    String imageUrl = null;
    Element lazyImg = element.selectFirst("img.lazyload[data-src], img[data-src]");
    if (lazyImg != null && !lazyImg.attr("data-src").isBlank()) {
      imageUrl = lazyImg.attr("data-src");
    }
    if (imageUrl == null || imageUrl.isBlank()) {
      imageUrl = PropertyParser.extractImageUrl(element);
    }
    if ((imageUrl == null || imageUrl.isBlank()) && element.parent() != null) {
      imageUrl = PropertyParser.extractImageUrl(element.parent());
    }
    if (imageUrl == null || imageUrl.isBlank()) {
      return null;
    }
    return resolveUrl(baseUrl, imageUrl);
  }

  public static boolean isCommonBotChallenge(Document document) {
    if (document == null) {
      return false;
    }
    String title = document.title();
    boolean titleIndicatesChallenge =
        title != null
            && (title.contains("ellenőrzés")
                || title.contains("Cloudflare")
                || title.contains("Just a moment")
                || title.contains("Attention Required"));
    return titleIndicatesChallenge
        || !document
            .select(
                "#challenge-error-text, #robot-protection-illustration,"
                    + " script[src*='challenge-platform']")
            .isEmpty();
  }
}
