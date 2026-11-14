package ingatlanok.core.dedup;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

import ingatlanok.common.CityUtils;
import ingatlanok.core.dedup.LocationConflictDetector.ExtractedLocation;
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

  private static final double WEIGHT_PRICE = 40.0;
  private static final double WEIGHT_STREET = 30.0;
  private static final double WEIGHT_LOCATION = 20.0;
  private static final double WEIGHT_IMAGE = 25.0;
  private static final double WEIGHT_AREA = 15.0;
  private static final double WEIGHT_ROOMS = 10.0;
  private static final double WEIGHT_TITLE = 10.0;
  private static final double WEIGHT_BOTH_ACTIVE = 5.0;

  private ClusterMatchScorer() {}

  private record MatchEdge(int indexA, int indexB, double score) {}

  private record BucketKey(String normalizedCity, PropertyType propertyType) {}

  /**
   * Groups compatible items into clusters by merging highest-scoring pairs first
   * while enforcing pairwise compatibility across every member of merged clusters.
   */
  public static List<List<IngatlanItem>> buildBestMatchClusters(
      List<IngatlanItem> items, BiPredicate<IngatlanItem, IngatlanItem> matcher) {
    int count = items.size();
    List<List<Integer>> clusterIndices = new ArrayList<>(count);
    int[] clusterOf = new int[count];
    for (int i = 0; i < count; i++) {
      List<Integer> singleton = new ArrayList<>(1);
      singleton.add(i);
      clusterIndices.add(singleton);
      clusterOf[i] = i;
    }

    List<MatchEdge> edges = collectScoredMatchEdges(items, matcher);
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

    List<List<IngatlanItem>> result = new ArrayList<>();
    for (List<Integer> group : clusterIndices) {
      if (!group.isEmpty()) {
        List<IngatlanItem> cluster = new ArrayList<>(group.size());
        for (int idx : group) {
          cluster.add(items.get(idx));
        }
        result.add(cluster);
      }
    }
    return result;
  }

  private static List<MatchEdge> collectScoredMatchEdges(
      List<IngatlanItem> items, BiPredicate<IngatlanItem, IngatlanItem> matcher) {
    Map<BucketKey, List<Integer>> buckets = groupByCityAndType(items);
    List<MatchEdge> edges = new ArrayList<>();
    for (List<Integer> bucket : buckets.values()) {
      collectBucketEdges(items, bucket, matcher, edges);
    }
    edges.sort(Comparator.comparingDouble(MatchEdge::score).reversed()
        .thenComparingInt(MatchEdge::indexA)
        .thenComparingInt(MatchEdge::indexB));
    return edges;
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
      List<MatchEdge> edges) {
    for (int i = 0; i < bucket.size(); i++) {
      int idxA = bucket.get(i);
      IngatlanItem itemA = items.get(idxA);
      for (int j = i + 1; j < bucket.size(); j++) {
        int idxB = bucket.get(j);
        IngatlanItem itemB = items.get(idxB);
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
    return score;
  }

  private static double scorePriceProximity(IngatlanItem a, IngatlanItem b) {
    Long priceA = a.getPriceInfo() != null ? a.getPriceInfo().getAmountHuf() : null;
    Long priceB = b.getPriceInfo() != null ? b.getPriceInfo().getAmountHuf() : null;
    if (priceA == null || priceB == null || priceA <= 0 || priceB <= 0) {
      return 0.0;
    }
    double diffRatio = (double) Math.abs(priceA - priceB) / (double) Math.max(priceA, priceB);
    double closeness = Math.max(0.0, 1.0 - (diffRatio / MAX_PRICE_BAND_RATIO));
    return WEIGHT_PRICE * closeness;
  }

  private static double scoreAreaProximity(IngatlanItem a, IngatlanItem b) {
    Double areaA = a.getDetailsInfo() != null ? a.getDetailsInfo().getAreaSquareMeters() : null;
    Double areaB = b.getDetailsInfo() != null ? b.getDetailsInfo().getAreaSquareMeters() : null;
    if (areaA == null || areaB == null || areaA <= 0 || areaB <= 0) {
      return 0.0;
    }
    double diffRatio = Math.abs(areaA - areaB) / Math.max(areaA, areaB);
    double closeness = Math.max(0.0, 1.0 - (diffRatio / MAX_AREA_TOLERANCE_RATIO));
    return WEIGHT_AREA * closeness;
  }

  private static double scoreRoomProximity(IngatlanItem a, IngatlanItem b) {
    Double roomsA = a.getDetailsInfo() != null ? a.getDetailsInfo().getRoomCount() : null;
    Double roomsB = b.getDetailsInfo() != null ? b.getDetailsInfo().getRoomCount() : null;
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
    if (locA.hasStreet() && locB.hasStreet() && locA.street().equals(locB.street())) {
      return WEIGHT_STREET;
    }
    if (LocationConflictDetector.hasAffirmativeMatch(locA, locB)) {
      return WEIGHT_LOCATION;
    }
    return 0.0;
  }

  private static double scoreMediaAndTitle(IngatlanItem a, IngatlanItem b) {
    double score = 0.0;
    if (ClusterConsolidationHelper.hasMatchingImage(a, b)) {
      score += WEIGHT_IMAGE;
    }
    Set<String> tokensA = TextualSimilarityMatcher.extractDistinctiveTokens(a);
    Set<String> tokensB = TextualSimilarityMatcher.extractDistinctiveTokens(b);
    score += WEIGHT_TITLE * TextualSimilarityMatcher.calculateJaccardSimilarity(tokensA, tokensB);
    return score;
  }

  private static boolean isActive(IngatlanItem item) {
    ListingStatus status = item.getStatus();
    return status == null || status == ListingStatus.ACTIVE
        || status == ListingStatus.PRICE_DROP || status == ListingStatus.PRICE_INCREASE;
  }
}
