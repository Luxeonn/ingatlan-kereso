package ingatlanok.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.Builder;

/**
 * Immutable domain value object holding aggregated market statistics, medians, segment breakdowns,
 * and price-drop metrics produced by {@link ingatlanok.core.MarketAnalyticsEngine}.
 */
@Builder
public record MarketStats(
    Double averagePriceMillion,
    Long averagePricePerM2,
    Double minPriceMillion,
    Double maxPriceMillion,
    Double newRatePercent,
    Map<String, Integer> cityBreakdown,
    Long medianPricePerM2,
    Integer totalSampleSize,
    Boolean insufficientData,
    Long residentialMedianPricePerM2,
    Long residentialAveragePricePerM2,
    Integer residentialSampleSize,
    Boolean residentialInsufficientData,
    Long plotMedianPricePerM2,
    Long plotAveragePricePerM2,
    Integer plotSampleSize,
    Boolean plotInsufficientData,
    List<MarketSegmentStats> segments,
    Integer priceDropCount,
    Double priceDropRatePercent,
    Long averagePriceDropAmountHuf,
    Double averagePriceDropPercent,
    List<PriceDropItem> topPriceDrops,
    Integer bargainCount,
    Double bargainRatePercent) {

  public MarketStats {
    cityBreakdown =
        cityBreakdown != null
            ? Collections.unmodifiableMap(new LinkedHashMap<>(cityBreakdown))
            : Map.of();
    totalSampleSize = Objects.requireNonNullElse(totalSampleSize, 0);
    insufficientData = Objects.requireNonNullElse(insufficientData, Boolean.TRUE);
    residentialSampleSize = Objects.requireNonNullElse(residentialSampleSize, 0);
    residentialInsufficientData =
        Objects.requireNonNullElse(residentialInsufficientData, Boolean.TRUE);
    plotSampleSize = Objects.requireNonNullElse(plotSampleSize, 0);
    plotInsufficientData = Objects.requireNonNullElse(plotInsufficientData, Boolean.TRUE);
    segments =
        segments != null ? Collections.unmodifiableList(new ArrayList<>(segments)) : List.of();
    priceDropCount = Objects.requireNonNullElse(priceDropCount, 0);
    priceDropRatePercent = Objects.requireNonNullElse(priceDropRatePercent, 0.0);
    topPriceDrops =
        topPriceDrops != null
            ? Collections.unmodifiableList(new ArrayList<>(topPriceDrops))
            : List.of();
    bargainCount = Objects.requireNonNullElse(bargainCount, 0);
    bargainRatePercent = Objects.requireNonNullElse(bargainRatePercent, 0.0);
  }
}
