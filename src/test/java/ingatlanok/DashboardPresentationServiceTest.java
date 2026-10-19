package ingatlanok;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.presentation.DashboardPresentationService;
import ingatlanok.presentation.MarketStatsDto;

class DashboardPresentationServiceTest {

  @Test
  void testCalculateMarketStats() {
    DashboardPresentationService service = DashboardPresentationService.getInstance();

    IngatlanItem item1 = new IngatlanItem();
    item1.setCity("Debrecen");
    item1.setNew(true);
    item1.setPriceInfo(new PriceInfo("40 M Ft", 40_000_000L, 40.0));
    item1.setDetailsInfo(new PropertyDetails("40 m²", 40.0, 1.0, 1_000_000L));

    IngatlanItem item2 = new IngatlanItem();
    item2.setCity("Debrecen");
    item2.setNew(false);
    item2.setPriceInfo(new PriceInfo("60 M Ft", 60_000_000L, 60.0));
    item2.setDetailsInfo(new PropertyDetails("50 m²", 50.0, 2.0, 1_200_000L));

    MarketStatsDto stats = service.calculateMarketStats(List.of(item1, item2));

    assertNotNull(stats);
    assertEquals(50.0, stats.getAveragePriceMillion(), "Átlagár (40 + 60) / 2 = 50 M Ft");
    assertEquals(40.0, stats.getMinPriceMillion());
    assertEquals(60.0, stats.getMaxPriceMillion());
    assertEquals(
        1_100_000L,
        stats.getAveragePricePerM2(),
        "Átlagos m2 ár (1.000.000 + 1.200.000) / 2 = 1.100.000 Ft/m²");
    assertEquals(50.0, stats.getNewRatePercent(), "2 elemből 1 új = 50%");
    assertEquals(2, stats.getCityBreakdown().get("Debrecen"));
  }

  @Test
  void testCalculateMarketStatsWithEmptyList() {
    DashboardPresentationService service = DashboardPresentationService.getInstance();
    MarketStatsDto stats = service.calculateMarketStats(List.of());

    assertNotNull(stats);
    assertNull(stats.getAveragePriceMillion());
    assertNull(stats.getAveragePricePerM2());
  }
}
