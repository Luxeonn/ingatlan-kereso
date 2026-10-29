package ingatlanok.presentation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Aggregated market statistics and KPIs.
 */
@Getter
@Setter
@NoArgsConstructor
public class MarketStatsDto implements Serializable {

  private Double averagePriceMillion;
  private Long averagePricePerM2;
  private Double minPriceMillion;
  private Double maxPriceMillion;
  private Double newRatePercent;
  private Map<String, Integer> cityBreakdown = new LinkedHashMap<>();

  // Milestone R4 Analytics fields (#263 & #264)
  private Long medianPricePerM2;
  private Integer totalSampleSize = 0;
  private Boolean insufficientData = true;

  private Long residentialMedianPricePerM2;
  private Long residentialAveragePricePerM2;
  private Integer residentialSampleSize = 0;
  private Boolean residentialInsufficientData = true;

  private Long plotMedianPricePerM2;
  private Long plotAveragePricePerM2;
  private Integer plotSampleSize = 0;
  private Boolean plotInsufficientData = true;

  private List<MarketSegmentStatsDto> segments = new ArrayList<>();

  private Integer priceDropCount = 0;
  private Double priceDropRatePercent = 0.0;
  private Long averagePriceDropAmountHuf;
  private Double averagePriceDropPercent;
  private List<PriceDropItemDto> topPriceDrops = new ArrayList<>();

  private Integer bargainCount = 0;
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

  public void setSegments(List<MarketSegmentStatsDto> segments) {
    this.segments = segments != null ? segments : new ArrayList<>();
  }

  public void setTopPriceDrops(List<PriceDropItemDto> topPriceDrops) {
    this.topPriceDrops = topPriceDrops != null ? topPriceDrops : new ArrayList<>();
  }
}
