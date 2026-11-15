package ingatlanok.core.dedup;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ingatlanok.common.SlugUtils;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PropertyType;

/**
 * Textual tokenization, multi-generation detection, and semantic similarity evaluation.
 */
public final class TextualSimilarityMatcher {

  private static final Pattern USER_ID_URL_PATTERN =
      Pattern.compile("(?i)/user-id-(\\d+)(?=/|$)");

  private static final Set<String> STOPWORDS =
      Set.of(
          "elado", "kiado", "ingatlan", "ingatlanok", "haz", "lakas", "telek",
          "otthon", "centrum", "duna", "house", "jofogas", "koltozzbe", "ingatlanbazar",
          "hirdetes", "forint", "alatt", "egy", "ket", "harom", "szoba", "szobas",
          "nagyon", "kivalo", "szep", "minden", "ahol", "ami", "itt", "van",
          "meg", "csak", "mar", "vagy", "hogy", "mert", "akarmilyen",
          "fedezze", "fel", "almai", "otthonat", "varja", "ont", "sziveben",
          "csaladi", "ikerhaz", "sorhaz", "tegla", "panel",
          "panellakas", "teglalakas", "garzon", "garzonlakas",
          "kisterseg", "kistersegben", "jaras", "megye", "megyeben");

  private static final Set<String> MULTI_GEN_MARKERS =
      Set.of(
          "ketgeneracios", "ket-generacios", "2-generacios", "2generacios",
          "ket-epulet", "2-epulet", "ket-lakresz", "2-lakresz", "osszekoltoz");

  private TextualSimilarityMatcher() {}

  /**
   * Extracts distinctive alphabetic tokens from an item's title.
   */
  public static Set<String> extractDistinctiveTokens(IngatlanItem item) {
    if (item == null) {
      return Collections.emptySet();
    }

    String title = item.getTitle() != null ? item.getTitle() : "";
    String raw = title.toLowerCase(Locale.ROOT);
    String stripped = SlugUtils.stripAccents(raw);
    String cityPrefix = extractNormalizedCityPrefix(item);

    String[] parts = stripped.split("[^a-z]+");
    Set<String> tokens = new HashSet<>();
    for (String part : parts) {
      if (part == null || part.isBlank()) {
        continue;
      }
      String clean = part.trim();
      if (clean.length() >= 3
          && !STOPWORDS.contains(clean)
          && !isCityToken(clean, cityPrefix)) {
        tokens.add(clean);
      }
    }
    return tokens;
  }

  private static String extractNormalizedCityPrefix(IngatlanItem item) {
    if (item == null || item.getCity() == null || item.getCity().isBlank()) {
      return "";
    }
    return SlugUtils.stripAccents(item.getCity().trim().toLowerCase(Locale.ROOT))
        .replaceAll("[^a-z]+", "");
  }

  private static boolean isCityToken(String token, String cityPrefix) {
    return !cityPrefix.isBlank() && cityPrefix.length() >= 3 && token.startsWith(cityPrefix);
  }

  /**
   * Calculates Jaccard token similarity between two token sets.
   */
  public static double calculateJaccardSimilarity(Set<String> tokensA, Set<String> tokensB) {
    if (tokensA == null || tokensB == null || tokensA.isEmpty() || tokensB.isEmpty()) {
      return 0.0;
    }
    Set<String> intersection = new HashSet<>(tokensA);
    intersection.retainAll(tokensB);

    Set<String> union = new HashSet<>(tokensA);
    union.addAll(tokensB);

    return (double) intersection.size() / (double) union.size();
  }

  /**
   * Checks if an item indicates a 2-generation or multi-building/multi-unit property.
   */
  public static boolean isMultiGeneration(IngatlanItem item) {
    if (item == null) {
      return false;
    }
    String text = (item.getTitle() + " " + item.getDetails() + " " + item.getUrl()).toLowerCase(Locale.ROOT);
    String slug = SlugUtils.toSlug(text);
    return MULTI_GEN_MARKERS.stream().anyMatch(slug::contains);
  }

