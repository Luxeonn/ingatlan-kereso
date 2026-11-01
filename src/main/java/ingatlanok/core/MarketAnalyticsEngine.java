package ingatlanok.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ingatlanok.common.CityUtils;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.MarketSegmentStats;
import ingatlanok.model.MarketStats;
import ingatlanok.model.PropertyType;

/**
 * Statistical market analytics engine computing medians, category segmentation, bargain indicators,
 * and price drop metrics purely within the domain/core layer.
 */
public class MarketAnalyticsEngine {

  private static final int MIN_SAMPLE_SIZE = MarketStatisticsCalculator.MIN_SAMPLE_SIZE;
  private static final double BARGAIN_DISCOUNT_THRESHOLD_PERCENT = 20.0;

  private final DataQualityEngine qualityEngine;
  private final MarketStatisticsCalculator statisticsCalculator;

  public MarketAnalyticsEngine() {
    this(new DataQualityEngine());
  }

  public MarketAnalyticsEngine(DataQualityEngine qualityEngine) {
    this.qualityEngine = qualityEngine != null ? qualityEngine : new DataQualityEngine();
    this.statisticsCalculator = new MarketStatisticsCalculator();
  }

  public boolean isEligibleForMedian(IngatlanItem item) {
    if (item == null) {
      return false;
    }
    int score =
        item.getQualityScore() != null
            ? item.getQualityScore()
            : qualityEngine.evaluate(item).score();
    return score >= DataQualityEngine.PASSING_THRESHOLD;
  }

  public MarketStats calculateMarketStats(List<IngatlanItem> items) {
    MarketStats.MarketStatsBuilder stats = MarketStats.builder();
    if (items == null || items.isEmpty()) {
      return stats.build();
    }

    List<IngatlanItem> uniqueItems = statisticsCalculator.deduplicateItems(items);
    int totalItems = uniqueItems.size();

    MarketStatsAccumulator acc = accumulateItemMetrics(uniqueItems);
    computeGeneralPriceMetrics(stats, acc, totalItems);
    computeResidentialAndPlotMetrics(stats, acc);
    Map<String, MarketSegmentStats> segmentMap = computeSegmentMetrics(acc.segmentBuckets);
    detectBargains(stats, uniqueItems, segmentMap, totalItems);
    computePriceDrops(stats, uniqueItems, totalItems);

    return stats.build();
  }

  private MarketStatsAccumulator accumulateItemMetrics(List<IngatlanItem> uniqueItems) {
    MarketStatsAccumulator acc = new MarketStatsAccumulator();
    for (IngatlanItem item : uniqueItems) {
      if (item == null) {
        continue;
      }
      if (item.isNew()) {
        acc.newItemsCount++;
      }
      accumulateCityCount(acc.cityCounts, item.getCity());
      accumulatePriceMillion(acc.pricesMillion, item);
      accumulatePricePerM2(acc, item);
      accumulateSegmentBucket(acc.segmentBuckets, item);
    }
    return acc;
  }

  private void accumulateCityCount(Map<String, Integer> cityCounts, String city) {
    if (city != null && !city.isBlank()) {
      cityCounts.put(city, cityCounts.getOrDefault(city, 0) + 1);
    }
  }

  private void accumulatePriceMillion(List<Double> pricesMillion, IngatlanItem item) {
    if (item.getPriceInfo() != null
        && item.getPriceInfo().getAmountMillion() != null
        && item.getPriceInfo().getAmountMillion() > 0) {
      pricesMillion.add(item.getPriceInfo().getAmountMillion());
    }
  }

  private void accumulatePricePerM2(MarketStatsAccumulator acc, IngatlanItem item) {
    Long pricePerM2 = resolvePricePerM2(item);
    if (pricePerM2 == null || pricePerM2 <= 0) {
      return;
    }
    acc.allPricesPerM2.add(pricePerM2);
    if (!isEligibleForMedian(item)) {
      return;
    }
    PropertyType type = item.getPropertyType();
    if (type.isResidential()) {
      acc.residentialPricesM2.add(pricePerM2);
    } else if (type.isPlot()) {
      acc.plotPricesM2.add(pricePerM2);
    } else {
      acc.otherPricesM2.add(pricePerM2);
    }
  }

