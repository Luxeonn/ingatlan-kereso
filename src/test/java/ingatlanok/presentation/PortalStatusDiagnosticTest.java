package ingatlanok.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.core.DeduplicationEngine;
import ingatlanok.core.MarketAnalyticsEngine;
import ingatlanok.core.PropertyFilterEngine;
import ingatlanok.core.SearchExecutionEngine;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.PortalScrapeStatus;
import ingatlanok.model.PortalSource;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;
import ingatlanok.persistence.InMemoryListingStorage;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalEngine;
import ingatlanok.portal.PortalEngineRegistry;

class PortalStatusDiagnosticTest {

  @Test
  @DisplayName("PortalStatusDto full constructor and property accessors")
  void testPortalStatusDtoProperties() {
    PortalStatusDto dto =
        new PortalStatusDto(
            "oc",
            "Otthon Centrum",
            "FAILED",
            0,
            350L,
            "https://oc.hu/elado/lakas",
            "Hiba történt",
            500,
            "Read timed out after 15000ms");

    assertEquals("oc", dto.getPortalId());
    assertEquals("Otthon Centrum", dto.getPortalName());
    assertEquals("FAILED", dto.getStatus());
    assertEquals(0, dto.getFoundCount());
    assertEquals(350L, dto.getLatencyMs());
    assertEquals("https://oc.hu/elado/lakas", dto.getFallbackUrl());
    assertEquals("Hiba történt", dto.getMessage());
    assertEquals(500, dto.getHttpStatusCode());
    assertEquals("Read timed out after 15000ms", dto.getTechnicalDetails());

    dto =
        dto.toBuilder()
            .portalId("dh")
            .portalName("Duna House")
            .status("SUCCESS")
            .foundCount(15)
            .latencyMs(420L)
            .fallbackUrl("https://dh.hu")
            .message("Sikeres")
            .httpStatusCode(200)
            .technicalDetails("Minden rendben")
            .build();

    assertEquals("dh", dto.getPortalId());
    assertEquals("Duna House", dto.getPortalName());
    assertEquals("SUCCESS", dto.getStatus());
    assertEquals(15, dto.getFoundCount());
    assertEquals(420L, dto.getLatencyMs());
    assertEquals("https://dh.hu", dto.getFallbackUrl());
    assertEquals("Sikeres", dto.getMessage());
    assertEquals(200, dto.getHttpStatusCode());
    assertEquals("Minden rendben", dto.getTechnicalDetails());
  }

  @Test
  @DisplayName("PortalScrapeResult diagnostic factory methods with status codes")
  void testPortalScrapeResultDiagnostics() {
    PortalScrapeResult success =
        PortalScrapeResult.success("dh", "Duna House", Collections.emptyList(), 150L);
    assertEquals(PortalScrapeStatus.EMPTY, success.getStatus());
    assertEquals(200, success.getHttpStatusCode());
    assertNotNull(success.getTechnicalDetails());

    PortalScrapeResult botBlocked =
        PortalScrapeResult.botProtected("ingatlancom", "Ingatlan.com", "https://ingatlan.com", 80L);
    assertEquals(PortalScrapeStatus.BOT_PROTECTED, botBlocked.getStatus());
    assertEquals(403, botBlocked.getHttpStatusCode());
    assertTrue(botBlocked.getTechnicalDetails().contains("403 Forbidden"));

    PortalScrapeResult failureWithCode =
        PortalScrapeResult.failure(
            "oc", "Otthon Centrum", "Kapcsolati hiba", 200L, 503, "https://oc.hu");
    assertEquals(PortalScrapeStatus.FAILED, failureWithCode.getStatus());
    assertEquals(503, failureWithCode.getHttpStatusCode());
    assertEquals("https://oc.hu", failureWithCode.getFallbackUrl());
    assertEquals("Kapcsolati hiba", failureWithCode.getTechnicalDetails());
  }

  @Test
  @DisplayName("SearchResult contains portalStatuses list")
  void testSearchResultPortalStatuses() {
    SearchResult result = new SearchResult();
    assertNotNull(result.getPortalStatuses());
    assertTrue(result.getPortalStatuses().isEmpty());

    PortalStatusDto dto =
        new PortalStatusDto("ib", "Ingatlanbazár", "SUCCESS", 25, 300L, null, "Sikeres");
    result.setPortalStatuses(List.of(dto));

    assertEquals(1, result.getPortalStatuses().size());
    assertEquals("ib", result.getPortalStatuses().get(0).getPortalId());
  }

  @Test
  @DisplayName("DashboardPresentationService populates portalStatuses in response")
  void testDashboardPresentationServicePortalStatuses() {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCities(List.of("Debrecen"));
    criteria.setPortals(List.of("ingatlanbazar"));

    PortalEngine engine = new PortalEngine() {
      @Override
      public PortalDescriptor getDescriptor() {
        return new PortalDescriptor("ingatlanbazar", "Ingatlanbazár", "#123456",
            "https://ingatlanbazar.hu", true, List.of("lakas"), true);
      }

      @Override
      public PortalScrapeResult scrape(SearchCriteria searchCriteria) {
        IngatlanItem item = new IngatlanItem("1", PortalSource.of("ingatlanbazar"),
            "https://ingatlanbazar.hu/1", "Teszt", new PriceInfo("40 M Ft", 40_000_000L, 40.0),
            "Debrecen", new PropertyDetails("60 m²", 60.0, 2.0, 666_667L), "2026-10-15", true);
        return PortalScrapeResult.success("ingatlanbazar", "Ingatlanbazár", List.of(item), 1);
      }
    };
    InMemoryListingStorage storage = new InMemoryListingStorage();
    MarketAnalyticsEngine analytics = new MarketAnalyticsEngine();
    SearchExecutionEngine search = new SearchExecutionEngine(new PortalEngineRegistry(List.of(engine)),
        storage, storage, new PropertyFilterEngine(), new DeduplicationEngine(storage), analytics);
    DashboardResponseDto response =
        new DashboardPresentationService(search, analytics).executeSearch(criteria);
    assertNotNull(response);
    assertNotNull(response.getPortalStatuses());
    assertFalse(response.getPortalStatuses().isEmpty());

    PortalStatusDto first = response.getPortalStatuses().get(0);
    assertNotNull(first.getPortalId());
    assertNotNull(first.getPortalName());
    assertNotNull(first.getStatus());
  }

