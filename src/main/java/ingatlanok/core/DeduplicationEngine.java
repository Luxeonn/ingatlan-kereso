package ingatlanok.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ingatlanok.common.CityUtils;
import ingatlanok.core.dedup.ClusterConsolidationHelper;
import ingatlanok.core.dedup.ClusterMatchScorer;
import ingatlanok.core.dedup.DedupOverrides;
import ingatlanok.core.dedup.LocationConflictDetector;
import ingatlanok.core.dedup.LocationConflictDetector.ExtractedLocation;
import ingatlanok.core.dedup.SemanticConflictDetector;
import ingatlanok.core.dedup.TextualSimilarityMatcher;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.ListingStatus;
import ingatlanok.model.PropertyType;
import ingatlanok.model.SourceListing;
import ingatlanok.persistence.DedupOverrideStorage;
import ingatlanok.persistence.ListingStorage;

@Service
public class DeduplicationEngine {

  private static final double MAX_AREA_TOLERANCE_RATIO = 0.05; // ±5%
  private static final double HOUSE_MAX_AREA_TOLERANCE_RATIO = 0.06; // ±6% for HAZ net/gross
  private static final double MAX_ROOM_DIFFERENCE = 0.5; // within ±0.5 rooms
  private static final double MAX_PRICE_BAND_RATIO = 0.20; // within ±20%
  private static final double MISSING_AREA_MAX_PRICE_DIFF_RATIO = 0.01; // ±1%
  private static final double UNLOCATED_MAX_AREA_DIFF_RATIO = 0.02; // ±2% without location/image
  private static final double UNLOCATED_MAX_PRICE_DIFF_RATIO = 0.01; // ±1% without location/image
  private static final double INTRA_PORTAL_SPAM_MAX_AREA_DIFF_RATIO = 0.01; // ±1%
  private static final double HOUSE_VARIANT_MAX_PRICE_DIFF_RATIO = 0.005; // ±0.5%

  private final ListingStorage listingStorage;
  private final DedupOverrideStorage dedupOverrideStorage;

  public DeduplicationEngine(ListingStorage listingStorage) {
    this(listingStorage, null);
  }

  @Autowired
  public DeduplicationEngine(
      ListingStorage listingStorage,
      @Autowired(required = false) DedupOverrideStorage dedupOverrideStorage) {
    this.listingStorage = listingStorage;
    this.dedupOverrideStorage = dedupOverrideStorage;
  }

  /**
   * Fast O(1) URL deduplication preserving iteration order.
   */
  public List<IngatlanItem> deduplicateByUrl(Collection<IngatlanItem> items) {
    if (items == null || items.isEmpty()) {
      return Collections.emptyList();
    }
    Map<String, IngatlanItem> unique = new LinkedHashMap<>();
    for (IngatlanItem item : items) {
      if (item != null && item.getUrl() != null && !item.getUrl().isBlank()) {
        unique.putIfAbsent(item.getUrl().trim(), item);
      }
    }
    return new ArrayList<>(unique.values());
  }

  /**
   * Multi-signal cross-portal clustering and consolidation.
   */
  public List<IngatlanItem> deduplicateMultiSignal(Collection<IngatlanItem> items) {
    if (items == null || items.isEmpty()) {
      return Collections.emptyList();
    }

    // Stage 1: Fast O(1) URL deduplication
    List<IngatlanItem> stage1Items = deduplicateByUrl(items);
    if (stage1Items.isEmpty()) {
      return Collections.emptyList();
    }

    DedupOverrides overrides = loadOverrides();

    // Stage 2: Score-ordered cross-portal multi-signal clustering (order-independent best match)
    List<List<IngatlanItem>> clusters =
        ClusterMatchScorer.buildBestMatchClusters(
            stage1Items, (a, b) -> isMatch(a, b, overrides), overrides);

    // Stage 3: Consolidation into grouped IngatlanItem
    List<IngatlanItem> consolidatedItems = new ArrayList<>(clusters.size());
    for (List<IngatlanItem> cluster : clusters) {
      consolidatedItems.add(consolidateCluster(cluster, overrides));
    }

    return consolidatedItems;
  }

  private DedupOverrides loadOverrides() {
    if (dedupOverrideStorage == null) {
      return DedupOverrides.EMPTY;
    }
    return DedupOverrides.of(dedupOverrideStorage.getAllOverrides());
  }

  public List<IngatlanItem> deduplicate(Collection<IngatlanItem> items) {
    return deduplicateMultiSignal(items);
  }

  public boolean isMatch(IngatlanItem a, IngatlanItem b) {
    return isMatch(a, b, DedupOverrides.EMPTY);
  }

