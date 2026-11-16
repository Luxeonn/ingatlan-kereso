package ingatlanok.core.dedup;

import java.util.Locale;
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

  private static final int MAX_SUPPORTED_FLOOR = 30;

  private static final Pattern FLOOR_NUMBER_PATTERN =
      Pattern.compile("(?iuU)(?<!\\d)(\\d{1,2})\\s*\\.?\\s*(?:emelet(?:en|i)?\\b|em\\.(?!\\w)|em\\b)");

  private static final Pattern GROUND_FLOOR_PATTERN =
      Pattern.compile("(?iuU)\\b(?:foldszint(?:en|i)?|fszt\\.?)(?!\\w)");

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
    if (hasExplicitFloorConflict(a, b)) {
      return true;
    }
    boolean groundA = isGroundFloor(slugA);
    boolean groundB = isGroundFloor(slugB);
    boolean topA = isTopFloor(slugA);
    boolean topB = isTopFloor(slugB);
    return (groundA && topB) || (groundB && topA);
  }

  private static boolean hasExplicitFloorConflict(IngatlanItem a, IngatlanItem b) {
    Integer floorA = extractFloorNumber(a);
    Integer floorB = extractFloorNumber(b);
    return floorA != null && floorB != null && !floorA.equals(floorB);
  }

  /**
   * Extracts an explicit apartment floor number (0 for ground floor, 1..30 for upper floors)
   * while ignoring building story counts such as "4 emeletes".
   */
  public static Integer extractFloorNumber(IngatlanItem item) {
    if (item == null) {
      return null;
    }
    String rawDetails = item.getDetailsInfo() != null && item.getDetailsInfo().getRawText() != null
        ? item.getDetailsInfo().getRawText()
        : "";
    String combined = getCombinedText(item) + " " + rawDetails;
    String normalized = SlugUtils.stripAccents(combined.toLowerCase(Locale.ROOT));

    Matcher matcher = FLOOR_NUMBER_PATTERN.matcher(normalized);
    if (matcher.find()) {
      return parseBoundedFloor(matcher.group(1));
    }
    if (GROUND_FLOOR_PATTERN.matcher(normalized).find()) {
      return 0;
    }
    return null;
  }

  private static Integer parseBoundedFloor(String rawNumber) {
    try {
      int floor = Integer.parseInt(rawNumber);
      return (floor >= 1 && floor <= MAX_SUPPORTED_FLOOR) ? floor : null;
    } catch (NumberFormatException ignored) {
      return null;
    }
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
    boolean rowA = isRowHouse(slugA);
    boolean rowB = isRowHouse(slugB);
    boolean twinA = isTwinHouse(slugA);
    boolean twinB = isTwinHouse(slugB);
    boolean detachedA = isDetachedHouse(slugA);
    boolean detachedB = isDetachedHouse(slugB);
    boolean attachedA = rowA || twinA;
    boolean attachedB = rowB || twinB;

    return hasTanyaMismatch(slugA, slugB, attachedA, attachedB, detachedA, detachedB)
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
      String slugA,
      String slugB,
      boolean attachedA,
      boolean attachedB,
      boolean detachedA,
      boolean detachedB) {
    boolean tanyaA = isTanya(slugA);
    boolean tanyaB = isTanya(slugB);
    boolean nonTanyaA = attachedA || detachedA || isGenericFamilyHouse(slugA);
    boolean nonTanyaB = attachedB || detachedB || isGenericFamilyHouse(slugB);
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
    String location = item.getLocation() != null ? item.getLocation() : "";
    String details = item.getDetails() != null ? item.getDetails() : "";
    return title + " " + location + " " + details;
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

  private static boolean isGenericFamilyHouse(String slug) {
    return slug.contains("csaladi-haz") || slug.contains("csaladihaz");
  }

  private static boolean isDetachedHouse(String slug) {
    if (isTwinOrRowHouse(slug)) {
      return false;
    }
    return slug.contains("onallo-haz")
        || slug.contains("onallo-csaladi")
        || slug.contains("kulonallo-haz")
        || slug.contains("kulonallo-csaladi");
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
