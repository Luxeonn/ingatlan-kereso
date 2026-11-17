package ingatlanok.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.model.SearchCriteria;
import ingatlanok.model.raw.ExtractionMethod;
import ingatlanok.model.raw.RawExtractionResult;
import ingatlanok.model.raw.RawPropertyListing;
import ingatlanok.portal.dh.DunaHouseHtmlStrategy;
import ingatlanok.portal.dh.DunaHouseRawParser;
import ingatlanok.portal.dh.DunaHouseUrlBuilder;

class AbstractPaginatedExtractionStrategyTest {

  @Test
  @DisplayName("PortalUrlBuilder.appendPageQueryParam kezeli a határeseteket és elkerüli a duplikálást")
  void appendPageQueryParamHandlesEdgeCases() {
    assertNull(PortalUrlBuilder.appendPageQueryParam(null, "page", 2));
    assertEquals("   ", PortalUrlBuilder.appendPageQueryParam("   ", "page", 2));
    assertEquals(
        "https://dh.hu/lista",
        PortalUrlBuilder.appendPageQueryParam("https://dh.hu/lista", "page", 1));
    assertEquals(
        "https://dh.hu/lista?page=2",
        PortalUrlBuilder.appendPageQueryParam("https://dh.hu/lista", "page", 2));
    assertEquals(
        "https://jofogas.hu/lista?st=s&o=3",
        PortalUrlBuilder.appendPageQueryParam("https://jofogas.hu/lista?st=s", "o", 3));
    assertEquals(
        "https://dh.hu/lista?page=2",
        PortalUrlBuilder.appendPageQueryParam("https://dh.hu/lista?page=2", "page", 4));
    assertEquals(
        "https://dh.hu/lista?foo=1&page=2",
        PortalUrlBuilder.appendPageQueryParam("https://dh.hu/lista?foo=1&page=2", "page", 5));
    assertEquals(
        "https://dh.hu/lista",
        PortalUrlBuilder.appendPageQueryParam("https://dh.hu/lista", " ", 2));
  }

  @Test
  @DisplayName("Virtuális szálakon futó párhuzamos extract hívások teljesen izoláltak")
  void concurrentVirtualThreadExtractionsRemainIsolated() throws Exception {
    DunaHouseHtmlStrategy strategy =
        new DunaHouseHtmlStrategy(
            new DunaHouseUrlBuilder(),
            new DunaHouseRawParser(),
            url -> {
              String id = url.contains("budapest") ? "BP1001" : "DB2002";
              String city = url.contains("budapest") ? "Budapest" : "Debrecen";
              return Jsoup.parse(
                  "<html><body><a href='https://dh.hu/ingatlan/"
                      + id
                      + "/elado-lakas'>"
                      + city
                      + " 55 mFt 60 m2</a></body></html>",
                  url);
            },
            1);

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<Callable<RawExtractionResult>> tasks = new ArrayList<>();
      for (int i = 0; i < 16; i++) {
        String city = (i % 2 == 0) ? "Budapest" : "Debrecen";
        tasks.add(
            () -> {
              SearchCriteria criteria = new SearchCriteria();
              criteria.setCity(city);
              return strategy.extract(criteria);
            });
      }
      List<Future<RawExtractionResult>> futures = executor.invokeAll(tasks);
      for (int i = 0; i < futures.size(); i++) {
        RawExtractionResult res = futures.get(i).get();
        String expectedId = (i % 2 == 0) ? "BP1001" : "DB2002";
        assertEquals(1, res.listings().size());
        assertEquals(expectedId, res.listings().getFirst().nativeId());
      }
    }
  }

  @Test
  @DisplayName("Keresési URL építési kivétel esetén 400-as hibával tér vissza összeomlás nélkül")
  void resolveSearchUrlExceptionReturns400Failure() {
    AbstractPaginatedExtractionStrategy strategy =
        new AbstractPaginatedExtractionStrategy(3, 3) {
          @Override
          public ExtractionMethod method() {
            return ExtractionMethod.HTML;
          }

          @Override
          protected String getPortalLogName() {
            return "TestPortal";
          }

          @Override
          protected String getFallbackBaseUrl() {
            return "https://example.invalid";
          }

          @Override
          protected String resolveSearchUrl(SearchCriteria criteria) {
            throw new IllegalStateException("Simulated URL builder error");
          }

          @Override
          protected String buildPageUrl(
              SearchCriteria criteria, String searchUrl, int page, PaginationState state) {
            return searchUrl;
          }

          @Override
          protected PageStepOutcome fetchAndProcessPage(
              SearchCriteria criteria,
              String searchUrl,
              String pageUrl,
              int page,
              PaginationState state) {
            return PageStepOutcome.stop(PaginationStopReason.EMPTY_PAGE);
          }
        };

    RawExtractionResult result = strategy.extract(new SearchCriteria());
    assertEquals(400, result.httpStatusCode());
    assertEquals("https://example.invalid", result.fallbackUrl());
    assertTrue(result.listings().isEmpty());
  }

  @Test
  @DisplayName("Köztes oldal bot-védelem megőrzi az addig kinyert találatokat")
  void intermediateBotProtectionPreservesCollectedListings() {
    DunaHouseHtmlStrategy strategy =
        new DunaHouseHtmlStrategy(
            new DunaHouseUrlBuilder(),
            new DunaHouseRawParser(),
            url -> {
              if (url.endsWith("?page=2")) {
                return Jsoup.parse("<html><head><title>Just a moment...</title></head></html>", url);
              }
              return Jsoup.parse(
                  "<html><body><a href='https://dh.hu/ingatlan/DH9001/elado-lakas'>"
                      + "Debrecen 50 mFt 50 m2</a></body></html>",
                  url);
            },
            3);

    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");
    RawExtractionResult result = strategy.extract(criteria);

    assertEquals(1, result.listings().size());
    List<RawPropertyListing> listings = result.listings();
    assertEquals("DH9001", listings.getFirst().nativeId());
    assertTrue(
        result.technicalDetails().contains("stopReason=INTERMEDIATE_PAGE_BOT_PROTECTED"));
  }
}
