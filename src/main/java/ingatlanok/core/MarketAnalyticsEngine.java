package ingatlanok.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ingatlanok.common.CityUtils;
import ingatlanok.model.IngatlanItem;
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

    // Deduplicate by URL (or unique ID) to ensure each physical listing has equal weight
    List<IngatlanItem> uniqueItems = deduplicateItems(items);
    int totalItems = uniqueItems.size();

    List<Double> pricesMillion = new ArrayList<>();
    List<Long> allPricesPerM2 = new ArrayList<>();
    List<Long> residentialPricesM2 = new ArrayList<>();
    List<Long> plotPricesM2 = new ArrayList<>();
    List<Long> otherPricesM2 = new ArrayList<>();

    Map<String, Integer> cityCounts = new LinkedHashMap<>();
    int newItemsCount = 0;

    // Map bucket key (cityKey#type) -> list of items in segment
    Map<String, List<IngatlanItem>> segmentBuckets = new LinkedHashMap<>();

    for (IngatlanItem item : uniqueItems) {
      if (item == null) {
        continue;
      }

      if (item.isNew()) {
        newItemsCount++;
      }

      String city = item.getCity();
      if (city != null && !city.isBlank()) {
        cityCounts.put(city, cityCounts.getOrDefault(city, 0) + 1);
      }

      if (item.getPriceInfo() != null && item.getPriceInfo().getAmountMillion() != null
          && item.getPriceInfo().getAmountMillion() > 0) {
        pricesMillion.add(item.getPriceInfo().getAmountMillion());
      }

      Long pricePerM2 = resolvePricePerM2(item);
      if (pricePerM2 != null && pricePerM2 > 0) {
        allPricesPerM2.add(pricePerM2);
        if (isEligibleForMedian(item)) {
          PropertyType type = item.getPropertyType();
          if (type.isResidential()) {
            residentialPricesM2.add(pricePerM2);
          } else if (type.isPlot()) {
            plotPricesM2.add(pricePerM2);
          } else {
            otherPricesM2.add(pricePerM2);
          }
        }
      }

      String cityKey = CityUtils.normalizeCityKey(item.getCity());
      PropertyType type = item.getPropertyType();
      String bucketKey = cityKey + "#" + type.name();
      segmentBuckets.computeIfAbsent(bucketKey, k -> new ArrayList<>()).add(item);
    }

    // Legacy general metrics
    if (!pricesMillion.isEmpty()) {
      double sum = pricesMillion.stream().mapToDouble(Double::doubleValue).sum();
      double avg = sum / pricesMillion.size();
      stats
          .averagePriceMillion(Math.round(avg * 10.0) / 10.0)
          .minPriceMillion(pricesMillion.stream().mapToDouble(Double::doubleValue).min().orElse(0.0))
          .maxPriceMillion(pricesMillion.stream().mapToDouble(Double::doubleValue).max().orElse(0.0));
    }

    if (!allPricesPerM2.isEmpty()) {
      long sumM2 = allPricesPerM2.stream().mapToLong(Long::longValue).sum();
      stats.averagePricePerM2(sumM2 / allPricesPerM2.size());
    }

    double newRate = totalItems > 0
        ? Math.round(((double) newItemsCount / totalItems * 100.0) * 10.0) / 10.0
        : 0.0;
    stats.newRatePercent(newRate).cityBreakdown(cityCounts);

    // Residential living area metrics (LAKAS + HAZ)
    Long resMedian = null;
    stats.residentialSampleSize(residentialPricesM2.size());
    if (residentialPricesM2.size() >= MIN_SAMPLE_SIZE) {
      resMedian = calculateMedian(residentialPricesM2);
      long resSum = residentialPricesM2.stream().mapToLong(Long::longValue).sum();
      stats
          .residentialMedianPricePerM2(resMedian)
          .residentialAveragePricePerM2(resSum / residentialPricesM2.size())
          .residentialInsufficientData(false);
    } else {
      stats.residentialMedianPricePerM2(null);
      if (!residentialPricesM2.isEmpty()) {
        long resSum = residentialPricesM2.stream().mapToLong(Long::longValue).sum();
        stats.residentialAveragePricePerM2(resSum / residentialPricesM2.size());
      }
      stats.residentialInsufficientData(true);
    }

    // Plot land area metrics (TELEK) - strictly separated from living area
    Long plotMedian = null;
    stats.plotSampleSize(plotPricesM2.size());
    if (plotPricesM2.size() >= MIN_SAMPLE_SIZE) {
      plotMedian = calculateMedian(plotPricesM2);
      long plotSum = plotPricesM2.stream().mapToLong(Long::longValue).sum();
      stats
          .plotMedianPricePerM2(plotMedian)
          .plotAveragePricePerM2(plotSum / plotPricesM2.size())
          .plotInsufficientData(false);
    } else {
      stats.plotMedianPricePerM2(null);
      if (!plotPricesM2.isEmpty()) {
        long plotSum = plotPricesM2.stream().mapToLong(Long::longValue).sum();
        stats.plotAveragePricePerM2(plotSum / plotPricesM2.size());
      }
      stats.plotInsufficientData(true);
    }

    // Overall medianPricePerM2 & insufficientData guard
    int totalValidSqm = residentialPricesM2.size() + plotPricesM2.size() + otherPricesM2.size();
    stats.totalSampleSize(totalValidSqm);

    if (residentialPricesM2.size() >= MIN_SAMPLE_SIZE) {
      stats.medianPricePerM2(resMedian).insufficientData(false);
    } else if (plotPricesM2.size() >= MIN_SAMPLE_SIZE && residentialPricesM2.isEmpty()) {
      stats.medianPricePerM2(plotMedian).insufficientData(false);
    } else {
      stats.medianPricePerM2(null).insufficientData(true);
    }

    // Segment stats calculation and bucket lookup map
    Map<String, MarketSegmentStatsDto> segmentDtoMap = new LinkedHashMap<>();
    List<MarketSegmentStatsDto> segmentsList = new ArrayList<>();

    for (Map.Entry<String, List<IngatlanItem>> entry : segmentBuckets.entrySet()) {
      String bucketKey = entry.getKey();
      List<IngatlanItem> bucketItems = entry.getValue();

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
      for (IngatlanItem bi : bucketItems) {
        if (isEligibleForMedian(bi)) {
          Long pm2 = resolvePricePerM2(bi);
          if (pm2 != null && pm2 > 0) {
            bucketPricesM2.add(pm2);
          }
          if (bi.getPriceInfo() != null && bi.getPriceInfo().getAmountMillion() != null
              && bi.getPriceInfo().getAmountMillion() > 0) {
            bucketPricesM.add(bi.getPriceInfo().getAmountMillion());
          }
        }
      }

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

      if (!bucketPricesM.isEmpty()) {
        double sumM = bucketPricesM.stream().mapToDouble(Double::doubleValue).sum();
        segBuilder
            .averagePriceMillion(Math.round((sumM / bucketPricesM.size()) * 10.0) / 10.0)
            .medianPriceMillion(calculateDoubleMedian(bucketPricesM))
            .minPriceMillion(
                bucketPricesM.stream().mapToDouble(Double::doubleValue).min().orElse(0.0))
            .maxPriceMillion(
                bucketPricesM.stream().mapToDouble(Double::doubleValue).max().orElse(0.0));
      }

      MarketSegmentStatsDto segment = segBuilder.build();
      segmentDtoMap.put(bucketKey, segment);
      segmentsList.add(segment);
    }
    stats.segments(segmentsList);

    // Bargain detection on items
    int totalBargains = 0;
    for (IngatlanItem item : uniqueItems) {
      if (item == null) {
        continue;
      }
      String cityKey = CityUtils.normalizeCityKey(item.getCity());
      PropertyType type = item.getPropertyType();
      String bucketKey = cityKey + "#" + type.name();
      MarketSegmentStatsDto segment = segmentDtoMap.get(bucketKey);

      Long itemPriceM2 = resolvePricePerM2(item);
      if (itemPriceM2 != null && itemPriceM2 > 0 && segment != null
          && !segment.isInsufficientData() && segment.getMedianPricePerM2() != null
          && segment.getMedianPricePerM2() > 0
          && isEligibleForMedian(item)) {
        long refMedian = segment.getMedianPricePerM2();
        double pctBelow = ((double) (refMedian - itemPriceM2) / refMedian) * 100.0;
        double roundedPctBelow = Math.round(pctBelow * 10.0) / 10.0;

        item.setReferenceMedianPricePerSquareMeter(refMedian);
        item.setPercentageBelowMedian(roundedPctBelow);
        item.setReferenceSampleSize(segment.getSampleSize());

        if (pctBelow >= BARGAIN_DISCOUNT_THRESHOLD_PERCENT) {
          item.setIsBargain(true);
          totalBargains++;
          segment.setBargainCount(segment.getBargainCount() + 1);
        } else {
          item.setIsBargain(false);
        }
      } else {
        // Insufficient data (< 5 samples in bucket) or missing price/m2 -> bargain strictly suppressed
        item.setIsBargain(false);
        item.setReferenceMedianPricePerSquareMeter(null);
        item.setPercentageBelowMedian(null);
        item.setReferenceSampleSize(segment != null ? segment.getSampleSize() : 0);
      }
    }

    double bargainRate = totalItems > 0
        ? Math.round(((double) totalBargains / totalItems * 100.0) * 10.0) / 10.0
        : 0.0;
    stats.bargainCount(totalBargains).bargainRatePercent(bargainRate);

    // Price drops aggregation (listings with priceDelta < 0)
    List<IngatlanItem> priceDropItems = new ArrayList<>();
    for (IngatlanItem item : uniqueItems) {
      if (item != null && item.getPriceDelta() != null && item.getPriceDelta() < 0) {
        priceDropItems.add(item);
      }
    }

    double dropRate = totalItems > 0
        ? Math.round(((double) priceDropItems.size() / totalItems * 100.0) * 10.0) / 10.0
        : 0.0;
    stats.priceDropCount(priceDropItems.size()).priceDropRatePercent(dropRate);

    if (!priceDropItems.isEmpty()) {
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

      // Sort descending by absolute price drop (largest discount first)
      priceDropItems.sort(Comparator.comparingLong((IngatlanItem it) -> Math.abs(it.getPriceDelta())).reversed());

      List<PriceDropItemDto> topDrops = new ArrayList<>();
      int limit = Math.min(priceDropItems.size(), 10);
      for (int i = 0; i < limit; i++) {
        IngatlanItem it = priceDropItems.get(i);
        topDrops.add(PresentationMapper.INSTANCE.toPriceDropItemDto(it, resolvePricePerM2(it)));
      }
      stats.topPriceDrops(topDrops);
    } else {
      stats
          .averagePriceDropAmountHuf(null)
          .averagePriceDropPercent(null)
          .topPriceDrops(new ArrayList<>());
    }

    return stats.build();
  }

  public Long resolvePricePerM2(IngatlanItem item) {
    if (item == null) {
      return null;
    }
    if (item.getDetailsInfo() != null && item.getDetailsInfo().getPricePerSquareMeter() != null
        && item.getDetailsInfo().getPricePerSquareMeter() > 0) {
      return item.getDetailsInfo().getPricePerSquareMeter();
    }
    if (item.getPriceInfo() != null && item.getPriceInfo().getAmountHuf() != null
        && item.getPriceInfo().getAmountHuf() > 0
        && item.getDetailsInfo() != null && item.getDetailsInfo().getAreaSquareMeters() != null
        && item.getDetailsInfo().getAreaSquareMeters() > 0) {
      return Math.round((double) item.getPriceInfo().getAmountHuf() / item.getDetailsInfo().getAreaSquareMeters());
    }
    return null;
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
