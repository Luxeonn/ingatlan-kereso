package ingatlanok.presentation;

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
 * Aggregated market statistics and KPIs.
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class MarketStatsDto implements Serializable {

  private Double averagePriceMillion;
  private Long averagePricePerM2;
  private Double minPriceMillion;
  private Double maxPriceMillion;
  private Double newRatePercent;
  @Builder.Default
  private Map<String, Integer> cityBreakdown = new LinkedHashMap<>();

  // Milestone R4 Analytics fields (#263 & #264)
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
  private List<MarketSegmentStatsDto> segments = new ArrayList<>();

  @Builder.Default
  private Integer priceDropCount = 0;
  @Builder.Default
  private Double priceDropRatePercent = 0.0;
  private Long averagePriceDropAmountHuf;
  private Double averagePriceDropPercent;
  @Builder.Default
  private List<PriceDropItemDto> topPriceDrops = new ArrayList<>();

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
