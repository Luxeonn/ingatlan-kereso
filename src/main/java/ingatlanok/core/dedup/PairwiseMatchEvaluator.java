package ingatlanok.core.dedup;

import java.util.Set;

import ingatlanok.common.CityUtils;
import ingatlanok.core.dedup.LocationConflictDetector.ExtractedLocation;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PropertyType;

/**
 * Evaluates pairwise physical-identity heuristics (area/price tolerances, room compatibility,
 * location/semantic conflicts, and intra-portal spam/variant guards) for cross-portal deduplication.
 */
public final class PairwiseMatchEvaluator {

  private static final double MAX_AREA_TOLERANCE_RATIO = 0.05; // ±5%
  private static final double HOUSE_MAX_AREA_TOLERANCE_RATIO = 0.06; // ±6% for HAZ net/gross
  private static final double MAX_ROOM_DIFFERENCE = 0.5; // within ±0.5 rooms
  private static final double MAX_PRICE_BAND_RATIO = 0.20; // within ±20%
  private static final double MISSING_AREA_MAX_PRICE_DIFF_RATIO = 0.01; // ±1%
  private static final double UNLOCATED_MAX_AREA_DIFF_RATIO = 0.02; // ±2% without location/image
  private static final double UNLOCATED_MAX_PRICE_DIFF_RATIO = 0.01; // ±1% without location/image
  private static final double INTRA_PORTAL_SPAM_MAX_AREA_DIFF_RATIO = 0.01; // ±1%
  private static final double HOUSE_VARIANT_MAX_PRICE_DIFF_RATIO = 0.005; // ±0.5%

  private PairwiseMatchEvaluator() {}

  public static boolean isHeuristicMatch(IngatlanItem a, IngatlanItem b) {
    if (!isBaseCompatible(a, b)) {
      return false;
    }

    boolean bothAreasKnown = ClusterConsolidationHelper.hasPositiveArea(a)
        && ClusterConsolidationHelper.hasPositiveArea(b);
    if (!isAreaAndPriceCompatible(a, b, bothAreasKnown)) {
      return false;
    }

    String normCity = CityUtils.normalizeCityKey(a.getCity());
    ExtractedLocation locA = LocationConflictDetector.extractLocation(a, normCity);
    ExtractedLocation locB = LocationConflictDetector.extractLocation(b, normCity);
    if (hasStructuralConflict(a, b, normCity, locA, locB)
        || hasIntraPortalConflict(a, b, locA, locB, bothAreasKnown)) {
      return false;
    }
    boolean hasAffirmativeLocation = LocationConflictDetector.hasAffirmativeMatch(locA, locB);

    Set<String> tokensA = TextualSimilarityMatcher.extractDistinctiveTokens(a);
    Set<String> tokensB = TextualSimilarityMatcher.extractDistinctiveTokens(b);
    if (TextualSimilarityMatcher.hasDirectTitleConflict(a, b, tokensA, tokensB, hasAffirmativeLocation)
        || !isRoomCountCompatible(a, b, a.getPropertyType(), tokensA, tokensB, hasAffirmativeLocation)) {
      return false;
    }

    if (!bothAreasKnown) {
      return hasCorroboratingMissingAreaEvidence(a, b, tokensA, tokensB, hasAffirmativeLocation);
    }
    if (ClusterConsolidationHelper.hasMatchingImage(a, b) || hasAffirmativeLocation) {
      return true;
    }
    return hasUnlocatedMatchEvidence(a, b, tokensA, tokensB);
  }

  public static boolean isMatchingCity(IngatlanItem a, IngatlanItem b) {
    String cityA = a.getCity();
    String cityB = b.getCity();
    if (cityA == null || cityB == null || cityA.isBlank() || cityB.isBlank()) {
      return false;
    }
    return CityUtils.normalizeCityKey(cityA).equals(CityUtils.normalizeCityKey(cityB));
  }

  private static boolean isBaseCompatible(IngatlanItem a, IngatlanItem b) {
    return a != null && b != null && isMatchingCity(a, b) && isSupportedPropertyType(a, b);
  }

