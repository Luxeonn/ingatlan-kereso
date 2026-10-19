package ingatlanok.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalSource;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.presentation.MarketStatsDto;

class MarketAnalyticsEngineTest {

  @Test
  @DisplayName("Piaci KPI mutatók helyes kiszámítása")
  void testCalculateMarketStats() {
    MarketAnalyticsEngine engine = new MarketAnalyticsEngine();

    IngatlanItem item1 =
        new IngatlanItem(
            "1",
            PortalSource.of("dh"),
            "http://1",
            "Lakás 1",
            new PriceInfo("40 M Ft", 40_000_000L, 40.0),
            "Debrecen",
            new PropertyDetails("50 m²", 50.0, 2.0, 800_000L),
            "2026-10-14",
            true);

    IngatlanItem item2 =
        new IngatlanItem(
            "2",
            PortalSource.of("oc"),
            "http://2",
            "Lakás 2",
            new PriceInfo("60 M Ft", 60_000_000L, 60.0),
            "Debrecen",
            new PropertyDetails("60 m²", 60.0, 3.0, 1_000_000L),
            "2026-10-14",
            false);

    MarketStatsDto stats = engine.calculateMarketStats(List.of(item1, item2));

    assertNotNull(stats);
    assertEquals(50.0, stats.getAveragePriceMillion());
    assertEquals(40.0, stats.getMinPriceMillion());
    assertEquals(60.0, stats.getMaxPriceMillion());
    assertEquals(900_000L, stats.getAveragePricePerM2());
    assertEquals(50.0, stats.getNewRatePercent());
    assertEquals(2, stats.getCityBreakdown().get("Debrecen"));
  }

  @Test
  @DisplayName("Üres lista esetén alapértelmezett statisztikákat ad vissza")
  void testEmptyList() {
    MarketAnalyticsEngine engine = new MarketAnalyticsEngine();
    MarketStatsDto stats = engine.calculateMarketStats(List.of());
    assertNotNull(stats);
    assertNull(stats.getAveragePriceMillion());
  }
}
