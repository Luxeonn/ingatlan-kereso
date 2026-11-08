package ingatlanok.portal;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import ingatlanok.common.HttpFetcher;
import ingatlanok.common.PropertyParser;
import ingatlanok.common.SlugUtils;
import ingatlanok.model.PropertyType;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.raw.ExtractionMethod;
import ingatlanok.model.raw.RawExtractionResult;

/** Shared modular helper methods for portal URL builders, HTML parsers, and scraper engines. */
public final class PortalSupport {

  private static final DateTimeFormatter TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private static final Set<String> PLACEHOLDER_MARKERS = Set.of(
      "nopic",
      "no-pic",
      "placeholder",
      "default-image",
      "no-image",
      "noimage",
      "/y/img/app_banner/",
      "/y/img/foot-logo",
      "/y/img/sale_icon"
  );

  private static final Set<String> CHALLENGE_TITLE_MARKERS = Set.of(
      "ellenőrzés",
      "cloudflare",
      "just a moment",
      "attention required",
      "security check",
      "bot védelem",
      "bot-védelem",
      "robot check"
  );

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
    String slug = SlugUtils.toSlug(rawCity);
    if (slug.isEmpty()) {
      return "Debrecen";
    }
    return Character.toUpperCase(slug.charAt(0)) + slug.substring(1);
  }

  public static PropertyType resolvePropertyType(SearchCriteria criteria) {
    if (criteria == null || criteria.getPropertyType() == null || criteria.getPropertyType().isBlank()) {
      return PropertyType.LAKAS;
    }
    PropertyType parsed = PropertyType.fromString(criteria.getPropertyType());
    return parsed != PropertyType.EGYEB_ISMERETLEN ? parsed : PropertyType.LAKAS;
  }

  public static String resolvePropertyTypeSlug(SearchCriteria criteria) {
    SearchCriteria safe = normalizeCriteria(criteria);
    PropertyType type = resolvePropertyType(safe);
    return resolvePropertyTypeSlug(type);
  }

  public static String resolvePropertyTypeSlug(PropertyType type) {
    if (type == null) {
      return "lakas";
    }
    return switch (type) {
      case HAZ -> "haz";
      case TELEK -> "telek";
      case GARAZS -> "garazs";
      case IPARI_KERESKEDELMI -> "ipari";
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
    String canonical = canonicalListingUrl(url);
    if (canonical.isBlank()) {
      return false;
    }
    if (processedUrls == null) {
      return true;
    }
    return processedUrls.add(canonical);
  }

  public static boolean markProcessedListing(
      Set<String> processedUrls,
      Set<String> processedNativeIds,
      String url,
      String nativeId) {
    String canonicalUrl = canonicalListingUrl(url);
    if (canonicalUrl.isBlank()) {
      return false;
    }
    if (processedNativeIds != null && nativeId != null && !nativeId.isBlank()) {
      String cleanId = nativeId.trim();
      if (!processedNativeIds.add(cleanId)) {
        return false;
      }
    }
    if (processedUrls == null) {
      return true;
    }
    return processedUrls.add(canonicalUrl);
  }

  public static String canonicalListingUrl(String url) {
    if (url == null || url.isBlank()) {
      return "";
    }
    String clean = url.trim();
    int hashIdx = clean.indexOf('#');
    if (hashIdx >= 0) {
      clean = clean.substring(0, hashIdx);
    }
    int queryIdx = clean.indexOf('?');
    if (queryIdx >= 0) {
      String query = clean.substring(queryIdx + 1);
      if (!query.contains("id=")) {
        clean = clean.substring(0, queryIdx);
      }
    }
    while (clean.endsWith("/") && clean.length() > "https://a.b/".length()) {
      clean = clean.substring(0, clean.length() - 1);
    }
    return clean;
  }

  public static String nowTimestamp() {
    return TIMESTAMP_FORMATTER.format(LocalDateTime.now());
  }

  public static boolean isPlaceholderImageUrl(String url) {
    if (url == null || url.isBlank()) {
      return true;
    }
    String lower = url.toLowerCase(java.util.Locale.ROOT);
    return PLACEHOLDER_MARKERS.stream().anyMatch(lower::contains);
  }

  public static String extractImageWithFallback(Element element, String baseUrl) {
    if (element == null) {
      return null;
    }
    String imageUrl = null;
    Element lazyImg = element.selectFirst("img.lazyload[data-src], img.lazy[data-src], img[data-src]");
    if (lazyImg != null && !lazyImg.attr("data-src").isBlank()) {
      imageUrl = lazyImg.attr("data-src");
    }
    if (imageUrl == null || imageUrl.isBlank()) {
      imageUrl = PropertyParser.extractImageUrl(element);
    }
    if (imageUrl == null || imageUrl.isBlank()) {
      return null;
    }
    String resolved = resolveUrl(baseUrl, imageUrl);
    return isPlaceholderImageUrl(resolved) ? null : resolved;
  }

  public static boolean isCommonBotChallenge(Document document) {
    if (document == null) {
      return false;
    }
    String title = document.title();
    if (title != null) {
      String lower = title.toLowerCase(java.util.Locale.ROOT);
      if (CHALLENGE_TITLE_MARKERS.stream().anyMatch(lower::contains)) {
        return true;
      }
    }
    return !document
        .select(
            "#challenge-error-text, #robot-protection-illustration,"
                + " script[src*='challenge-platform'], .cf-browser-verification,"
                + " #cf-wrapper, .g-recaptcha, .cf-turnstile")
        .isEmpty();
  }

  public static RawExtractionResult resolveFirstPageFetchFailure(
      ExtractionMethod method, String fallbackUrl, String defaultErrorMessage) {
    Integer lastStatus = HttpFetcher.getLastStatusCode();
    if (lastStatus != null && (lastStatus == 403 || lastStatus == 429)) {
      return RawExtractionResult.botProtected(
          method, fallbackUrl, "HTTP " + lastStatus + " bot-védelem / rate limit detektálva");
    }
    int statusCode = (lastStatus != null && lastStatus >= 400) ? lastStatus : 500;
    return RawExtractionResult.failure(method, fallbackUrl, defaultErrorMessage, statusCode);
  }
}
