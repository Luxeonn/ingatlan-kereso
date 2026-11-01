package ingatlanok.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Immutable domain value object holding aggregated market statistics, medians, segment breakdowns,
 * and price-drop metrics produced by {@link ingatlanok.core.MarketAnalyticsEngine}.
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class MarketStats implements Serializable {

  private Double averagePriceMillion;
  private Long averagePricePerM2;
  private Double minPriceMillion;
  private Double maxPriceMillion;
  private Double newRatePercent;
  @Builder.Default
  private Map<String, Integer> cityBreakdown = new LinkedHashMap<>();

  private Long medianPricePerM2;
  @Builder.Default
  private Integer totalSampleSize = 0;
  @Builder.Default
  private Boolean insufficientData = true;

  private Long residentialMedianPricePerM2;
  private Long residentialAveragePricePerM2;
  @Builder.Default
  private Integer residentialSampleSize = 0;
  @Builder.Default
  private Boolean residentialInsufficientData = true;

  private Long plotMedianPricePerM2;
  private Long plotAveragePricePerM2;
  @Builder.Default
  private Integer plotSampleSize = 0;
  @Builder.Default
  private Boolean plotInsufficientData = true;

  @Builder.Default
  private List<MarketSegmentStats> segments = new ArrayList<>();

  @Builder.Default
  private Integer priceDropCount = 0;
  @Builder.Default
  private Double priceDropRatePercent = 0.0;
  private Long averagePriceDropAmountHuf;
  private Double averagePriceDropPercent;
  @Builder.Default
  private List<PriceDropItem> topPriceDrops = new ArrayList<>();

  @Builder.Default
  private Integer bargainCount = 0;
  @Builder.Default
  private Double bargainRatePercent = 0.0;

  public Boolean isInsufficientData() {
    return insufficientData;
  }

  public Boolean isResidentialInsufficientData() {
    return residentialInsufficientData;
  }

  public Boolean isPlotInsufficientData() {
    return plotInsufficientData;
  }
}
