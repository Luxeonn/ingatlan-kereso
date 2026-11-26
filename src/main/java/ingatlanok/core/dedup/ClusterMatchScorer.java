package ingatlanok.core.dedup;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

import ingatlanok.common.CityUtils;
import ingatlanok.core.dedup.LocationConflictDetector.ExtractedLocation;
import ingatlanok.model.DedupOverride.UrlPair;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.ListingStatus;
import ingatlanok.model.PropertyType;

/**
 * Scores pairwise listing matches and performs score-ordered best-match clustering
 * so cross-portal deduplication prioritizes the strongest physical identity signals
 * over arbitrary iteration order.
 */
public final class ClusterMatchScorer {

  private static final double MAX_PRICE_BAND_RATIO = 0.20;
  private static final double MAX_AREA_TOLERANCE_RATIO = 0.05;
  private static final double HOUSE_MAX_AREA_TOLERANCE_RATIO = 0.06;

  private static final double WEIGHT_PRICE = 40.0;
  private static final double WEIGHT_STREET = 30.0;
  private static final double WEIGHT_LOCATION = 20.0;
  private static final double WEIGHT_IMAGE = 25.0;
  private static final double WEIGHT_AREA = 15.0;
  private static final double WEIGHT_ROOMS = 10.0;
  private static final double WEIGHT_TITLE = 10.0;
  private static final double WEIGHT_BOTH_ACTIVE = 5.0;
  private static final double WEIGHT_INTRA_PORTAL_PRIORITY = 15.0;
  private static final double FORCE_MERGE_SCORE = 1_000_000.0;

  private ClusterMatchScorer() {}

  private record MatchEdge(int indexA, int indexB, double score) {}

  private record BucketKey(String normalizedCity, PropertyType propertyType) {}

  private record ForceMergeContext(
      List<IngatlanItem> items,
      BiPredicate<IngatlanItem, IngatlanItem> matcher,
      Set<Long> addedPairs,
      List<MatchEdge> edges) {}

  /**
   * Groups compatible items into clusters by merging highest-scoring pairs first
   * while enforcing pairwise compatibility across every member of merged clusters.
   */
  public static List<List<IngatlanItem>> buildBestMatchClusters(
      List<IngatlanItem> items, BiPredicate<IngatlanItem, IngatlanItem> matcher) {
    return buildBestMatchClusters(items, matcher, DedupOverrides.EMPTY);
  }

  public static List<List<IngatlanItem>> buildBestMatchClusters(
      List<IngatlanItem> items,
      BiPredicate<IngatlanItem, IngatlanItem> matcher,
      DedupOverrides overrides) {
    if (items == null || items.isEmpty()) {
      return List.of();
    }
    DedupOverrides effectiveOverrides = overrides != null ? overrides : DedupOverrides.EMPTY;
    int count = items.size();
    List<List<Integer>> clusterIndices = new ArrayList<>(count);
    int[] clusterOf = new int[count];
    for (int i = 0; i < count; i++) {
      List<Integer> singleton = new ArrayList<>(1);
      singleton.add(i);
      clusterIndices.add(singleton);
      clusterOf[i] = i;
    }

    List<MatchEdge> edges = collectScoredMatchEdges(items, matcher, effectiveOverrides);
    for (MatchEdge edge : edges) {
      int cA = clusterOf[edge.indexA()];
      int cB = clusterOf[edge.indexB()];
      if (cA == cB) {
        continue;
      }
      List<Integer> groupA = clusterIndices.get(cA);
      List<Integer> groupB = clusterIndices.get(cB);
      if (canMergeClusters(items, groupA, groupB, matcher)) {
        groupA.addAll(groupB);
        for (int memberIdx : groupB) {
          clusterOf[memberIdx] = cA;
        }
        groupB.clear();
      }
    }

    return collectNonEmptyClusters(items, clusterIndices);
  }

  private static List<List<IngatlanItem>> collectNonEmptyClusters(
      List<IngatlanItem> items, List<List<Integer>> clusterIndices) {
    List<List<IngatlanItem>> result = new ArrayList<>();
    for (List<Integer> group : clusterIndices) {
      if (group.isEmpty()) {
        continue;
      }
      List<IngatlanItem> cluster = new ArrayList<>(group.size());
      for (int idx : group) {
        IngatlanItem item = items.get(idx);
        if (item != null) {
          cluster.add(item);
        }
      }
      if (!cluster.isEmpty()) {
        result.add(cluster);
      }
    }
    return result;
  }

  private static List<MatchEdge> collectScoredMatchEdges(
      List<IngatlanItem> items,
      BiPredicate<IngatlanItem, IngatlanItem> matcher,
      DedupOverrides overrides) {
    Map<BucketKey, List<Integer>> buckets = groupByCityAndType(items);
    List<MatchEdge> edges = new ArrayList<>();
    collectForceMergeEdges(items, matcher, overrides, edges);
    for (List<Integer> bucket : buckets.values()) {
      collectBucketEdges(items, bucket, matcher, overrides, edges);
    }
    edges.sort(Comparator.comparingDouble(MatchEdge::score).reversed()
        .thenComparingInt(MatchEdge::indexA)
        .thenComparingInt(MatchEdge::indexB));
    return edges;
  }