  public boolean isMatch(IngatlanItem a, IngatlanItem b, DedupOverrides overrides) {
    if (a == null || b == null) {
      return false;
    }
    DedupOverrides effectiveOverrides = overrides != null ? overrides : DedupOverrides.EMPTY;
    if (effectiveOverrides.isForceSplit(a.getUrl(), b.getUrl())) {
      return false;
    }
    if (effectiveOverrides.isForceMerge(a.getUrl(), b.getUrl())) {
      return isMatchingCity(a, b);
    }
    return isHeuristicMatch(a, b);
  }

  private boolean isHeuristicMatch(IngatlanItem a, IngatlanItem b) {
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

  private boolean isBaseCompatible(IngatlanItem a, IngatlanItem b) {
    return a != null && b != null && isMatchingCity(a, b) && isSupportedPropertyType(a, b);
  }

  private boolean hasIntraPortalConflict(
      IngatlanItem a, IngatlanItem b, ExtractedLocation locA, ExtractedLocation locB, boolean bothAreasKnown) {
    return ClusterConsolidationHelper.isSamePortalDifferentUrl(a, b)
        && !isIntraPortalDuplicate(a, b, locA, locB, bothAreasKnown);
  }

  private boolean isIntraPortalDuplicate(
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

  private boolean isIntraPortalHouseVariant(
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

  private boolean hasIntraPortalAreaConflict(IngatlanItem a, IngatlanItem b) {
    return ClusterConsolidationHelper.hasPositiveArea(a)
        && ClusterConsolidationHelper.hasPositiveArea(b)
        && !ClusterConsolidationHelper.isWithinAreaRatio(
            a, b, INTRA_PORTAL_SPAM_MAX_AREA_DIFF_RATIO);
  }

  private boolean isAreaAndPriceCompatible(IngatlanItem a, IngatlanItem b, boolean bothAreasKnown) {
    if (bothAreasKnown) {
      return isWithinAreaTolerance(a, b) && isWithinPriceBand(a, b);
    }
    return isWithinMissingAreaPriceBand(a, b);
  }

  private boolean hasStructuralConflict(
      IngatlanItem a, IngatlanItem b, String normCity, ExtractedLocation locA, ExtractedLocation locB) {
    return LocationConflictDetector.hasConflict(locA, locB, normCity)
        || SemanticConflictDetector.hasConflict(a, b);
  }

  private boolean hasCorroboratingMissingAreaEvidence(
      IngatlanItem a, IngatlanItem b, Set<String> tokensA, Set<String> tokensB, boolean hasAffirmativeLocation) {
    return ClusterConsolidationHelper.hasMatchingImage(a, b)
        || TextualSimilarityMatcher.hasStrongTitleSimilarity(
            a, b, tokensA, tokensB, hasAffirmativeLocation);
  }

  private boolean isMatchingCity(IngatlanItem a, IngatlanItem b) {
    String cityA = a.getCity();
    String cityB = b.getCity();
    if (cityA == null || cityB == null || cityA.isBlank() || cityB.isBlank()) {
      return false;
    }
    return CityUtils.normalizeCityKey(cityA).equals(CityUtils.normalizeCityKey(cityB));
  }

  private boolean isSupportedPropertyType(IngatlanItem a, IngatlanItem b) {
    PropertyType typeA = a.getPropertyType();
    PropertyType typeB = b.getPropertyType();
    if (typeA == null || typeB == null || typeA != typeB) {
      return false;
    }
    return typeA == PropertyType.LAKAS || typeA == PropertyType.HAZ || typeA == PropertyType.TELEK;
  }

  private boolean isWithinAreaTolerance(IngatlanItem a, IngatlanItem b) {
    double tolerance = a.getPropertyType() == PropertyType.HAZ
        ? HOUSE_MAX_AREA_TOLERANCE_RATIO : MAX_AREA_TOLERANCE_RATIO;
    return ClusterConsolidationHelper.isWithinAreaRatio(a, b, tolerance);
  }

  private boolean isWithinPriceBand(IngatlanItem a, IngatlanItem b) {
    return ClusterConsolidationHelper.isWithinPriceRatio(a, b, MAX_PRICE_BAND_RATIO);
  }

  private boolean isWithinMissingAreaPriceBand(IngatlanItem a, IngatlanItem b) {
    return ClusterConsolidationHelper.isWithinPriceRatio(a, b, MISSING_AREA_MAX_PRICE_DIFF_RATIO);
  }

  private boolean hasUnlocatedMatchEvidence(
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

  private boolean isRoomCountCompatible(
      IngatlanItem a,
      IngatlanItem b,
      PropertyType type,
      Set<String> tokensA,
      Set<String> tokensB,
      boolean hasAffirmativeLocation) {
    Double roomsA = a.getDetailsInfo() != null ? a.getDetailsInfo().getRoomCount() : null;
    Double roomsB = b.getDetailsInfo() != null ? b.getDetailsInfo().getRoomCount() : null;
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

  private boolean isAdaptiveRoomDiffAllowed(
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

  private boolean isAdaptiveAreaEligible(IngatlanItem a, IngatlanItem b, double roomDiff) {
    Double areaA = a.getDetailsInfo() != null ? a.getDetailsInfo().getAreaSquareMeters() : null;
    Double areaB = b.getDetailsInfo() != null ? b.getDetailsInfo().getAreaSquareMeters() : null;
    if (areaA == null || areaB == null) {
      return false;
    }
    double maxArea = Math.max(areaA, areaB);
    double areaDiffRatio = Math.abs(areaA - areaB) / maxArea;
    boolean largeOrHouse = a.getPropertyType() == PropertyType.HAZ || maxArea >= 120.0;
    double maxAllowedDiff = maxArea >= 150.0 ? 3.0 : 2.5;
    return largeOrHouse && roomDiff <= maxAllowedDiff && areaDiffRatio <= 0.035;
  }

  private IngatlanItem consolidateCluster(List<IngatlanItem> cluster, DedupOverrides overrides) {
    IngatlanItem primary = ClusterConsolidationHelper.findLowestActivePriceSource(cluster);

    ClusterConsolidationHelper.adoptMissingDetailsFromCluster(primary, cluster);

    Double clusterArea = primary != null && primary.getDetailsInfo() != null
        ? primary.getDetailsInfo().getAreaSquareMeters()
        : null;

    List<SourceListing> sourceListings = cluster.stream()
        .filter(Objects::nonNull)
        .map(item -> {
          SourceListing sl = toSourceListing(item, clusterArea);
          if (sl != null && overrides.hasAnySplit(item.getUrl())) {
            sl.setManuallySplit(true);
          }
          return sl;
        })
        .sorted(Comparator.comparing(
            SourceListing::getPriceHuf,
            Comparator.nullsLast(Long::compareTo)))
        .toList();

    primary.setSourcesCount(cluster.size());
    primary.setSourceListings(sourceListings);
    primary.setManuallyMerged(overrides.hasAnyForceMergePair(cluster));
    primary.setManuallySplit(overrides.hasAnySplitMember(cluster));

    // If primary has no image, adopt image from any cluster member that has one
    if (primary.getImageUrl() == null || primary.getImageUrl().isBlank()) {
      for (IngatlanItem member : cluster) {
        if (member != null && member.getImageUrl() != null && !member.getImageUrl().isBlank()) {
          primary.setImageUrl(member.getImageUrl());
          break;
        }
      }
    }

    boolean anyNew = cluster.stream().filter(Objects::nonNull).anyMatch(IngatlanItem::isNew);
    boolean allViewed = cluster.stream().filter(Objects::nonNull).allMatch(IngatlanItem::isViewed);
    boolean anyNewConstruction =
        cluster.stream().filter(Objects::nonNull).anyMatch(IngatlanItem::isNewConstruction);
    boolean anyHidden =
        cluster.stream().filter(Objects::nonNull).anyMatch(IngatlanItem::isHidden);
    primary.setNew(anyNew);
    primary.setViewed(allViewed);
    primary.setNewConstruction(anyNewConstruction);
    primary.setHidden(anyHidden);

    return primary;
  }

  public SourceListing toSourceListing(IngatlanItem item) {
    return toSourceListing(item, null);
  }

  public SourceListing toSourceListing(IngatlanItem item, Double fallbackArea) {
    if (item == null) {
      return null;
    }
    String portal = resolveSourcePortal(item);
    Long priceHuf = item.getPriceInfo() != null ? item.getPriceInfo().getAmountHuf() : null;
    Long pricePerM2 = ClusterConsolidationHelper.resolveSourcePricePerM2(item, priceHuf, fallbackArea);

    return SourceListing.builder()
        .portal(portal)
        .externalListingId(item.getId())
        .url(item.getUrl())
        .title(item.getTitle())
        .imageUrl(item.getImageUrl())
        .firstSeenAt(item.getDiscoveredAt())
        .lastSeenAt(item.getDiscoveredAt())
        .status(item.getStatus() != null ? item.getStatus() : ListingStatus.ACTIVE)
        .priceHuf(priceHuf)
        .pricePerM2(pricePerM2)
        .build();
  }

  private String resolveSourcePortal(IngatlanItem item) {
    String portal = item.getPortal();
    if ((portal == null || portal.isBlank()) && item.getSource() != null) {
      portal = item.getSource().getPortalName();
    }
    return (portal != null && !portal.isBlank()) ? portal : "Ismeretlen";
  }

  public void applyNewStatus(String city, Collection<IngatlanItem> items, boolean isFirstSearch) {
    if (items == null || items.isEmpty()) {
      return;
    }

    for (IngatlanItem item : items) {
      if (item == null || item.getUrl() == null) {
        continue;
      }

      if (isFirstSearch) {
        item.setNew(false);
      } else if (listingStorage != null) {
        boolean isNew = listingStorage.isNewUrl(city, item.getUrl());
        item.setNew(isNew);
      }
    }
  }
}
