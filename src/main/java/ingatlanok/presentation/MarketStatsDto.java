package ingatlanok.presentation;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Piaci statisztikák és KPI mutatók DTO a webes felület és külső értesítők számára.
 */
public class MarketStatsDto implements Serializable {

    private Double averagePriceMillion;
    private Long averagePricePerM2;
    private Double minPriceMillion;
    private Double maxPriceMillion;
    private Double newRatePercent;
    private Map<String, Integer> cityBreakdown = new LinkedHashMap<>();

    public MarketStatsDto() {
    }

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
}