  private static void collectForceMergeEdges(
      List<IngatlanItem> items,
      BiPredicate<IngatlanItem, IngatlanItem> matcher,
      DedupOverrides overrides,
      List<MatchEdge> edges) {
    if (overrides.isEmpty()) {
      return;
    }
    Map<String, Integer> urlToIndex = indexItemsByUrl(items);
    ForceMergeContext ctx = new ForceMergeContext(items, matcher, new HashSet<>(), edges);
    for (UrlPair pair : overrides.getForceMergePairs()) {
      Integer idxA = urlToIndex.get(pair.urlA());
      Integer idxB = urlToIndex.get(pair.urlB());
      if (idxA != null && idxB != null && !idxA.equals(idxB)) {
        tryAddForceMergeEdge(ctx, idxA, idxB);
      }
    }
    addTransitiveComponentEdges(ctx, overrides);
  }

  private static Map<String, Integer> indexItemsByUrl(List<IngatlanItem> items) {
    Map<String, Integer> urlToIndex = new HashMap<>(items.size());
    for (int i = 0; i < items.size(); i++) {
      IngatlanItem item = items.get(i);
      if (item != null && item.getUrl() != null && !item.getUrl().isBlank()) {
        urlToIndex.putIfAbsent(item.getUrl().trim(), i);
      }
    }
    return urlToIndex;
  }

  private static void addTransitiveComponentEdges(
      ForceMergeContext ctx, DedupOverrides overrides) {
    Map<Integer, List<Integer>> byComponent = new LinkedHashMap<>();
    for (int i = 0; i < ctx.items().size(); i++) {
      IngatlanItem item = ctx.items().get(i);
      if (item == null || item.getUrl() == null) {
        continue;
      }
      Integer compId = overrides.getForceMergeComponentId(item.getUrl());
      if (compId != null) {
        byComponent.computeIfAbsent(compId, ignored -> new ArrayList<>()).add(i);
      }
    }
    for (List<Integer> group : byComponent.values()) {
      linkTransitiveGroup(ctx, group);
    }
  }

  private static void linkTransitiveGroup(ForceMergeContext ctx, List<Integer> group) {
    if (group.size() < 2) {
      return;
    }
    int anchor = group.get(0);
    for (int k = 1; k < group.size(); k++) {
      int candidate = group.get(k);
      if (!tryAddForceMergeEdge(ctx, anchor, candidate)) {
        linkToFirstCompatiblePeer(ctx, group, k, candidate);
      }
    }
  }

  private static void linkToFirstCompatiblePeer(
      ForceMergeContext ctx, List<Integer> group, int limitExclusive, int candidate) {
    for (int j = 1; j < limitExclusive; j++) {
      if (tryAddForceMergeEdge(ctx, group.get(j), candidate)) {
        return;
      }
    }
  }

  private static boolean tryAddForceMergeEdge(ForceMergeContext ctx, int idxA, int idxB) {
    int minIdx = Math.min(idxA, idxB);
    int maxIdx = Math.max(idxA, idxB);
    if (!ctx.matcher().test(ctx.items().get(minIdx), ctx.items().get(maxIdx))) {
      return false;
    }
    long key = (((long) minIdx) << 32) | (maxIdx & 0xFFFFFFFFL);
    if (ctx.addedPairs().add(key)) {
      ctx.edges().add(new MatchEdge(minIdx, maxIdx, FORCE_MERGE_SCORE));
    }
    return true;
  }

  private static Map<BucketKey, List<Integer>> groupByCityAndType(List<IngatlanItem> items) {
    Map<BucketKey, List<Integer>> buckets = new LinkedHashMap<>();
    for (int i = 0; i < items.size(); i++) {
      IngatlanItem item = items.get(i);
      if (item == null || item.getCity() == null || item.getCity().isBlank()
          || item.getPropertyType() == null) {
        continue;
      }
      BucketKey key = new BucketKey(CityUtils.normalizeCityKey(item.getCity()), item.getPropertyType());
      buckets.computeIfAbsent(key, ignored -> new ArrayList<>()).add(i);
    }
    return buckets;
  }

  private static void collectBucketEdges(
      List<IngatlanItem> items,
      List<Integer> bucket,
      BiPredicate<IngatlanItem, IngatlanItem> matcher,
      DedupOverrides overrides,
      List<MatchEdge> edges) {
    for (int i = 0; i < bucket.size(); i++) {
      int idxA = bucket.get(i);
      IngatlanItem itemA = items.get(idxA);
      for (int j = i + 1; j < bucket.size(); j++) {
        int idxB = bucket.get(j);
        IngatlanItem itemB = items.get(idxB);
        if (overrides.isForceMerge(itemA.getUrl(), itemB.getUrl())) {
          continue;
        }
        if (matcher.test(itemA, itemB)) {
          edges.add(new MatchEdge(idxA, idxB, computeMatchScore(itemA, itemB)));
        }
      }
    }
  }

