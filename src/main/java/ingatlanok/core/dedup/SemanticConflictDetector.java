package ingatlanok.core.dedup;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ingatlanok.common.SlugUtils;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PropertyType;

/**
 * Detects semantic, architectural, and physical condition conflicts between two listings.
 */
public final class SemanticConflictDetector {

  private static final Set<String> NEW_CONSTRUCTION_KEYWORDS = Set.of(
      "uj-epites", "ujepites", "epites-alatt", "kulcsrakes", "most-epulo"
  );

  private static final Set<String> RENOVATION_OR_DEMOLITION_KEYWORDS = Set.of(
      "felujitando", "erosen-felujitando", "bontando", "bontasra",
      "kadarkocka", "valyog", "bontando-haz"
  );

  private static final Pattern LOT_SIZE_PATTERN =
      Pattern.compile(
          "(?iuU)(?:telek|telken|telek méret|telekméret|terület)[:\\s]*(\\d+[\\s\\d]*)\\s*(?:m2|m²|nm)\\b"
              + "|(\\d+[\\s\\d]*)\\s*(?:m2|m²|nm)\\s*(?:-es|-os|-as)?\\s*telk");

  private SemanticConflictDetector() {}

  /**
   * Evaluates whether two listings have mutually exclusive semantic or structural properties.
   */
  public static boolean hasConflict(IngatlanItem a, IngatlanItem b) {
    if (a == null || b == null) {
      return false;
    }

    String textA = getCombinedText(a);
    String textB = getCombinedText(b);
    String slugA = SlugUtils.toSlug(textA);
    String slugB = SlugUtils.toSlug(textB);

    return hasConditionConflict(a, b, slugA, slugB)
        || hasFlatConflict(a, b, slugA, slugB)
        || hasHouseConflict(a, b, slugA, slugB, textA, textB);
  }

  private static boolean hasConditionConflict(
      IngatlanItem a, IngatlanItem b, String slugA, String slugB) {
    boolean newA = isNewConstruction(a, slugA);
    boolean newB = isNewConstruction(b, slugB);
    boolean renoA = isNeedsRenovationOrDemolition(slugA);
    boolean renoB = isNeedsRenovationOrDemolition(slugB);
    return (newA && renoB) || (newB && renoA);
  }

  private static boolean hasFlatConflict(
      IngatlanItem a, IngatlanItem b, String slugA, String slugB) {
    if (a.getPropertyType() != PropertyType.LAKAS || b.getPropertyType() != PropertyType.LAKAS) {
      return false;
    }
    boolean panelA = isPanelFlat(slugA);
    boolean panelB = isPanelFlat(slugB);
    boolean brickA = isBrickFlat(slugA);
    boolean brickB = isBrickFlat(slugB);
    if ((panelA && brickB) || (panelB && brickA)) {
      return true;
    }
    boolean groundA = isGroundFloor(slugA);
    boolean groundB = isGroundFloor(slugB);
    boolean topA = isTopFloor(slugA);
    boolean topB = isTopFloor(slugB);
    return (groundA && topB) || (groundB && topA);
  }

  private static boolean hasHouseConflict(
      IngatlanItem a, IngatlanItem b, String slugA, String slugB, String textA, String textB) {
    if (a.getPropertyType() != PropertyType.HAZ || b.getPropertyType() != PropertyType.HAZ) {
      return false;
    }
    if (hasHouseSubtypeConflict(slugA, slugB) || hasHouseNewConstructionConflict(a, b, slugA, slugB)) {
      return true;
    }
    return hasLotDiscrepancy(textA, textB);
  }

  private static boolean hasHouseSubtypeConflict(String slugA, String slugB) {
    boolean tanyaA = isTanya(slugA);
    boolean tanyaB = isTanya(slugB);
    boolean rowA = isRowHouse(slugA);
    boolean rowB = isRowHouse(slugB);
    boolean twinA = isTwinHouse(slugA);
    boolean twinB = isTwinHouse(slugB);
    boolean detachedA = isDetachedHouse(slugA);
    boolean detachedB = isDetachedHouse(slugB);
    boolean attachedA = rowA || twinA;
    boolean attachedB = rowB || twinB;

    return hasTanyaMismatch(tanyaA, tanyaB, attachedA, attachedB, detachedA, detachedB)
        || hasAttachedMismatch(rowA, rowB, twinA, twinB)
        || hasDetachedMismatch(attachedA, attachedB, detachedA, detachedB);
  }