  private void accumulateSegmentBucket(
      Map<String, List<IngatlanItem>> segmentBuckets, IngatlanItem item) {
    String bucketKey = resolveBucketKey(item);
    segmentBuckets.computeIfAbsent(bucketKey, k -> new ArrayList<>()).add(item);
  }

  private void computeGeneralPriceMetrics(
      MarketStats.MarketStatsBuilder stats, MarketStatsAccumulator acc, int totalItems) {
    if (!acc.pricesMillion.isEmpty()) {
      double sum = acc.pricesMillion.stream().mapToDouble(Double::doubleValue).sum();
      double avg = sum / acc.pricesMillion.size();
      stats
          .averagePriceMillion(Math.round(avg * 10.0) / 10.0)
          .minPriceMillion(
              acc.pricesMillion.stream().mapToDouble(Double::doubleValue).min().orElse(0.0))
          .maxPriceMillion(
              acc.pricesMillion.stream().mapToDouble(Double::doubleValue).max().orElse(0.0));
    }
    if (!acc.allPricesPerM2.isEmpty()) {
      long sumM2 = acc.allPricesPerM2.stream().mapToLong(Long::longValue).sum();
      stats.averagePricePerM2(sumM2 / acc.allPricesPerM2.size());
    }
    double newRate =
        totalItems > 0
            ? Math.round(((double) acc.newItemsCount / totalItems * 100.0) * 10.0) / 10.0
            : 0.0;
    stats.newRatePercent(newRate).cityBreakdown(acc.cityCounts);
  }

  private void computeResidentialAndPlotMetrics(
      MarketStats.MarketStatsBuilder stats, MarketStatsAccumulator acc) {
    Long resMedian = computeResidentialMetrics(stats, acc.residentialPricesM2);
    Long plotMedian = computePlotMetrics(stats, acc.plotPricesM2);
    computeOverallMedian(stats, acc, resMedian, plotMedian);
  }

  private Long computeResidentialMetrics(
      MarketStats.MarketStatsBuilder stats, List<Long> residentialPricesM2) {
    stats.residentialSampleSize(residentialPricesM2.size());
    if (residentialPricesM2.size() >= MIN_SAMPLE_SIZE) {
      Long resMedian = calculateMedian(residentialPricesM2);
      long resSum = residentialPricesM2.stream().mapToLong(Long::longValue).sum();
      stats
          .residentialMedianPricePerM2(resMedian)
          .residentialAveragePricePerM2(resSum / residentialPricesM2.size())
          .residentialInsufficientData(false);
      return resMedian;
    }
    stats.residentialMedianPricePerM2(null);
    if (!residentialPricesM2.isEmpty()) {
      long resSum = residentialPricesM2.stream().mapToLong(Long::longValue).sum();
      stats.residentialAveragePricePerM2(resSum / residentialPricesM2.size());
    }
    stats.residentialInsufficientData(true);
    return null;
  }

  private Long computePlotMetrics(MarketStats.MarketStatsBuilder stats, List<Long> plotPricesM2) {
    stats.plotSampleSize(plotPricesM2.size());
    if (plotPricesM2.size() >= MIN_SAMPLE_SIZE) {
      Long plotMedian = calculateMedian(plotPricesM2);
      long plotSum = plotPricesM2.stream().mapToLong(Long::longValue).sum();
      stats
          .plotMedianPricePerM2(plotMedian)
          .plotAveragePricePerM2(plotSum / plotPricesM2.size())
          .plotInsufficientData(false);
      return plotMedian;
    }
    stats.plotMedianPricePerM2(null);
    if (!plotPricesM2.isEmpty()) {
      long plotSum = plotPricesM2.stream().mapToLong(Long::longValue).sum();
      stats.plotAveragePricePerM2(plotSum / plotPricesM2.size());
    }
    stats.plotInsufficientData(true);
    return null;
  }