  private static boolean hasIntraPortalConflict(
      IngatlanItem a, IngatlanItem b, ExtractedLocation locA, ExtractedLocation locB, boolean bothAreasKnown) {
    return ClusterConsolidationHelper.isSamePortalDifferentUrl(a, b)
        && !isIntraPortalDuplicate(a, b, locA, locB, bothAreasKnown);
  }

  private static boolean isIntraPortalDuplicate(
      IngatlanItem a, IngatlanItem b, ExtractedLocation locA, ExtractedLocation locB, boolean bothAreasKnown) {
    if (a.isNewConstruction() != b.isNewConstruction() || !isWithinMissingAreaPriceBand(a, b)) {
      return false;
    }
    if (!hasIntraPortalAreaConflict(a, b)
        && (ClusterConsolidationHelper.hasMatchingImage(a, b)
            || TextualSimilarityMatcher.hasIdenticalTitleSlug(a, b))) {
      return true;
    }
    return isIntraPortalHouseVariant(a, b, locA, locB, bothAreasKnown);
  }

  private static boolean isIntraPortalHouseVariant(
      IngatlanItem a, IngatlanItem b, ExtractedLocation locA, ExtractedLocation locB, boolean bothAreasKnown) {
    if (a.getPropertyType() != PropertyType.HAZ || !bothAreasKnown) {
      return false;
    }
    boolean withinNumericTolerance =
        ClusterConsolidationHelper.isWithinPriceRatio(a, b, HOUSE_VARIANT_MAX_PRICE_DIFF_RATIO)
            && ClusterConsolidationHelper.isWithinAreaRatio(a, b, HOUSE_MAX_AREA_TOLERANCE_RATIO)
            && ClusterConsolidationHelper.hasCompatibleRoomCount(a, b, MAX_ROOM_DIFFERENCE);
    return withinNumericTolerance
        && (ClusterConsolidationHelper.hasMatchingImage(a, b)
            || LocationConflictDetector.hasSameStreet(locA, locB)
            || TextualSimilarityMatcher.areBothGenericTemplateTitles(a, b));
  }

  private static boolean hasIntraPortalAreaConflict(IngatlanItem a, IngatlanItem b) {
    return ClusterConsolidationHelper.hasPositiveArea(a)
        && ClusterConsolidationHelper.hasPositiveArea(b)
        && !ClusterConsolidationHelper.isWithinAreaRatio(
            a, b, INTRA_PORTAL_SPAM_MAX_AREA_DIFF_RATIO);
  }

  private static boolean isAreaAndPriceCompatible(IngatlanItem a, IngatlanItem b, boolean bothAreasKnown) {
    if (bothAreasKnown) {
      return isWithinAreaTolerance(a, b) && isWithinPriceBand(a, b);
    }
    return isWithinMissingAreaPriceBand(a, b);
  }

  private static boolean hasStructuralConflict(
      IngatlanItem a, IngatlanItem b, String normCity, ExtractedLocation locA, ExtractedLocation locB) {
    return LocationConflictDetector.hasConflict(locA, locB, normCity)
        || SemanticConflictDetector.hasConflict(a, b);
  }

  private static boolean hasCorroboratingMissingAreaEvidence(
      IngatlanItem a, IngatlanItem b, Set<String> tokensA, Set<String> tokensB, boolean hasAffirmativeLocation) {
    return ClusterConsolidationHelper.hasMatchingImage(a, b)
        || TextualSimilarityMatcher.hasStrongTitleSimilarity(
            a, b, tokensA, tokensB, hasAffirmativeLocation);
  }

  private static boolean isSupportedPropertyType(IngatlanItem a, IngatlanItem b) {
    PropertyType typeA = a.getPropertyType();
    PropertyType typeB = b.getPropertyType();
    if (typeA == null || typeB == null || typeA != typeB) {
      return false;
    }
    return typeA == PropertyType.LAKAS || typeA == PropertyType.HAZ || typeA == PropertyType.TELEK;
  }

  private static boolean isWithinAreaTolerance(IngatlanItem a, IngatlanItem b) {
    double tolerance = a.getPropertyType() == PropertyType.HAZ
        ? HOUSE_MAX_AREA_TOLERANCE_RATIO : MAX_AREA_TOLERANCE_RATIO;
    return ClusterConsolidationHelper.isWithinAreaRatio(a, b, tolerance);
  }