  @Test
  @DisplayName(
      "Többvárosos keresésnél a portalStatuses minden aktív portált pontosan egyszer tartalmaz"
          + " aggregált találatszámmal (#251)")
  void testMultiCitySearchAggregatesPortalStatusesWithoutDuplicates() {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCities(List.of("Debrecen", "Hajdúböszörmény"));
    criteria.setPortals(List.of("dh", "ingatlancom", "oc"));
    criteria.setMinPriceMillion(30);
    criteria.setMaxPriceMillion(80);
    criteria.setMinArea(40);
    criteria.setMaxArea(120);

    PortalEngine dhEngine =
        new PortalEngine() {
          @Override
          public PortalDescriptor getDescriptor() {
            return new PortalDescriptor(
                "dh", "Duna House", "#008638", "https://dh.hu", true, List.of("lakas"), true);
          }

          @Override
          public PortalScrapeResult scrape(SearchCriteria searchCriteria) {
            String city = searchCriteria.getCity();
            String slug = "Debrecen".equals(city) ? "debrecen-1" : "hb-1";
            IngatlanItem item =
                new IngatlanItem(
                    slug,
                    PortalSource.of("Duna House"),
                    "https://dh.hu/ingatlan/" + slug,
                    city + " lakás",
                    new PriceInfo("50 M Ft", 50_000_000L, 50.0),
                    city,
                    new PropertyDetails("60 m²", 60.0, 2.0, 833_333L),
                    "2026-10-20 10:00:00",
                    false);
            return PortalScrapeResult.success("dh", "Duna House", List.of(item), 110);
          }
        };

    PortalEngine icEngine =
        new PortalEngine() {
          @Override
          public PortalDescriptor getDescriptor() {
            return new PortalDescriptor(
                "ingatlancom",
                "Ingatlan.com",
                "#0284c7",
                "https://ingatlan.com",
                true,
                List.of("lakas"),
                false);
          }

          @Override
          public PortalScrapeResult scrape(SearchCriteria searchCriteria) {
            return PortalScrapeResult.botProtected(
                "ingatlancom", "Ingatlan.com", "https://ingatlan.com/lista", 50);
          }
        };

    PortalEngine ocEngine =
        new PortalEngine() {
          @Override
          public PortalDescriptor getDescriptor() {
            return new PortalDescriptor(
                "oc", "Otthon Centrum", "#ea580c", "https://oc.hu", true, List.of("lakas"), true);
          }

          @Override
          public PortalScrapeResult scrape(SearchCriteria searchCriteria) {
            if ("Debrecen".equals(searchCriteria.getCity())) {
              return PortalScrapeResult.empty("oc", "Otthon Centrum", "https://oc.hu/debrecen", 40);
            }
            return PortalScrapeResult.failure(
                "oc", "Otthon Centrum", "HTTP 503", 60, 503, "https://oc.hu/hb");
          }
        };

    InMemoryListingStorage storage = new InMemoryListingStorage();
    MarketAnalyticsEngine analytics = new MarketAnalyticsEngine();
    SearchExecutionEngine search =
        new SearchExecutionEngine(
            new PortalEngineRegistry(List.of(dhEngine, icEngine, ocEngine)),
            storage,
            storage,
            new PropertyFilterEngine(),
            new DeduplicationEngine(storage),
            analytics);

    DashboardResponseDto response =
        new DashboardPresentationService(search, analytics).executeSearch(criteria);

    List<PortalStatusDto> statuses = response.getPortalStatuses();
    assertEquals(3, statuses.size(), "Minden aktív portál pontosan egyszer szerepelhet");
    long uniquePortalIds = statuses.stream().map(PortalStatusDto::getPortalId).distinct().count();
    assertEquals(3L, uniquePortalIds);

    PortalStatusDto dhStatus =
        statuses.stream().filter(s -> "dh".equals(s.getPortalId())).findFirst().orElseThrow();
    assertEquals("SUCCESS", dhStatus.getStatus());
    assertEquals(2, dhStatus.getFoundCount());
    assertEquals(220L, dhStatus.getLatencyMs());

    PortalStatusDto icStatus =
        statuses.stream()
            .filter(s -> "ingatlancom".equals(s.getPortalId()))
            .findFirst()
            .orElseThrow();
    assertEquals("BOT_PROTECTED", icStatus.getStatus());
    assertEquals(0, icStatus.getFoundCount());
    assertEquals(100L, icStatus.getLatencyMs());

    PortalStatusDto ocStatus =
        statuses.stream().filter(s -> "oc".equals(s.getPortalId())).findFirst().orElseThrow();
    assertEquals("FAILED", ocStatus.getStatus());
    assertEquals(0, ocStatus.getFoundCount());
    assertEquals(100L, ocStatus.getLatencyMs());
  }
}