  private static boolean canMergeClusters(
      List<IngatlanItem> items,
      List<Integer> groupA,
      List<Integer> groupB,
      BiPredicate<IngatlanItem, IngatlanItem> matcher) {
    for (int idxA : groupA) {
      IngatlanItem memberA = items.get(idxA);
      for (int idxB : groupB) {
        if (!matcher.test(memberA, items.get(idxB))) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Computes a non-negative match strength score for two compatible listings.
   */
  public static double computeMatchScore(IngatlanItem a, IngatlanItem b) {
    if (a == null || b == null) {
      return 0.0;
    }
    double score = 0.0;
    score += scorePriceProximity(a, b);
    score += scoreAreaProximity(a, b);
    score += scoreRoomProximity(a, b);
    score += scoreLocationAlignment(a, b);
    score += scoreMediaAndTitle(a, b);
    if (isActive(a) && isActive(b)) {
      score += WEIGHT_BOTH_ACTIVE;
    }
    if (ClusterConsolidationHelper.isSamePortalDifferentUrl(a, b)) {
      score += WEIGHT_INTRA_PORTAL_PRIORITY;
    }
    return score;
  }

  private static double scorePriceProximity(IngatlanItem a, IngatlanItem b) {
    Long priceA = a.getPriceInfo() != null ? a.getPriceInfo().amountHuf() : null;
    Long priceB = b.getPriceInfo() != null ? b.getPriceInfo().amountHuf() : null;
    if (priceA == null || priceB == null || priceA <= 0 || priceB <= 0) {
      return 0.0;
    }
    double diffRatio = (double) Math.abs(priceA - priceB) / (double) Math.max(priceA, priceB);
    double closeness = Math.max(0.0, 1.0 - (diffRatio / MAX_PRICE_BAND_RATIO));
    return WEIGHT_PRICE * closeness;
  }

  private static double scoreAreaProximity(IngatlanItem a, IngatlanItem b) {
    Double areaA = a.getDetailsInfo() != null ? a.getDetailsInfo().areaSquareMeters() : null;
    Double areaB = b.getDetailsInfo() != null ? b.getDetailsInfo().areaSquareMeters() : null;
    if (areaA == null || areaB == null || areaA <= 0 || areaB <= 0) {
      return 0.0;
    }
    double diffRatio = Math.abs(areaA - areaB) / Math.max(areaA, areaB);
    double tolerance = a.getPropertyType() == PropertyType.HAZ
        ? HOUSE_MAX_AREA_TOLERANCE_RATIO : MAX_AREA_TOLERANCE_RATIO;
    double closeness = Math.max(0.0, 1.0 - (diffRatio / tolerance));
    return WEIGHT_AREA * closeness;
  }

  private static double scoreRoomProximity(IngatlanItem a, IngatlanItem b) {
    Double roomsA = a.getDetailsInfo() != null ? a.getDetailsInfo().roomCount() : null;
    Double roomsB = b.getDetailsInfo() != null ? b.getDetailsInfo().roomCount() : null;
    if (roomsA == null || roomsB == null || roomsA <= 0 || roomsB <= 0) {
      return 0.0;
    }
    double diff = Math.abs(roomsA - roomsB);
    if (diff < 0.01) {
      return WEIGHT_ROOMS;
    }
    if (diff <= 0.5) {
      return WEIGHT_ROOMS * 0.5;
    }
    return 0.0;
  }

  private static double scoreLocationAlignment(IngatlanItem a, IngatlanItem b) {
    String normCity = CityUtils.normalizeCityKey(a.getCity());
    ExtractedLocation locA = LocationConflictDetector.extractLocation(a, normCity);
    ExtractedLocation locB = LocationConflictDetector.extractLocation(b, normCity);
    double score = 0.0;
    if (LocationConflictDetector.hasSameStreet(locA, locB)) {
      score += WEIGHT_STREET;
    }
    if (LocationConflictDetector.hasSameSubArea(locA, locB)) {
      score += WEIGHT_LOCATION;
    }
    return score;
  }

  private static double scoreMediaAndTitle(IngatlanItem a, IngatlanItem b) {
    double score = 0.0;
    if (ClusterConsolidationHelper.hasMatchingImage(a, b)) {
      score += WEIGHT_IMAGE;
    }
    Set<String> tokensA = TextualSimilarityMatcher.extractDistinctiveTokens(a);
    Set<String> tokensB = TextualSimilarityMatcher.extractDistinctiveTokens(b);
    double titleSimilarity = TextualSimilarityMatcher.hasIdenticalTitleSlug(a, b)
        ? 1.0
        : TextualSimilarityMatcher.calculateJaccardSimilarity(tokensA, tokensB);
    score += WEIGHT_TITLE * titleSimilarity;
    return score;
  }

  private static boolean isActive(IngatlanItem item) {
    ListingStatus status = item.getStatus();
    return status == null || status == ListingStatus.ACTIVE
        || status == ListingStatus.PRICE_DROP || status == ListingStatus.PRICE_INCREASE;
  }
}