  private void computeOverallMedian(
      MarketStats.MarketStatsBuilder stats,
      MarketStatsAccumulator acc,
      Long resMedian,
      Long plotMedian) {
    int totalValidSqm =
        acc.residentialPricesM2.size() + acc.plotPricesM2.size() + acc.otherPricesM2.size();
    stats.totalSampleSize(totalValidSqm);

    if (acc.residentialPricesM2.size() >= MIN_SAMPLE_SIZE) {
      stats.medianPricePerM2(resMedian).insufficientData(false);
    } else if (acc.plotPricesM2.size() >= MIN_SAMPLE_SIZE && acc.residentialPricesM2.isEmpty()) {
      stats.medianPricePerM2(plotMedian).insufficientData(false);
    } else {
      stats.medianPricePerM2(null).insufficientData(true);
    }
  }

  private Map<String, MarketSegmentStats> computeSegmentMetrics(
      Map<String, List<IngatlanItem>> segmentBuckets) {
    Map<String, MarketSegmentStats> segmentMap = new LinkedHashMap<>();
    for (Map.Entry<String, List<IngatlanItem>> entry : segmentBuckets.entrySet()) {
      segmentMap.put(
          entry.getKey(),
          statisticsCalculator.buildSegmentStats(entry.getValue(), this::isEligibleForMedian));
    }
    return segmentMap;
  }

  private void detectBargains(
      MarketStats.MarketStatsBuilder stats,
      List<IngatlanItem> uniqueItems,
      Map<String, MarketSegmentStats> segmentMap,
      int totalItems) {
    int totalBargains = 0;
    Map<String, Integer> segmentBargains = new HashMap<>();

    for (IngatlanItem item : uniqueItems) {
      if (item == null) {
        continue;
      }
      String bucketKey = resolveBucketKey(item);
      MarketSegmentStats segment = segmentMap.get(bucketKey);
      Long itemPriceM2 = resolvePricePerM2(item);

      if (isEligibleForBargain(itemPriceM2, segment, item)) {
        boolean isBargain = evaluateItemBargain(item, itemPriceM2, segment);
        if (isBargain) {
          totalBargains++;
          segmentBargains.merge(bucketKey, 1, Integer::sum);
        }
      } else {
        suppressItemBargain(item, segment);
      }
    }

    finalizeSegmentBargains(stats, segmentMap, segmentBargains);
    double bargainRate =
        totalItems > 0
            ? Math.round(((double) totalBargains / totalItems * 100.0) * 10.0) / 10.0
            : 0.0;
    stats.bargainCount(totalBargains).bargainRatePercent(bargainRate);
  }

  private String resolveBucketKey(IngatlanItem item) {
    String cityKey = CityUtils.normalizeCityKey(item.getCity());
    PropertyType type = item.getPropertyType();
    return cityKey + "#" + type.name();
  }

  private boolean isEligibleForBargain(
      Long itemPriceM2, MarketSegmentStats segment, IngatlanItem item) {
    if (itemPriceM2 == null
        || itemPriceM2 <= 0
        || segment == null
        || segment.isInsufficientData()) {
      return false;
    }
    Long median = segment.getMedianPricePerM2();
    return median != null && median > 0 && isEligibleForMedian(item);
  }

  private boolean evaluateItemBargain(
      IngatlanItem item, Long itemPriceM2, MarketSegmentStats segment) {
    long refMedian = segment.getMedianPricePerM2();
    double pctBelow = ((double) (refMedian - itemPriceM2) / refMedian) * 100.0;
    double roundedPctBelow = Math.round(pctBelow * 10.0) / 10.0;

    item.setReferenceMedianPricePerSquareMeter(refMedian);
    item.setPercentageBelowMedian(roundedPctBelow);
    item.setReferenceSampleSize(segment.getSampleSize());

    boolean isBargain = pctBelow >= BARGAIN_DISCOUNT_THRESHOLD_PERCENT;
    item.setIsBargain(isBargain);
    return isBargain;
  }

