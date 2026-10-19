package ingatlanok;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalSource;
import ingatlanok.model.SearchCriteria;
import ingatlanok.persistence.IngatlanRepository;
import ingatlanok.presentation.DashboardPresentationService;
import ingatlanok.presentation.DashboardResponseDto;
import ingatlanok.presentation.MarketStatsDto;
import ingatlanok.presentation.PortalStatusDto;
import ingatlanok.scraper.IngatlanBazarLocationResolver;
import ingatlanok.scraper.PortalScrapeResult;
import ingatlanok.scraper.PortalScrapeStatus;
import ingatlanok.scraper.SidecarResponse;

class RepositoryAndPresentationCoverageTest {

  @org.junit.jupiter.api.BeforeAll
  static void setupMockPortal() {
    ingatlanok.portal.PortalEngine mockEngine =
        new ingatlanok.portal.PortalEngine() {
          @Override
          public ingatlanok.portal.PortalDescriptor getDescriptor() {
            return new ingatlanok.portal.PortalDescriptor(
                "mockbazar",
                "Mock Ingatlanbazár",
                "#336699",
                "https://mockbazar.hu",
                true,
                List.of("lakas"),
                true);
          }

          @Override
          public ingatlanok.scraper.PortalScrapeResult scrape(SearchCriteria criteria) {
            IngatlanItem item =
                new IngatlanItem(
                    "mb1",
                    PortalSource.of("mockbazar"),
                    "https://mockbazar.hu/1",
                    "Debreceni lakás",
                    new ingatlanok.model.PriceInfo("50 M Ft", 50_000_000L, 50.0),
                    "Debrecen",
                    new ingatlanok.model.PropertyDetails("100 m²", 100.0, 3.0, 500_000L),
                    "2026-10-15",
                    true);
            return PortalScrapeResult.success("mockbazar", "Mock Ingatlanbazár", List.of(item), 10);
          }
        };
    ingatlanok.portal.PortalEngineRegistry.getInstance().register(mockEngine);
  }

  @Test
  @DisplayName("IngatlanRepository operations and edge cases")
  void testIngatlanRepositoryMethods() {
    IngatlanRepository repo = IngatlanRepository.getInstance();

    assertNotNull(repo);
    assertTrue(repo.getTotalCount() >= 0);
    assertNotNull(repo.getTrackedCitiesInfo());

    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Eger");
    repo.saveLastCriteria(criteria);

    SearchCriteria loaded = repo.getLastCriteria();
    assertNotNull(loaded);
    assertEquals("Eger", loaded.getCity());

    IngatlanItem item = new IngatlanItem();
    item.setUrl("https://test.hu/item-" + System.currentTimeMillis());
    item.setCity("TesztVaros");
    item.setPrice("30 M Ft");

    repo.saveCityItems("TesztVaros", List.of(item));
    assertTrue(repo.hasCityDatabase("TesztVaros"));
    assertFalse(repo.isNewUrl("TesztVaros", item.getUrl()));
    assertFalse(repo.isViewed(item.getUrl()));

    repo.markAsViewed(item.getUrl());
    assertTrue(repo.isViewed(item.getUrl()));

    List<IngatlanItem> byCity = repo.getItemsByCity("TesztVaros");
    assertNotNull(byCity);
    assertFalse(byCity.isEmpty());
    assertTrue(byCity.stream().anyMatch(i -> item.getUrl().equals(i.getUrl()) && i.isViewed()));

    assertTrue(repo.containsUrl(item.getUrl()));
    assertFalse(repo.markAsViewed(null));
    assertFalse(repo.markAsViewed(""));
    assertFalse(repo.markAsViewed("https://not-exists.hu/12345"));
    assertFalse(repo.isViewed(null));
    assertFalse(repo.isViewed(""));
    assertFalse(repo.isViewed("https://not-exists.hu/12345"));

    IngatlanItem item2 = new IngatlanItem();
    item2.setUrl("https://test.hu/save-all-" + System.currentTimeMillis());
    item2.setCity("TesztVaros2");
    repo.saveAll(List.of(item2));
    assertTrue(repo.containsUrl(item2.getUrl()));
    repo.saveAll(null);
    repo.saveAll(List.of());

    assertNotNull(repo.getAll());
    assertNotNull(repo.getAllUrls());

    repo.clearCityHistory("TesztVaros");
    String lastRun = repo.getLastRunTime();
    assertTrue(lastRun == null || !lastRun.isBlank());
  }