  /**
   * Checks whether two token sets share at least one distinctive keyword (>= 4 characters).
   */
  public static boolean hasDistinctiveKeywordOverlap(Set<String> tokensA, Set<String> tokensB) {
    if (tokensA == null || tokensB == null || tokensA.isEmpty() || tokensB.isEmpty()) {
      return false;
    }
    for (String tokenA : tokensA) {
      if (tokenA.length() >= 4 && tokensB.contains(tokenA)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks whether two listings with descriptive titles have zero token overlap
   * indicating completely disparate properties.
   */
  public static boolean hasDirectTitleConflict(
      IngatlanItem a,
      IngatlanItem b,
      Set<String> tokensA,
      Set<String> tokensB,
      boolean hasAffirmativeLocation) {

    if (hasAffirmativeLocation) {
      return false; // Confirmed street/quarter overrides generic title divergence
    }

    if (tokensA == null || tokensB == null) {
      return false;
    }

    // If both listings have rich descriptive titles (>= 3 distinctive tokens)
    // but share zero tokens in common, they describe completely different properties
    if (tokensA.size() >= 3 && tokensB.size() >= 3) {
      Set<String> common = new HashSet<>(tokensA);
      common.retainAll(tokensB);
      if (common.isEmpty()) {
        return true;
      }
    }

    return false;
  }

  /**
   * Evaluates whether two items have strong textual similarity based on affirmative location,
   * identical slugs, title substring containment, or high Jaccard token overlap.
   */
  public static boolean hasStrongTitleSimilarity(
      IngatlanItem a,
      IngatlanItem b,
      Set<String> tokensA,
      Set<String> tokensB,
      boolean hasAffirmativeLocation) {
    if (hasAffirmativeLocation) {
      return true;
    }
    int minTokens = minTokenCount(tokensA, tokensB);
    String titleA = a != null ? a.getTitle() : "";
    String titleB = b != null ? b.getTitle() : "";
    if (isExactDistinctiveSlugMatch(titleA, titleB, minTokens)) {
      return true;
    }
    if (minTokens == 0) {
      return false;
    }
    return (minTokens >= 2 && isSlugSimilar(titleA, titleB)) || hasTokenSimilarity(tokensA, tokensB);
  }

  private static int minTokenCount(Set<String> tokensA, Set<String> tokensB) {
    if (tokensA == null || tokensB == null) {
      return 0;
    }
    return Math.min(tokensA.size(), tokensB.size());
  }

  private static boolean isExactDistinctiveSlugMatch(String titleA, String titleB, int minTokens) {
    String slugA = SlugUtils.toSlug(titleA);
    String slugB = SlugUtils.toSlug(titleB);
    if (slugA.isBlank() || !slugA.equals(slugB)) {
      return false;
    }
    return minTokens >= 1 || slugA.split("-").length >= 5;
  }

  public static boolean hasIdenticalTitleSlug(IngatlanItem a, IngatlanItem b) {
    String slugA = SlugUtils.toSlug(a != null ? a.getTitle() : "");
    String slugB = SlugUtils.toSlug(b != null ? b.getTitle() : "");
    if (slugA.isBlank() || !slugA.equals(slugB)) {
      return false;
    }
    int distinctiveCount = extractDistinctiveTokens(a).size();
    if (distinctiveCount >= 2) {
      return true;
    }
    if (slugA.contains("kisterseg") && !hasSameUserIdInUrl(a, b)) {
      return false;
    }
    if (distinctiveCount >= 1) {
      return true;
    }
    return isGenericTemplateSlug(a, slugA);
  }

  /**
   * Checks whether an item title represents a multi-token generic template slug (LAKAS/HAZ/TELEK).
   */
  public static boolean isGenericTemplateSlug(IngatlanItem item, String slug) {
    if (item == null || slug == null || slug.isBlank()) {
      return false;
    }
    PropertyType type = item.getPropertyType();
    if (type != PropertyType.LAKAS && type != PropertyType.HAZ && type != PropertyType.TELEK) {
      return false;
    }
    return slug.split("-").length >= 4;
  }

  /**
   * Checks whether two items both have portal-generated generic template titles without distinctive words.
   */
  public static boolean areBothGenericTemplateTitles(IngatlanItem a, IngatlanItem b) {
    if (a == null || b == null) {
      return false;
    }
    String slugA = SlugUtils.toSlug(a.getTitle());
    String slugB = SlugUtils.toSlug(b.getTitle());
    if (slugA.isBlank() || slugB.isBlank()) {
      return false;
    }
    if ((slugA.contains("kisterseg") || slugB.contains("kisterseg")) && !hasSameUserIdInUrl(a, b)) {
      return false;
    }
    return isGenericTemplateSlug(a, slugA)
        && isGenericTemplateSlug(b, slugB)
        && extractDistinctiveTokens(a).isEmpty()
        && extractDistinctiveTokens(b).isEmpty();
  }

  private static boolean hasSameUserIdInUrl(IngatlanItem a, IngatlanItem b) {
    String userIdA = extractUserIdFromUrl(a != null ? a.getUrl() : null);
    String userIdB = extractUserIdFromUrl(b != null ? b.getUrl() : null);
    return userIdA != null && userIdA.equals(userIdB);
  }

  private static String extractUserIdFromUrl(String url) {
    if (url == null || url.isBlank()) {
      return null;
    }
    Matcher matcher = USER_ID_URL_PATTERN.matcher(url);
    return matcher.find() ? matcher.group(1) : null;
  }

  private static boolean isSlugSimilar(String titleA, String titleB) {
    String slugA = SlugUtils.toSlug(titleA);
    String slugB = SlugUtils.toSlug(titleB);
    if (slugA.isBlank() || slugB.isBlank()) {
      return false;
    }
    if (slugA.equals(slugB)) {
      return true;
    }
    boolean contained = slugA.contains(slugB) || slugB.contains(slugA);
    return contained && Math.min(slugA.length(), slugB.length()) >= 12;
  }

  private static boolean hasTokenSimilarity(Set<String> tokensA, Set<String> tokensB) {
    double jaccard = calculateJaccardSimilarity(tokensA, tokensB);
    int minSize = (tokensA != null && tokensB != null) ? Math.min(tokensA.size(), tokensB.size()) : 0;
    if (minSize >= 2 && jaccard >= 0.50) {
      return true;
    }
    return jaccard >= 0.33 && hasDistinctiveKeywordOverlap(tokensA, tokensB);
  }
}
