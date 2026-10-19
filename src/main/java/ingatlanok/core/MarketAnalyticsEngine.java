package ingatlanok.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ingatlanok.model.IngatlanItem;
import ingatlanok.presentation.MarketStatsDto;

/** Piaci KPI-k és statisztikák számításáért felelős üzleti motor. */
public class MarketAnalyticsEngine {

  public MarketStatsDto calculateMarketStats(List<IngatlanItem> items) {
    MarketStatsDto stats = new MarketStatsDto();
    if (items == null || items.isEmpty()) {
      return stats;
    }

    List<Double> pricesMillion = new ArrayList<>();
    List<Long> pricesPerM2 = new ArrayList<>();
    Map<String, Integer> cityCounts = new LinkedHashMap<>();
    int newItemsCount = 0;

    for (IngatlanItem item : items) {
      if (item == null) {
        continue;
      }

      if (item.isNew()) {
        newItemsCount++;
      }

      String city = item.getCity();
      if (city != null) {
        cityCounts.put(city, cityCounts.getOrDefault(city, 0) + 1);
      }

      if (item.getPriceInfo() != null && item.getPriceInfo().getAmountMillion() != null) {
        pricesMillion.add(item.getPriceInfo().getAmountMillion());
      }

      if (item.getDetailsInfo() != null && item.getDetailsInfo().getPricePerSquareMeter() != null) {
        pricesPerM2.add(item.getDetailsInfo().getPricePerSquareMeter());
      }
    }

    if (!pricesMillion.isEmpty()) {
      double sum = pricesMillion.stream().mapToDouble(Double::doubleValue).sum();
      double avg = sum / pricesMillion.size();
      stats.setAveragePriceMillion(Math.round(avg * 10.0) / 10.0);
      stats.setMinPriceMillion(
          pricesMillion.stream().mapToDouble(Double::doubleValue).min().orElse(0.0));
      stats.setMaxPriceMillion(
          pricesMillion.stream().mapToDouble(Double::doubleValue).max().orElse(0.0));
    }

    if (!pricesPerM2.isEmpty()) {
      long sumM2 = pricesPerM2.stream().mapToLong(Long::longValue).sum();
      stats.setAveragePricePerM2(sumM2 / pricesPerM2.size());
    }

    double newRate = Math.round(((double) newItemsCount / items.size() * 100.0) * 10.0) / 10.0;
    stats.setNewRatePercent(newRate);
    stats.setCityBreakdown(cityCounts);

    return stats;
  }
}