  @Test
  @DisplayName("DashboardPresentationService and DTO coverage")
  void testPresentationAndDtos() {
    DashboardPresentationService service = DashboardPresentationService.getInstance();
    assertNotNull(service);

    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");
    criteria.setPropertyType("lakas");
    criteria.setPortals(List.of("mockbazar"));

    DashboardResponseDto dto = service.executeSearch(criteria);
    assertNotNull(dto);
    assertNotNull(dto.getItems());
    assertNotNull(dto.getMarketStats());
    assertNotNull(dto.getPortalStatuses());
    assertTrue(dto.getTotalFound() >= 0);

    MarketStatsDto stats = dto.getMarketStats();
    assertNotNull(stats.getCityBreakdown());

    stats.setAveragePriceMillion(65.0);
    stats.setAveragePricePerM2(850000L);
    stats.setMinPriceMillion(40.0);
    stats.setMaxPriceMillion(95.0);
    stats.setNewRatePercent(40.0);
    stats.setCityBreakdown(Map.of("Debrecen", 15));

    assertEquals(65.0, stats.getAveragePriceMillion());
    assertEquals(850000L, stats.getAveragePricePerM2());
    assertEquals(40.0, stats.getMinPriceMillion());
    assertEquals(95.0, stats.getMaxPriceMillion());
    assertEquals(40.0, stats.getNewRatePercent());
    assertEquals(1, stats.getCityBreakdown().size());

    dto.setTotalFound(20);
    dto.setNewCount(5);
    dto.setExecutedAt("2026-10-13 22:30:00");
    dto.setPortalCounts(Map.of("dh", 5));
    dto.setCityCounts(Map.of("Debrecen", 5));
    dto.setNewCitiesInitialized(List.of("Debrecen"));
    dto.setItems(List.of());

    assertEquals(20, dto.getTotalFound());
    assertEquals(5, dto.getNewCount());
    assertEquals("2026-10-13 22:30:00", dto.getExecutedAt());
    assertEquals(1, dto.getPortalCounts().size());
    assertEquals(1, dto.getCityCounts().size());
    assertEquals(1, dto.getNewCitiesInitialized().size());
    assertTrue(dto.getItems().isEmpty());

    PortalStatusDto portalStatus =
        new PortalStatusDto("dh", "Duna House", "SUCCESS", 5, 120L, "https://dh.hu", "OK");
    assertEquals("dh", portalStatus.getPortalId());
    assertEquals("Duna House", portalStatus.getPortalName());
    assertEquals("SUCCESS", portalStatus.getStatus());
    assertEquals(5, portalStatus.getFoundCount());
    assertEquals(120L, portalStatus.getLatencyMs());
    assertEquals("https://dh.hu", portalStatus.getFallbackUrl());
    assertEquals("OK", portalStatus.getMessage());

    portalStatus.setFoundCount(10);
    portalStatus.setLatencyMs(200L);
    portalStatus.setStatus("EMPTY");
    assertEquals(10, portalStatus.getFoundCount());
    assertEquals(200L, portalStatus.getLatencyMs());
    assertEquals("EMPTY", portalStatus.getStatus());
  }

  @Test
  @DisplayName("SidecarResponse getters, setters, and item conversion")
  void testSidecarResponseCoverage() {
    SidecarResponse response = new SidecarResponse();
    response.setPortalId("ingatlancom");
    response.setStatus("SUCCESS");
    response.setErrorMessage(null);
    response.setElapsedMs(500L);

    assertTrue(response.isSuccess());
    assertEquals("ingatlancom", response.getPortalId());
    assertEquals("SUCCESS", response.getStatus());
    assertNull(response.getErrorMessage());
    assertEquals(500L, response.getElapsedMs());

    SidecarResponse.SidecarItem sidecarItem =
        new SidecarResponse.SidecarItem(
            "sc-1",
            "https://ingatlan.com/sc-1",
            "Kiadó vagy eladó ház",
            55.5,
            "55.5 M Ft",
            75.0,
            3.0);

    assertEquals("sc-1", sidecarItem.getId());
    assertEquals("https://ingatlan.com/sc-1", sidecarItem.getUrl());
    assertEquals("Kiadó vagy eladó ház", sidecarItem.getTitle());
    assertEquals(55.5, sidecarItem.getPriceMillion());
    assertEquals("55.5 M Ft", sidecarItem.getPriceRaw());
    assertEquals(75.0, sidecarItem.getAreaSquareMeters());
    assertEquals(3.0, sidecarItem.getRoomCount());

    IngatlanItem item =
        sidecarItem.toIngatlanItem(PortalSource.of("ingatlan.com"), "Debrecen", "2026-10-13", true);
    assertNotNull(item);
    assertEquals("sc-1", item.getId());
    assertEquals("Debrecen", item.getCity());
    assertTrue(item.isNew());
    assertNotNull(item.getPriceInfo());
    assertEquals(55.5, item.getPriceInfo().getAmountMillion());

    response.setItems(List.of(sidecarItem));
    assertEquals(1, response.getItems().size());

    SidecarResponse emptyResponse = new SidecarResponse();
    assertNotNull(emptyResponse.getItems());
    assertFalse(emptyResponse.isSuccess());
  }

