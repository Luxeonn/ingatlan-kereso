package ingatlanok.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ingatlanok.common.CityUtils;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.PropertyType;
import ingatlanok.presentation.MarketSegmentStatsDto;
import ingatlanok.presentation.MarketStatsDto;
import ingatlanok.presentation.PresentationMapper;
import ingatlanok.presentation.PriceDropItemDto;

/**
 * Statistical market analytics engine computing medians, category segmentation,
 * bargain indicators, and price drop metrics.
 */
public class MarketAnalyticsEngine {

  private static final int MIN_SAMPLE_SIZE = 5;
  private static final double BARGAIN_DISCOUNT_THRESHOLD_PERCENT = 20.0;

  private final DataQualityEngine qualityEngine;

  public MarketAnalyticsEngine() {
    this(new DataQualityEngine());
  }

  public MarketAnalyticsEngine(DataQualityEngine qualityEngine) {
    this.qualityEngine = qualityEngine != null ? qualityEngine : new DataQualityEngine();
  }

  public boolean isEligibleForMedian(IngatlanItem item) {
    if (item == null) {
      return false;
    }
    int score = item.getQualityScore() != null
        ? item.getQualityScore()
        : qualityEngine.evaluate(item).score();
    return score >= DataQualityEngine.PASSING_THRESHOLD;
  }

  public MarketStatsDto calculateMarketStats(List<IngatlanItem> items) {
    MarketStatsDto.MarketStatsDtoBuilder stats = MarketStatsDto.builder();
    if (items == null || items.isEmpty()) {
      return stats.build();
    }

    List<IngatlanItem> uniqueItems = deduplicateItems(items);
    int totalItems = uniqueItems.size();

    MarketStatsAccumulator acc = accumulateItemMetrics(uniqueItems);
    computeGeneralPriceMetrics(stats, acc, totalItems);
    computeResidentialAndPlotMetrics(stats, acc);
    Map<String, MarketSegmentStatsDto> segmentDtoMap = computeSegmentMetrics(acc.segmentBuckets);
    detectBargains(stats, uniqueItems, segmentDtoMap, totalItems);
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
    String cityKey = CityUtils.normalizeCityKey(item.getCity());
    PropertyType type = item.getPropertyType();
    String bucketKey = cityKey + "#" + type.name();
    segmentBuckets.computeIfAbsent(bucketKey, k -> new ArrayList<>()).add(item);
  }