  private static boolean isWithinPriceBand(IngatlanItem a, IngatlanItem b) {
    return ClusterConsolidationHelper.isWithinPriceRatio(a, b, MAX_PRICE_BAND_RATIO);
  }

  private static boolean isWithinMissingAreaPriceBand(IngatlanItem a, IngatlanItem b) {
    return ClusterConsolidationHelper.isWithinPriceRatio(a, b, MISSING_AREA_MAX_PRICE_DIFF_RATIO);
  }

  private static boolean hasUnlocatedMatchEvidence(
      IngatlanItem a, IngatlanItem b, Set<String> tokensA, Set<String> tokensB) {
    if (TextualSimilarityMatcher.hasStrongTitleSimilarity(a, b, tokensA, tokensB, false)) {
      return true;
    }
    if (!ClusterConsolidationHelper.hasCompatibleRoomCount(a, b, MAX_ROOM_DIFFERENCE)) {
      return false;
    }
    boolean strictMatch =
        ClusterConsolidationHelper.isWithinPriceRatio(a, b, UNLOCATED_MAX_PRICE_DIFF_RATIO)
            && ClusterConsolidationHelper.isWithinAreaRatio(a, b, UNLOCATED_MAX_AREA_DIFF_RATIO);
    boolean houseVariantMatch = a.getPropertyType() == PropertyType.HAZ
        && ClusterConsolidationHelper.isWithinPriceRatio(a, b, HOUSE_VARIANT_MAX_PRICE_DIFF_RATIO)
        && ClusterConsolidationHelper.isWithinAreaRatio(a, b, HOUSE_MAX_AREA_TOLERANCE_RATIO);
    return strictMatch || houseVariantMatch;
  }

  private static boolean isRoomCountCompatible(
      IngatlanItem a,
      IngatlanItem b,
      PropertyType type,
      Set<String> tokensA,
      Set<String> tokensB,
      boolean hasAffirmativeLocation) {
    Double roomsA = a.getDetailsInfo() != null ? a.getDetailsInfo().roomCount() : null;
    Double roomsB = b.getDetailsInfo() != null ? b.getDetailsInfo().roomCount() : null;
    if (roomsA == null || roomsB == null || roomsA <= 0 || roomsB <= 0) {
      return true;
    }
    double diff = Math.abs(roomsA - roomsB);
    if (type == PropertyType.TELEK) {
      return diff < 1.0;
    }
    if (diff <= MAX_ROOM_DIFFERENCE) {
      return true;
    }
    return isAdaptiveRoomDiffAllowed(a, b, diff, tokensA, tokensB, hasAffirmativeLocation);
  }

  private static boolean isAdaptiveRoomDiffAllowed(
      IngatlanItem a,
      IngatlanItem b,
      double roomDiff,
      Set<String> tokensA,
      Set<String> tokensB,
      boolean hasAffirmativeLocation) {
    if (!isAdaptiveAreaEligible(a, b, roomDiff)) {
      return false;
    }
    boolean multiGen =
        TextualSimilarityMatcher.isMultiGeneration(a)
            || TextualSimilarityMatcher.isMultiGeneration(b);
    boolean keywordOverlap =
        TextualSimilarityMatcher.hasDistinctiveKeywordOverlap(tokensA, tokensB);
    return multiGen || keywordOverlap || hasAffirmativeLocation;
  }

  private static boolean isAdaptiveAreaEligible(IngatlanItem a, IngatlanItem b, double roomDiff) {
    Double areaA = a.getDetailsInfo() != null ? a.getDetailsInfo().areaSquareMeters() : null;
    Double areaB = b.getDetailsInfo() != null ? b.getDetailsInfo().areaSquareMeters() : null;
    if (areaA == null || areaB == null) {
      return false;
    }
    double maxArea = Math.max(areaA, areaB);
    double areaDiffRatio = Math.abs(areaA - areaB) / maxArea;
    boolean largeOrHouse = a.getPropertyType() == PropertyType.HAZ || maxArea >= 120.0;
    double maxAllowedDiff = maxArea >= 150.0 ? 3.0 : 2.5;
    return largeOrHouse && roomDiff <= maxAllowedDiff && areaDiffRatio <= 0.035;
  }
}