  private void suppressItemBargain(IngatlanItem item, MarketSegmentStats segment) {
    item.setIsBargain(false);
    item.setReferenceMedianPricePerSquareMeter(null);
    item.setPercentageBelowMedian(null);
    item.setReferenceSampleSize(segment != null ? segment.getSampleSize() : 0);
  }

  private void finalizeSegmentBargains(
      MarketStats.MarketStatsBuilder stats,
      Map<String, MarketSegmentStats> segmentMap,
      Map<String, Integer> segmentBargains) {
    List<MarketSegmentStats> finalizedSegments = new ArrayList<>();
    for (Map.Entry<String, MarketSegmentStats> entry : segmentMap.entrySet()) {
      String bucketKey = entry.getKey();
      MarketSegmentStats seg = entry.getValue();
      int bCount = segmentBargains.getOrDefault(bucketKey, 0);
      finalizedSegments.add(seg.toBuilder().bargainCount(bCount).build());
    }
    stats.segments(finalizedSegments);
  }

  private void computePriceDrops(
      MarketStats.MarketStatsBuilder stats, List<IngatlanItem> uniqueItems, int totalItems) {
    List<IngatlanItem> priceDropItems = new ArrayList<>();
    for (IngatlanItem item : uniqueItems) {
      if (item != null && item.getPriceDelta() != null && item.getPriceDelta() < 0) {
        priceDropItems.add(item);
      }
    }

    double dropRate =
        totalItems > 0
            ? Math.round(((double) priceDropItems.size() / totalItems * 100.0) * 10.0) / 10.0
            : 0.0;
    stats.priceDropCount(priceDropItems.size()).priceDropRatePercent(dropRate);

    if (priceDropItems.isEmpty()) {
      stats
          .averagePriceDropAmountHuf(null)
          .averagePriceDropPercent(null)
          .topPriceDrops(new ArrayList<>());
      return;
    }

    populatePriceDropDetails(stats, priceDropItems);
  }

  private void populatePriceDropDetails(
      MarketStats.MarketStatsBuilder stats, List<IngatlanItem> priceDropItems) {
    long totalDropHuf = 0;
    double totalDropPct = 0.0;
    for (IngatlanItem dropItem : priceDropItems) {
      totalDropHuf += Math.abs(dropItem.getPriceDelta());
      if (dropItem.getPriceChangePercent() != null) {
        totalDropPct += Math.abs(dropItem.getPriceChangePercent());
      }
    }
    stats
        .averagePriceDropAmountHuf(Math.round((double) totalDropHuf / priceDropItems.size()))
        .averagePriceDropPercent(
            Math.round((totalDropPct / priceDropItems.size()) * 10.0) / 10.0)
        .topPriceDrops(statisticsCalculator.buildTopPriceDrops(priceDropItems));
  }

  public Long resolvePricePerM2(IngatlanItem item) {
    return statisticsCalculator.resolvePricePerM2(item);
  }

  public Long calculateMedian(List<Long> values) {
    return statisticsCalculator.calculateMedian(values);
  }

  public Double calculateDoubleMedian(List<Double> values) {
    return statisticsCalculator.calculateDoubleMedian(values);
  }

  private static final class MarketStatsAccumulator {
    private final List<Double> pricesMillion = new ArrayList<>();
    private final List<Long> allPricesPerM2 = new ArrayList<>();
    private final List<Long> residentialPricesM2 = new ArrayList<>();
    private final List<Long> plotPricesM2 = new ArrayList<>();
    private final List<Long> otherPricesM2 = new ArrayList<>();
    private final Map<String, Integer> cityCounts = new LinkedHashMap<>();
    private final Map<String, List<IngatlanItem>> segmentBuckets = new LinkedHashMap<>();
    private int newItemsCount = 0;
  }
}