  private void computeGeneralPriceMetrics(
      MarketStatsDto.MarketStatsDtoBuilder stats, MarketStatsAccumulator acc, int totalItems) {
    if (!acc.pricesMillion.isEmpty()) {
      double sum = acc.pricesMillion.stream().mapToDouble(Double::doubleValue).sum();
      double avg = sum / acc.pricesMillion.size();
      stats
          .averagePriceMillion(Math.round(avg * 10.0) / 10.0)
          .minPriceMillion(acc.pricesMillion.stream().mapToDouble(Double::doubleValue).min().orElse(0.0))
          .maxPriceMillion(acc.pricesMillion.stream().mapToDouble(Double::doubleValue).max().orElse(0.0));
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
      MarketStatsDto.MarketStatsDtoBuilder stats, MarketStatsAccumulator acc) {
    Long resMedian = computeResidentialMetrics(stats, acc.residentialPricesM2);
    Long plotMedian = computePlotMetrics(stats, acc.plotPricesM2);
    computeOverallMedian(stats, acc, resMedian, plotMedian);
  }

  private Long computeResidentialMetrics(
      MarketStatsDto.MarketStatsDtoBuilder stats, List<Long> residentialPricesM2) {
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

  private Long computePlotMetrics(
      MarketStatsDto.MarketStatsDtoBuilder stats, List<Long> plotPricesM2) {
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
      MarketStatsDto.MarketStatsDtoBuilder stats,
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

  private Map<String, MarketSegmentStatsDto> computeSegmentMetrics(
      Map<String, List<IngatlanItem>> segmentBuckets) {
    Map<String, MarketSegmentStatsDto> segmentDtoMap = new LinkedHashMap<>();
    for (Map.Entry<String, List<IngatlanItem>> entry : segmentBuckets.entrySet()) {
      String bucketKey = entry.getKey();
      List<IngatlanItem> bucketItems = entry.getValue();
      segmentDtoMap.put(bucketKey, buildSegmentStats(bucketItems));
    }
    return segmentDtoMap;
  }

  private MarketSegmentStatsDto buildSegmentStats(List<IngatlanItem> bucketItems) {
    IngatlanItem sampleItem = bucketItems.getFirst();
    MarketSegmentStatsDto.MarketSegmentStatsDtoBuilder segBuilder =
        MarketSegmentStatsDto.builder()
            .city(sampleItem.getCity())
            .propertyType(sampleItem.getPropertyType())
            .propertyTypeName(sampleItem.getPropertyType().getDisplayName())
            .residential(sampleItem.getPropertyType().isResidential())
            .plot(sampleItem.getPropertyType().isPlot());

    List<Long> bucketPricesM2 = new ArrayList<>();
    List<Double> bucketPricesM = new ArrayList<>();
    collectBucketEligiblePrices(bucketItems, bucketPricesM2, bucketPricesM);

    populateSegmentSqmStats(segBuilder, bucketPricesM2);
    populateSegmentMillionStats(segBuilder, bucketPricesM);
    return segBuilder.build();
  }

  private void collectBucketEligiblePrices(
      List<IngatlanItem> bucketItems, List<Long> bucketPricesM2, List<Double> bucketPricesM) {
    for (IngatlanItem bi : bucketItems) {
      if (!isEligibleForMedian(bi)) {
        continue;
      }
      Long pm2 = resolvePricePerM2(bi);
      if (pm2 != null && pm2 > 0) {
        bucketPricesM2.add(pm2);
      }
      if (bi.getPriceInfo() != null
          && bi.getPriceInfo().getAmountMillion() != null
          && bi.getPriceInfo().getAmountMillion() > 0) {
        bucketPricesM.add(bi.getPriceInfo().getAmountMillion());
      }
    }
  }

  private void populateSegmentSqmStats(
      MarketSegmentStatsDto.MarketSegmentStatsDtoBuilder segBuilder, List<Long> bucketPricesM2) {
    segBuilder.sampleSize(bucketPricesM2.size());
    if (bucketPricesM2.size() >= MIN_SAMPLE_SIZE) {
      long sumM2 = bucketPricesM2.stream().mapToLong(Long::longValue).sum();
      segBuilder
          .insufficientData(false)
          .medianPricePerM2(calculateMedian(bucketPricesM2))
          .averagePricePerM2(sumM2 / bucketPricesM2.size());
    } else {
      segBuilder.insufficientData(true).medianPricePerM2(null);
      if (!bucketPricesM2.isEmpty()) {
        long sumM2 = bucketPricesM2.stream().mapToLong(Long::longValue).sum();
        segBuilder.averagePricePerM2(sumM2 / bucketPricesM2.size());
      }
    }
  }

  private void populateSegmentMillionStats(
      MarketSegmentStatsDto.MarketSegmentStatsDtoBuilder segBuilder, List<Double> bucketPricesM) {
    if (bucketPricesM.isEmpty()) {
      return;
    }
    double sumM = bucketPricesM.stream().mapToDouble(Double::doubleValue).sum();
    segBuilder
        .averagePriceMillion(Math.round((sumM / bucketPricesM.size()) * 10.0) / 10.0)
        .medianPriceMillion(calculateDoubleMedian(bucketPricesM))
        .minPriceMillion(bucketPricesM.stream().mapToDouble(Double::doubleValue).min().orElse(0.0))
        .maxPriceMillion(bucketPricesM.stream().mapToDouble(Double::doubleValue).max().orElse(0.0));
  }

  private void detectBargains(
      MarketStatsDto.MarketStatsDtoBuilder stats,
      List<IngatlanItem> uniqueItems,
      Map<String, MarketSegmentStatsDto> segmentDtoMap,
      int totalItems) {
    int totalBargains = 0;
    Map<String, Integer> segmentBargains = new HashMap<>();

    for (IngatlanItem item : uniqueItems) {
      if (item == null) {
        continue;
      }
      String bucketKey = resolveBucketKey(item);
      MarketSegmentStatsDto segment = segmentDtoMap.get(bucketKey);
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

    finalizeSegmentBargains(stats, segmentDtoMap, segmentBargains);
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
      Long itemPriceM2, MarketSegmentStatsDto segment, IngatlanItem item) {
    if (itemPriceM2 == null || itemPriceM2 <= 0 || segment == null || segment.isInsufficientData()) {
      return false;
    }
    Long median = segment.getMedianPricePerM2();
    return median != null && median > 0 && isEligibleForMedian(item);
  }

  private boolean evaluateItemBargain(
      IngatlanItem item, Long itemPriceM2, MarketSegmentStatsDto segment) {
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

  private void suppressItemBargain(IngatlanItem item, MarketSegmentStatsDto segment) {
    item.setIsBargain(false);
    item.setReferenceMedianPricePerSquareMeter(null);
    item.setPercentageBelowMedian(null);
    item.setReferenceSampleSize(segment != null ? segment.getSampleSize() : 0);
  }

  private void finalizeSegmentBargains(
      MarketStatsDto.MarketStatsDtoBuilder stats,
      Map<String, MarketSegmentStatsDto> segmentDtoMap,
      Map<String, Integer> segmentBargains) {
    List<MarketSegmentStatsDto> finalizedSegments = new ArrayList<>();
    for (Map.Entry<String, MarketSegmentStatsDto> entry : segmentDtoMap.entrySet()) {
      String bucketKey = entry.getKey();
      MarketSegmentStatsDto seg = entry.getValue();
      int bCount = segmentBargains.getOrDefault(bucketKey, 0);
      finalizedSegments.add(seg.toBuilder().bargainCount(bCount).build());
    }
    stats.segments(finalizedSegments);
  }

  private void computePriceDrops(
      MarketStatsDto.MarketStatsDtoBuilder stats, List<IngatlanItem> uniqueItems, int totalItems) {
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
      MarketStatsDto.MarketStatsDtoBuilder stats, List<IngatlanItem> priceDropItems) {
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
            Math.round((totalDropPct / priceDropItems.size()) * 10.0) / 10.0);

    priceDropItems.sort(
        Comparator.comparingLong((IngatlanItem it) -> Math.abs(it.getPriceDelta())).reversed());

    List<PriceDropItemDto> topDrops = new ArrayList<>();
    int limit = Math.min(priceDropItems.size(), 10);
    for (int i = 0; i < limit; i++) {
      IngatlanItem it = priceDropItems.get(i);
      topDrops.add(PresentationMapper.INSTANCE.toPriceDropItemDto(it, resolvePricePerM2(it)));
    }
    stats.topPriceDrops(topDrops);
  }

  public Long resolvePricePerM2(IngatlanItem item) {
    if (item == null) {
      return null;
    }
    PropertyDetails details = item.getDetailsInfo();
    if (details != null
        && details.getPricePerSquareMeter() != null
        && details.getPricePerSquareMeter() > 0) {
      return details.getPricePerSquareMeter();
    }
    PriceInfo price = item.getPriceInfo();
    if (hasValidAmountAndArea(price, details)) {
      return Math.round((double) price.getAmountHuf() / details.getAreaSquareMeters());
    }
    return null;
  }

  private boolean hasValidAmountAndArea(PriceInfo price, PropertyDetails details) {
    if (price == null || price.getAmountHuf() == null || price.getAmountHuf() <= 0) {
      return false;
    }
    return details != null && details.getAreaSquareMeters() != null && details.getAreaSquareMeters() > 0;
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

  public Long calculateMedian(List<Long> values) {
    if (values == null || values.isEmpty()) {
      return null;
    }
    List<Long> sorted = new ArrayList<>(values);
    sorted.sort(Long::compareTo);
    int n = sorted.size();
    if (n % 2 == 1) {
      return sorted.get(n / 2);
    } else {
      long mid1 = sorted.get(n / 2 - 1);
      long mid2 = sorted.get(n / 2);
      return Math.round((mid1 + mid2) / 2.0);
    }
  }

  public Double calculateDoubleMedian(List<Double> values) {
    if (values == null || values.isEmpty()) {
      return null;
    }
    List<Double> sorted = new ArrayList<>(values);
    sorted.sort(Double::compareTo);
    int n = sorted.size();
    if (n % 2 == 1) {
      return Math.round(sorted.get(n / 2) * 10.0) / 10.0;
    } else {
      double mid1 = sorted.get(n / 2 - 1);
      double mid2 = sorted.get(n / 2);
      return Math.round(((mid1 + mid2) / 2.0) * 10.0) / 10.0;
    }
  }

  private List<IngatlanItem> deduplicateItems(List<IngatlanItem> items) {
    Map<String, IngatlanItem> unique = new LinkedHashMap<>();
    for (IngatlanItem item : items) {
      if (item == null) {
        continue;
      }
      String key = item.getUrl() != null && !item.getUrl().isBlank()
          ? item.getUrl()
          : (item.getId() != null ? item.getId() : "");
      if (!key.isEmpty()) {
        unique.putIfAbsent(key, item);
      } else {
        unique.put(java.util.UUID.randomUUID().toString(), item);
      }
    }
    return new ArrayList<>(unique.values());
  }
}