  @Test
  @DisplayName("IngatlanBazarLocationResolver cache and resolving")
  void testIngatlanBazarLocationResolver() {
    Optional<Integer> debrecenId = IngatlanBazarLocationResolver.resolveLocationId("Debrecen");
    assertTrue(debrecenId.isPresent());
    assertEquals(1435, debrecenId.get());

    Optional<Integer> cachedDebrecen = IngatlanBazarLocationResolver.resolveLocationId("debrecen");
    assertTrue(cachedDebrecen.isPresent());
    assertEquals(1435, cachedDebrecen.get());

    Optional<Integer> budapestId = IngatlanBazarLocationResolver.resolveLocationId("Budapest");
    assertTrue(budapestId.isPresent());

    Optional<Integer> unknown =
        IngatlanBazarLocationResolver.resolveLocationId("IsmeretlenNemLetezoTelepules123");
    assertTrue(unknown.isEmpty());
  }

  @Test
  @DisplayName("PortalScrapeResult factory methods and getters/setters")
  void testPortalScrapeResultCoverage() {
    PortalScrapeResult emptyResult = new PortalScrapeResult();
    emptyResult.setPortalId("dh");
    emptyResult.setPortalName("Duna House");
    emptyResult.setStatus(PortalScrapeStatus.SUCCESS);
    emptyResult.setItems(List.of());
    emptyResult.setFallbackUrl("https://dh.hu");
    emptyResult.setMessage("OK");
    emptyResult.setExecutionTimeMs(120L);

    assertEquals("dh", emptyResult.getPortalId());
    assertEquals("Duna House", emptyResult.getPortalName());
    assertEquals(PortalScrapeStatus.SUCCESS, emptyResult.getStatus());
    assertTrue(emptyResult.getItems().isEmpty());
    assertEquals("https://dh.hu", emptyResult.getFallbackUrl());
    assertEquals("OK", emptyResult.getMessage());
    assertEquals(120L, emptyResult.getExecutionTimeMs());

    PortalScrapeResult success =
        PortalScrapeResult.success("oc", "Otthon Centrum", List.of(new IngatlanItem()), 150L);
    assertEquals(PortalScrapeStatus.SUCCESS, success.getStatus());
    assertEquals(1, success.getItems().size());

    PortalScrapeResult successEmpty =
        PortalScrapeResult.success("oc", "Otthon Centrum", List.of(), 150L);
    assertEquals(PortalScrapeStatus.EMPTY, successEmpty.getStatus());

    PortalScrapeResult empty =
        PortalScrapeResult.empty("jf", "Jófogás", "https://jofogas.hu", 200L);
    assertEquals(PortalScrapeStatus.EMPTY, empty.getStatus());
    assertEquals("https://jofogas.hu", empty.getFallbackUrl());

    PortalScrapeResult bot =
        PortalScrapeResult.botProtected("ic", "Ingatlan.com", "https://ingatlan.com", 300L);
    assertEquals(PortalScrapeStatus.BOT_PROTECTED, bot.getStatus());

    PortalScrapeResult spa =
        PortalScrapeResult.spaFallback("spa", "SPA Portál", "https://spa.hu", 250L);
    assertEquals(PortalScrapeStatus.SPA_FALLBACK, spa.getStatus());

    PortalScrapeResult failure =
        PortalScrapeResult.failure("err", "Hiba Portál", "Connection timeout", 400L);
    assertEquals(PortalScrapeStatus.FAILED, failure.getStatus());
    assertTrue(failure.getMessage().contains("Connection timeout"));
  }
}
