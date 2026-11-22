package ingatlanok.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import org.springframework.stereotype.Component;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.ListingStatus;
import ingatlanok.model.MarketSegmentStats;
import ingatlanok.model.PriceDropItem;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.SourceListing;

/**
 * Pure statistical math and segment calculation helper for {@link MarketAnalyticsEngine}.
 */
@Component
public class MarketStatisticsCalculator {

  static final int MIN_SAMPLE_SIZE = 5;
  private static final int MAX_TOP_PRICE_DROPS = 10;

  public Long calculateMedian(List<Long> values) {
    if (values == null || values.isEmpty()) {
      return null;
    }
    List<Long> sorted = new ArrayList<>(values);
    sorted.sort(Long::compareTo);
    int n = sorted.size();
    if (n % 2 == 1) {
      return sorted.get(n / 2);
    }
    long mid1 = sorted.get(n / 2 - 1);
    long mid2 = sorted.get(n / 2);
    return Math.round((mid1 + mid2) / 2.0);
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
    }
    double mid1 = sorted.get(n / 2 - 1);
    double mid2 = sorted.get(n / 2);
    return Math.round(((mid1 + mid2) / 2.0) * 10.0) / 10.0;
  }

  public Long resolvePricePerM2(IngatlanItem item) {
    if (item == null) {
      return null;
    }
    PropertyDetails details = item.getDetailsInfo();
    PriceInfo price = item.getPriceInfo();
    Long effectiveAmount = resolveEffectiveAmountHuf(item, price);
    Double area = details != null ? details.getAreaSquareMeters() : null;
    if (hasValidPositive(effectiveAmount, area) && differsFromPrimaryPrice(effectiveAmount, price)) {
      return Math.round((double) effectiveAmount / area);
    }
    Long directSqm = details != null ? details.getPricePerSquareMeter() : null;
    if (directSqm != null && directSqm > 0) {
      return directSqm;
    }
    return hasValidPositive(effectiveAmount, area)
        ? Math.round((double) effectiveAmount / area)
        : null;
  }

  private boolean hasValidPositive(Long amount, Double area) {
    return amount != null && amount > 0 && area != null && area > 0;
  }

  private boolean differsFromPrimaryPrice(Long effectiveAmount, PriceInfo price) {
    return price == null || price.getAmountHuf() == null || !effectiveAmount.equals(price.getAmountHuf());
  }

  private Long resolveEffectiveAmountHuf(IngatlanItem item, PriceInfo price) {
    List<SourceListing> sources = item.getSourceListings();
    if (sources == null || sources.isEmpty()) {
      return price != null ? price.getAmountHuf() : null;
    }
    Long minActive = null;
    for (SourceListing src : sources) {
      if (isActivePositiveSource(src) && (minActive == null || src.getPriceHuf() < minActive)) {
        minActive = src.getPriceHuf();
      }
    }
    return minActive != null ? minActive : (price != null ? price.getAmountHuf() : null);
  }

  private boolean isActivePositiveSource(SourceListing src) {
    if (src == null || src.getPriceHuf() == null || src.getPriceHuf() <= 0) {
      return false;
    }
    return src.getStatus() == null || src.getStatus() == ListingStatus.ACTIVE;
  }

  public List<IngatlanItem> deduplicateItems(List<IngatlanItem> items) {
    Map<String, IngatlanItem> unique = new LinkedHashMap<>();
    for (IngatlanItem item : items) {
      if (item == null) {
        continue;
      }
      String key =
          item.getUrl() != null && !item.getUrl().isBlank()
              ? item.getUrl()
              : (item.getId() != null ? item.getId() : "");
      if (!key.isEmpty()) {
        unique.putIfAbsent(key, item);
      } else {
        unique.put(UUID.randomUUID().toString(), item);
      }
    }
    return new ArrayList<>(unique.values());
  }

  public MarketSegmentStats buildSegmentStats(
      List<IngatlanItem> bucketItems, Predicate<IngatlanItem> eligibilityCheck) {
    IngatlanItem sampleItem = bucketItems.getFirst();
    MarketSegmentStats.MarketSegmentStatsBuilder segBuilder =
        MarketSegmentStats.builder()
            .city(sampleItem.getCity())
            .propertyType(sampleItem.getPropertyType())
            .propertyTypeName(sampleItem.getPropertyType().getDisplayName())
            .residential(sampleItem.getPropertyType().isResidential())
            .plot(sampleItem.getPropertyType().isPlot());

    List<Long> bucketPricesM2 = new ArrayList<>();
    List<Double> bucketPricesM = new ArrayList<>();
    collectBucketEligiblePrices(bucketItems, bucketPricesM2, bucketPricesM, eligibilityCheck);

    populateSegmentSqmStats(segBuilder, bucketPricesM2);
    populateSegmentMillionStats(segBuilder, bucketPricesM);
    return segBuilder.build();
  }

  private void collectBucketEligiblePrices(
      List<IngatlanItem> bucketItems,
      List<Long> bucketPricesM2,
      List<Double> bucketPricesM,
      Predicate<IngatlanItem> eligibilityCheck) {
    for (IngatlanItem bi : bucketItems) {
      if (!eligibilityCheck.test(bi)) {
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
      MarketSegmentStats.MarketSegmentStatsBuilder segBuilder, List<Long> bucketPricesM2) {
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
      MarketSegmentStats.MarketSegmentStatsBuilder segBuilder, List<Double> bucketPricesM) {
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

  public List<PriceDropItem> buildTopPriceDrops(List<IngatlanItem> priceDropItems) {
    priceDropItems.sort(
        Comparator.comparingLong((IngatlanItem it) -> Math.abs(it.getPriceDelta())).reversed());

    List<PriceDropItem> topDrops = new ArrayList<>();
    int limit = Math.min(priceDropItems.size(), MAX_TOP_PRICE_DROPS);
    for (int i = 0; i < limit; i++) {
      IngatlanItem it = priceDropItems.get(i);
      topDrops.add(PriceDropItem.from(it, resolvePricePerM2(it)));
    }
    return topDrops;
  }
}
