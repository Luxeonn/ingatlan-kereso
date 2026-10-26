package ingatlanok.core.dedup;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ingatlanok.common.SlugUtils;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PropertyType;

/**
 * Detects semantic, architectural, and physical condition conflicts between two listings.
 */
public final class SemanticConflictDetector {

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

    // 1. Condition conflict: New construction vs Needs renovation / Demolition / Kádárkocka
    boolean newA = isNewConstruction(a, slugA);
    boolean newB = isNewConstruction(b, slugB);
    boolean renoA = isNeedsRenovationOrDemolition(slugA);
    boolean renoB = isNeedsRenovationOrDemolition(slugB);

    if ((newA && renoB) || (newB && renoA)) {
      return true;
    }

    // 2. Flat structural type conflict: Panel vs Brick
    if (a.getPropertyType() == PropertyType.LAKAS && b.getPropertyType() == PropertyType.LAKAS) {
      boolean panelA = isPanelFlat(slugA);
      boolean panelB = isPanelFlat(slugB);
      boolean brickA = isBrickFlat(slugA);
      boolean brickB = isBrickFlat(slugB);

      if ((panelA && brickB) || (panelB && brickA)) {
        return true;
      }

      // 3. Floor level conflict: Ground floor vs Top floor / Penthouse
      boolean groundA = isGroundFloor(slugA);
      boolean groundB = isGroundFloor(slugB);
      boolean topA = isTopFloor(slugA);
      boolean topB = isTopFloor(slugB);

      if ((groundA && topB) || (groundB && topA)) {
        return true;
      }
    }

    // 4. House architectural conflict: Farmstead (tanya) vs Twin/Townhouse (ikerház/sorház)
    if (a.getPropertyType() == PropertyType.HAZ && b.getPropertyType() == PropertyType.HAZ) {
      boolean tanyaA = isTanya(slugA);
      boolean tanyaB = isTanya(slugB);
      boolean twinA = isTwinOrRowHouse(slugA);
      boolean twinB = isTwinOrRowHouse(slugB);

      if ((tanyaA && twinB) || (tanyaB && twinA)) {
        return true;
      }

      // 5. Lot size discrepancy if both explicitly state lot area
      Double lotA = extractLotSize(textA);
      Double lotB = extractLotSize(textB);
      if (lotA != null && lotB != null && lotA > 0 && lotB > 0) {
        double maxLot = Math.max(lotA, lotB);
        double lotDiffRatio = Math.abs(lotA - lotB) / maxLot;
        if (lotDiffRatio > 0.25) {
          return true;
        }
      }
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
    return slug.contains("uj-epites") || slug.contains("ujepites")
        || slug.contains("epites-alatt") || slug.contains("kulcsrakes")
        || slug.contains("most-epulo");
  }

  private static boolean isNeedsRenovationOrDemolition(String slug) {
    return slug.contains("felujitando") || slug.contains("erosen-felujitando")
        || slug.contains("bontando") || slug.contains("bontasra")
        || slug.contains("kadarkocka") || slug.contains("valyog")
        || slug.contains("bontando-haz");
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
    return slug.contains("ikerhaz") || slug.contains("iker-haz")
        || slug.contains("sorhaz") || slug.contains("sor-haz");
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
