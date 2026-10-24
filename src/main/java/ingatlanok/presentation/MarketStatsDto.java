package ingatlanok.presentation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregated market statistics and KPIs.
 */
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

  public MarketStatsDto() {}

  public Double getAveragePriceMillion() {
    return averagePriceMillion;
  }

  public void setAveragePriceMillion(Double averagePriceMillion) {
    this.averagePriceMillion = averagePriceMillion;
  }

  public Long getAveragePricePerM2() {
    return averagePricePerM2;
  }

  public void setAveragePricePerM2(Long averagePricePerM2) {
    this.averagePricePerM2 = averagePricePerM2;
  }

  public Double getMinPriceMillion() {
    return minPriceMillion;
  }

  public void setMinPriceMillion(Double minPriceMillion) {
    this.minPriceMillion = minPriceMillion;
  }

  public Double getMaxPriceMillion() {
    return maxPriceMillion;
  }

  public void setMaxPriceMillion(Double maxPriceMillion) {
    this.maxPriceMillion = maxPriceMillion;
  }

  public Double getNewRatePercent() {
    return newRatePercent;
  }

  public void setNewRatePercent(Double newRatePercent) {
    this.newRatePercent = newRatePercent;
  }

  public Map<String, Integer> getCityBreakdown() {
    return cityBreakdown;
  }

  public void setCityBreakdown(Map<String, Integer> cityBreakdown) {
    this.cityBreakdown = cityBreakdown;
  }

  public Long getMedianPricePerM2() {
    return medianPricePerM2;
  }

  public void setMedianPricePerM2(Long medianPricePerM2) {
    this.medianPricePerM2 = medianPricePerM2;
  }

  public Integer getTotalSampleSize() {
    return totalSampleSize;
  }

  public void setTotalSampleSize(Integer totalSampleSize) {
    this.totalSampleSize = totalSampleSize;
  }

  public Boolean isInsufficientData() {
    return insufficientData;
  }

  public Boolean getInsufficientData() {
    return insufficientData;
  }

  public void setInsufficientData(Boolean insufficientData) {
    this.insufficientData = insufficientData;
  }

  public Long getResidentialMedianPricePerM2() {
    return residentialMedianPricePerM2;
  }

  public void setResidentialMedianPricePerM2(Long residentialMedianPricePerM2) {
    this.residentialMedianPricePerM2 = residentialMedianPricePerM2;
  }

  public Long getResidentialAveragePricePerM2() {
    return residentialAveragePricePerM2;
  }

  public void setResidentialAveragePricePerM2(Long residentialAveragePricePerM2) {
    this.residentialAveragePricePerM2 = residentialAveragePricePerM2;
  }

  public Integer getResidentialSampleSize() {
    return residentialSampleSize;
  }

  public void setResidentialSampleSize(Integer residentialSampleSize) {
    this.residentialSampleSize = residentialSampleSize;
  }

  public Boolean isResidentialInsufficientData() {
    return residentialInsufficientData;
  }

  public Boolean getResidentialInsufficientData() {
    return residentialInsufficientData;
  }

  public void setResidentialInsufficientData(Boolean residentialInsufficientData) {
    this.residentialInsufficientData = residentialInsufficientData;
  }

  public Long getPlotMedianPricePerM2() {
    return plotMedianPricePerM2;
  }

  public void setPlotMedianPricePerM2(Long plotMedianPricePerM2) {
    this.plotMedianPricePerM2 = plotMedianPricePerM2;
  }

  public Long getPlotAveragePricePerM2() {
    return plotAveragePricePerM2;
  }

  public void setPlotAveragePricePerM2(Long plotAveragePricePerM2) {
    this.plotAveragePricePerM2 = plotAveragePricePerM2;
  }

  public Integer getPlotSampleSize() {
    return plotSampleSize;
  }

  public void setPlotSampleSize(Integer plotSampleSize) {
    this.plotSampleSize = plotSampleSize;
  }

  public Boolean isPlotInsufficientData() {
    return plotInsufficientData;
  }

  public Boolean getPlotInsufficientData() {
    return plotInsufficientData;
  }

  public void setPlotInsufficientData(Boolean plotInsufficientData) {
    this.plotInsufficientData = plotInsufficientData;
  }

  public List<MarketSegmentStatsDto> getSegments() {
    return segments;
  }

  public void setSegments(List<MarketSegmentStatsDto> segments) {
    this.segments = segments != null ? segments : new ArrayList<>();
  }

  public Integer getPriceDropCount() {
    return priceDropCount;
  }

  public void setPriceDropCount(Integer priceDropCount) {
    this.priceDropCount = priceDropCount;
  }

  public Double getPriceDropRatePercent() {
    return priceDropRatePercent;
  }

  public void setPriceDropRatePercent(Double priceDropRatePercent) {
    this.priceDropRatePercent = priceDropRatePercent;
  }

  public Long getAveragePriceDropAmountHuf() {
    return averagePriceDropAmountHuf;
  }

  public void setAveragePriceDropAmountHuf(Long averagePriceDropAmountHuf) {
    this.averagePriceDropAmountHuf = averagePriceDropAmountHuf;
  }

  public Double getAveragePriceDropPercent() {
    return averagePriceDropPercent;
  }

  public void setAveragePriceDropPercent(Double averagePriceDropPercent) {
    this.averagePriceDropPercent = averagePriceDropPercent;
  }

  public List<PriceDropItemDto> getTopPriceDrops() {
    return topPriceDrops;
  }

  public void setTopPriceDrops(List<PriceDropItemDto> topPriceDrops) {
    this.topPriceDrops = topPriceDrops != null ? topPriceDrops : new ArrayList<>();
  }

  public Integer getBargainCount() {
    return bargainCount;
  }

  public void setBargainCount(Integer bargainCount) {
    this.bargainCount = bargainCount;
  }

  public Double getBargainRatePercent() {
    return bargainRatePercent;
  }

  public void setBargainRatePercent(Double bargainRatePercent) {
    this.bargainRatePercent = bargainRatePercent;
  }
}
