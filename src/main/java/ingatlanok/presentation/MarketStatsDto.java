package ingatlanok.presentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.Builder;

/**
 * Aggregated market statistics and KPIs.
 */
@Builder
public record MarketStatsDto(
    Double averagePriceMillion,
    Long averagePricePerM2,
    Double minPriceMillion,
    Double maxPriceMillion,
    Double newRatePercent,
    Map<String, Integer> cityBreakdown,
    // Milestone R4 Analytics fields (#263 & #264)
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
    List<MarketSegmentStatsDto> segments,
    Integer priceDropCount,
    Double priceDropRatePercent,
    Long averagePriceDropAmountHuf,
    Double averagePriceDropPercent,
    List<PriceDropItemDto> topPriceDrops,
    Integer bargainCount,
    Double bargainRatePercent) {

  public MarketStatsDto {
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