  private static boolean hasAttachedMismatch(
      boolean rowA, boolean rowB, boolean twinA, boolean twinB) {
    return (rowA && twinB) || (rowB && twinA);
  }

  private static boolean hasDetachedMismatch(
      boolean attachedA, boolean attachedB, boolean detachedA, boolean detachedB) {
    return (attachedA && detachedB) || (attachedB && detachedA);
  }

  private static boolean hasTanyaMismatch(
      boolean tanyaA,
      boolean tanyaB,
      boolean attachedA,
      boolean attachedB,
      boolean detachedA,
      boolean detachedB) {
    boolean nonTanyaA = attachedA || detachedA;
    boolean nonTanyaB = attachedB || detachedB;
    return (tanyaA && nonTanyaB) || (tanyaB && nonTanyaA);
  }

  private static boolean hasHouseNewConstructionConflict(
      IngatlanItem a, IngatlanItem b, String slugA, String slugB) {
    boolean newA = isNewConstruction(a, slugA);
    boolean newB = isNewConstruction(b, slugB);
    if (newA == newB) {
      return false;
    }
    boolean attachedA = isTwinOrRowHouse(slugA);
    boolean attachedB = isTwinOrRowHouse(slugB);
    return attachedA != attachedB || a.isNewConstruction() != b.isNewConstruction();
  }

  private static boolean hasLotDiscrepancy(String textA, String textB) {
    Double lotA = extractLotSize(textA);
    Double lotB = extractLotSize(textB);
    if (lotA != null && lotB != null && lotA > 0 && lotB > 0) {
      double maxLot = Math.max(lotA, lotB);
      double lotDiffRatio = Math.abs(lotA - lotB) / maxLot;
      return lotDiffRatio > 0.25;
    }
    return false;
  }

  private static String getCombinedText(IngatlanItem item) {
    String title = item.getTitle() != null ? item.getTitle() : "";
    String details = item.getDetails() != null ? item.getDetails() : "";
    return title + " " + details;
  }

  private static boolean isNewConstruction(IngatlanItem item, String slug) {
    if (item.isNewConstruction()) {
      return true;
    }
    return NEW_CONSTRUCTION_KEYWORDS.stream().anyMatch(slug::contains);
  }

  private static boolean isNeedsRenovationOrDemolition(String slug) {
    return RENOVATION_OR_DEMOLITION_KEYWORDS.stream().anyMatch(slug::contains);
  }

  private static boolean isPanelFlat(String slug) {
    return slug.contains("panellakas") || slug.contains("panel-lakas")
        || slug.contains("panelprogram") || slug.contains("tizemeletes");
  }

  private static boolean isBrickFlat(String slug) {
    return slug.contains("teglalakas") || slug.contains("tegla-lakas")
        || slug.contains("tegla-epites") || slug.contains("polgari-lakas");
  }

  private static boolean isGroundFloor(String slug) {
    return slug.contains("foldszint") || slug.contains("foldszinti") || slug.contains("fszt");
  }

  private static boolean isTopFloor(String slug) {
    return slug.contains("tetoter") || slug.contains("tetoteri")
        || slug.contains("penthouse") || slug.contains("legfelso-emelet");
  }

  private static boolean isTanya(String slug) {
    return slug.contains("tanya") || slug.contains("birtok") || slug.contains("major");
  }

  private static boolean isTwinOrRowHouse(String slug) {
    return isTwinHouse(slug) || isRowHouse(slug);
  }

  private static boolean isTwinHouse(String slug) {
    return slug.contains("ikerhaz")
        || slug.contains("iker-haz")
        || slug.contains("iker-csaladi")
        || slug.contains("ikercsaladi");
  }

  private static boolean isRowHouse(String slug) {
    return slug.contains("sorhaz") || slug.contains("sor-haz");
  }

  private static boolean isDetachedHouse(String slug) {
    if (isTwinOrRowHouse(slug)) {
      return false;
    }
    return slug.contains("onallo-haz")
        || slug.contains("onallo-csaladi")
        || slug.contains("csaladi-haz")
        || slug.contains("csaladihaz");
  }

  private static Double extractLotSize(String text) {
    if (text == null || text.isBlank()) {
      return null;
    }
    Matcher m = LOT_SIZE_PATTERN.matcher(text);
    if (m.find()) {
      String numStr = m.group(1) != null ? m.group(1) : m.group(2);
      if (numStr != null) {
        try {
          String clean = numStr.replaceAll("\\s+", "");
          return Double.parseDouble(clean);
        } catch (NumberFormatException ignored) {
          // Fall through
        }
      }
    }
    return null;
  }
}
