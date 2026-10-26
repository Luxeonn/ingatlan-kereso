package ingatlanok.core.dedup;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import ingatlanok.common.SlugUtils;
import ingatlanok.model.IngatlanItem;

/**
 * Textual tokenization, multi-generation detection, and semantic similarity evaluation.
 */
public final class TextualSimilarityMatcher {

  private static final Set<String> STOPWORDS =
      Set.of(
          "elado", "kiado", "ingatlan", "ingatlanok", "haz", "lakas", "telek",
          "otthon", "centrum", "duna", "house", "jofogas", "koltozzbe", "ingatlanbazar",
          "hirdetes", "forint", "alatt", "egy", "ket", "harom", "szoba", "szobas",
          "nagyon", "kivalo", "szep", "minden", "ahol", "ami", "itt", "van",
          "meg", "csak", "mar", "vagy", "hogy", "mert", "akarmilyen");

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

    String[] parts = stripped.split("[^a-z]+");
    Set<String> tokens = new HashSet<>();
    for (String part : parts) {
      if (part == null || part.isBlank()) {
        continue;
      }
      String clean = part.trim();
      if (clean.length() >= 3 && !STOPWORDS.contains(clean)) {
        tokens.add(clean);
      }
    }
    return tokens;
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

    return slug.contains("ketgeneracios") || slug.contains("ket-generacios")
        || slug.contains("2-generacios") || slug.contains("2generacios")
        || slug.contains("ket-epulet") || slug.contains("2-epulet")
        || slug.contains("ket-lakresz") || slug.contains("2-lakresz")
        || slug.contains("osszekoltoz");
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
}
