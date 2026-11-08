package ingatlanok.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ingatlanok.common.AppLogger;
import ingatlanok.core.DeduplicationEngine;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.PortalScrapeStatus;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.raw.RawExtractionResult;
import ingatlanok.portal.dh.DunaHouseHtmlStrategy;
import ingatlanok.portal.dh.DunaHousePortalEngine;
import ingatlanok.portal.dh.DunaHouseRawParser;
import ingatlanok.portal.dh.DunaHouseUrlBuilder;
import ingatlanok.portal.ingatlanbazar.IngatlanBazarApiStrategy;
import ingatlanok.portal.ingatlanbazar.IngatlanBazarPortalEngine;
import ingatlanok.portal.ingatlanbazar.IngatlanBazarQueryBuilder;
import ingatlanok.portal.ingatlanbazar.IngatlanBazarRawParser;
import ingatlanok.portal.jofogas.JofogasPortalEngine;
import ingatlanok.portal.koltozzbe.KoltozzbeHtmlStrategy;
import ingatlanok.portal.koltozzbe.KoltozzbeJsonLdStrategy;
import ingatlanok.portal.koltozzbe.KoltozzbePortalEngine;
import ingatlanok.portal.koltozzbe.KoltozzbeRawParser;
import ingatlanok.portal.koltozzbe.KoltozzbeUrlBuilder;
import ingatlanok.portal.oc.OtthonCentrumPortalEngine;
import ingatlanok.testsupport.TestFixtures;

@DisplayName("Issue #373 - Lapozás, találati teljesség és duplikáció regressziós tesztek")
class PaginationCompletenessAndDeduplicationRegressionTest {

  private SearchCriteria criteria;

  @BeforeEach
  void setUp() {
    criteria = new SearchCriteria();
    criteria.setCity("Debrecen");
    criteria.setPropertyType("lakas");
  }

  @Nested
  @DisplayName("1. Többoldalas lapozás és pontos kérésszám/sorrend minden portálmechanizmusra")
  class MultiPagePaginationTests {

    @Test
    @DisplayName("Duna House: 3 oldalas lapozás (/oldal-N) hiánytalan rekordkészlettel és pontos URL-sorrenddel")
    void dunaHouseThreePagePagination() {
      DunaHousePortalEngine engine = new DunaHousePortalEngine();
      List<String> requestedUrls = new ArrayList<>();

      PortalScrapeResult result = engine.scrape(criteria, url -> {
        requestedUrls.add(url);
        if (url.endsWith("/oldal-2")) {
          return Jsoup.parse(dhPageHtml("LK000201", 49, 55, "LK000202", 52, 60), url);
        }
        if (url.endsWith("/oldal-3")) {
          return Jsoup.parse(dhPageHtml("LK000301", 58, 66), url);
        }
        return Jsoup.parse(dhPageHtml("LK000101", 42, 48, "LK000102", 45, 51), url);
      }, 3);

      assertEquals(PortalScrapeStatus.SUCCESS, result.getStatus());
      assertEquals(3, requestedUrls.size());
      assertFalse(requestedUrls.get(0).contains("/oldal-"));
      assertTrue(requestedUrls.get(1).endsWith("/oldal-2"));
      assertTrue(requestedUrls.get(2).endsWith("/oldal-3"));
      assertEquals(5, result.getItems().size());
      assertNotNull(result.getTechnicalDetails());
      assertTrue(result.getTechnicalDetails().contains("pages=3"));
      assertTrue(result.getTechnicalDetails().contains("stopReason=MAX_PAGES_REACHED"));
    }

    @Test
    @DisplayName("Otthon Centrum: 3 oldalas lapozás (?page=N) hiánytalan rekordkészlettel és pontos URL-sorrenddel")
    void otthonCentrumThreePagePagination() {
      OtthonCentrumPortalEngine engine = new OtthonCentrumPortalEngine();
      List<String> requestedUrls = new ArrayList<>();

      PortalScrapeResult result = engine.scrape(criteria, url -> {
        requestedUrls.add(url);
        if (url.endsWith("?page=2")) {
          return Jsoup.parse(ocPageHtml("H500201", 51_000_000L, 58, "H500202", 54_000_000L, 62), url);
        }
        if (url.endsWith("?page=3")) {
          return Jsoup.parse(ocPageHtml("H500301", 61_000_000L, 70), url);
        }
        return Jsoup.parse(ocPageHtml("H500101", 44_000_000L, 50, "H500102", 47_000_000L, 53), url);
      }, 3);

      assertEquals(PortalScrapeStatus.SUCCESS, result.getStatus());
      assertEquals(3, requestedUrls.size());
      assertFalse(requestedUrls.get(0).contains("?page="));
      assertTrue(requestedUrls.get(1).endsWith("?page=2"));
      assertTrue(requestedUrls.get(2).endsWith("?page=3"));
      assertEquals(5, result.getItems().size());
      assertTrue(result.getTechnicalDetails().contains("pages=3"));
      assertTrue(result.getTechnicalDetails().contains("stopReason=MAX_PAGES_REACHED"));
    }

    @Test
    @DisplayName("Jófogás: 3 oldalas lapozás (&o=N) hiánytalan rekordkészlettel és pontos URL-sorrenddel")
    void jofogasThreePagePagination() {
      JofogasPortalEngine engine = new JofogasPortalEngine();
      List<String> requestedUrls = new ArrayList<>();

      PortalScrapeResult result = engine.scrape(criteria, url -> {
        requestedUrls.add(url);
        if (url.endsWith("&o=2")) {
          return Jsoup.parse(jofogasPageHtml("900201", 48, 56, "900202", 53, 61), url);
        }
        if (url.endsWith("&o=3")) {
          return Jsoup.parse(jofogasPageHtml("900301", 62, 74), url);
        }
        return Jsoup.parse(jofogasPageHtml("900101", 39, 45, "900102", 44, 50), url);
      }, 3);

      assertEquals(PortalScrapeStatus.SUCCESS, result.getStatus());
      assertEquals(3, requestedUrls.size());
      assertFalse(requestedUrls.get(0).contains("&o="));
      assertTrue(requestedUrls.get(1).endsWith("&o=2"));
      assertTrue(requestedUrls.get(2).endsWith("&o=3"));
      assertEquals(5, result.getItems().size());
      assertTrue(result.getTechnicalDetails().contains("pages=3"));
      assertTrue(result.getTechnicalDetails().contains("stopReason=MAX_PAGES_REACHED"));
    }

    @Test
    @DisplayName("Költözzbe: 3 oldalas lapozás (?page=N) HTML + JSON-LD + rel=next / pagination linkek alapján")
    void koltozzbeThreePagePaginationAndSubStrategies() {
      KoltozzbePortalEngine engine = new KoltozzbePortalEngine();
      List<String> requestedUrls = new ArrayList<>();
      SearchCriteria filteredCriteria = new SearchCriteria();
      filteredCriteria.setCity("Debrecen");
      filteredCriteria.setPropertyType("lakas");
      filteredCriteria.setMinPriceMillion(20);

      PortalScrapeResult result = engine.scrape(filteredCriteria, url -> {
        requestedUrls.add(url);
        if (url.endsWith("&page=2")) {
          return Jsoup.parse(koltozzbePageHtml("201", 51, 58, 3), url);
        }
        if (url.endsWith("&page=3")) {
          return Jsoup.parse(koltozzbePageHtml("301", 64, 72, null), url);
        }
        return Jsoup.parse(koltozzbePageHtml("101", 43, 49, 2), url);
      }, 5);

      assertEquals(PortalScrapeStatus.SUCCESS, result.getStatus());
      assertEquals(3, requestedUrls.size());
      assertTrue(requestedUrls.get(0).contains("?p1=20000000"));
      assertFalse(requestedUrls.get(0).contains("page="));
      assertTrue(requestedUrls.get(1).endsWith("&page=2"));
      assertTrue(requestedUrls.get(2).endsWith("&page=3"));
      assertEquals(3, result.getItems().size());
      assertTrue(result.getTechnicalDetails().contains("pages=3"));
      assertTrue(result.getTechnicalDetails().contains("stopReason=NO_NEXT_PAGE"));

      List<String> jsonLdUrls = new ArrayList<>();
      KoltozzbeJsonLdStrategy jsonLdStrategy = new KoltozzbeJsonLdStrategy(
          new KoltozzbeUrlBuilder(),
          new KoltozzbeRawParser(),
          url -> {
            jsonLdUrls.add(url);
            if (url.endsWith("&page=2")) {
              return Jsoup.parse(koltozzbeJsonLdPageHtml("802", 57_000_000L, null), url);
            }
            return Jsoup.parse(koltozzbeJsonLdPageHtml("801", 47_000_000L, 2), url);
          },
          4);
      RawExtractionResult jsonLdResult = jsonLdStrategy.extract(filteredCriteria);
      assertEquals(PortalScrapeStatus.SUCCESS, jsonLdResult.status());
      assertEquals(2, jsonLdUrls.size());
      assertEquals(2, jsonLdResult.listings().size());
      assertTrue(jsonLdResult.technicalDetails().contains("stopReason=NO_NEXT_PAGE"));
    }

    @Test
    @DisplayName("Ingatlanbazár REST API: 3 oldalas lapozás (limit=50&page=N) pontos kéréssorrenddel")
    void ingatlanBazarThreePageApiPagination() {
      IngatlanBazarPortalEngine engine = new IngatlanBazarPortalEngine();
      List<String> requestedUrls = new ArrayList<>();

      PortalScrapeResult result = engine.scrape(criteria, url -> {
        requestedUrls.add(url);
        if (url.contains("&page=2")) {
          return ibJsonPage("IB-201", "/ingatlan/debrecen-201", 52_000_000L, 58,
              "IB-202", "/ingatlan/debrecen-202", 56_000_000L, 63);
        }
        if (url.contains("&page=3")) {
          return ibJsonPage("IB-301", "/ingatlan/debrecen-301", 65_000_000L, 75);
        }
        return ibJsonPage("IB-101", "/ingatlan/debrecen-101", 44_000_000L, 49,
            "IB-102", "/ingatlan/debrecen-102", 48_000_000L, 54);
      }, 3);

      assertEquals(PortalScrapeStatus.SUCCESS, result.getStatus());
      assertEquals(3, requestedUrls.size());
      assertTrue(requestedUrls.get(0).contains("limit=50&page=1"));
      assertTrue(requestedUrls.get(1).contains("limit=50&page=2"));
      assertTrue(requestedUrls.get(2).contains("limit=50&page=3"));
      assertEquals(5, result.getItems().size());
      assertTrue(result.getTechnicalDetails().contains("pages=3"));
      assertTrue(result.getTechnicalDetails().contains("stopReason=MAX_PAGES_REACHED"));
    }
  }

  @Nested
  @DisplayName("2. Utolsó oldal és üres oldal (első oldal üres vs. köztes üres oldal) kezelése")
  class LastPageAndEmptyPageHandlingTests {

    @Test
    @DisplayName("Üres 1. oldal esetén azonnal megáll (FIRST_PAGE_EMPTY) és nem kér le 2. oldalt")
    void emptyFirstPageStopsImmediatelyWithoutRequestingPageTwo() {
      List<String> dhUrls = new ArrayList<>();
      PortalScrapeResult dhRes = new DunaHousePortalEngine().scrape(criteria, url -> {
        dhUrls.add(url);
        return Jsoup.parse("<html><body><div>Nincs találat</div></body></html>", url);
      }, 3);
      assertEquals(PortalScrapeStatus.EMPTY, dhRes.getStatus());
      assertEquals(1, dhUrls.size(), "DH should not request page 2 when page 1 is empty");

      List<String> ocUrls = new ArrayList<>();
      PortalScrapeResult ocRes = new OtthonCentrumPortalEngine().scrape(criteria, url -> {
        ocUrls.add(url);
        return Jsoup.parse("<html><body><div>0 találat</div></body></html>", url);
      }, 3);
      assertEquals(PortalScrapeStatus.EMPTY, ocRes.getStatus());
      assertEquals(1, ocUrls.size(), "OC should not request page 2 when page 1 is empty");

      List<String> jfUrls = new ArrayList<>();
      PortalScrapeResult jfRes = new JofogasPortalEngine().scrape(criteria, url -> {
        jfUrls.add(url);
        return Jsoup.parse("<html><body><div>Nincs találat</div></body></html>", url);
      }, 3);
      assertEquals(PortalScrapeStatus.EMPTY, jfRes.getStatus());
      assertEquals(1, jfUrls.size(), "Jófogás should not request page 2 when page 1 is empty");

      List<String> kbUrls = new ArrayList<>();
      PortalScrapeResult kbRes = new KoltozzbePortalEngine().scrape(criteria, url -> {
        kbUrls.add(url);
        return Jsoup.parse("<html><body><div>Nincs találat</div></body></html>", url);
      }, 3);
      assertEquals(PortalScrapeStatus.EMPTY, kbRes.getStatus());
      assertEquals(1, kbUrls.size(), "Költözzbe should not request page 2 when page 1 is empty");

      List<String> ibUrls = new ArrayList<>();
      IngatlanBazarPortalEngine ibEngine = new IngatlanBazarPortalEngine();
      PortalScrapeResult ibRes = ibEngine.scrape(criteria, url -> {
        ibUrls.add(url);
        return "{\"hits\":[]}";
      }, 3);
      assertEquals(PortalScrapeStatus.EMPTY, ibRes.getStatus());
      assertEquals(1, ibUrls.size(), "Ingatlanbazár should not request page 2 when page 1 is empty");
    }

    @Test
    @DisplayName("Üres 2. oldal esetén megőrzi az 1. oldal találatait és nem kér le 3. oldalt (EMPTY_PAGE)")
    void emptySecondPageStopsWithoutRequestingPageThree() {
      List<String> dhUrls = new ArrayList<>();
      PortalScrapeResult dhRes = new DunaHousePortalEngine().scrape(criteria, url -> {
        dhUrls.add(url);
        if (url.endsWith("/oldal-2")) {
          return Jsoup.parse("<html><body><div>Üres oldal</div></body></html>", url);
        }
        return Jsoup.parse(dhPageHtml("LK000101", 45, 52), url);
      }, 4);
      assertEquals(PortalScrapeStatus.SUCCESS, dhRes.getStatus());
      assertEquals(2, dhUrls.size(), "DH must stop on empty page 2 and not request page 3");
      assertEquals(1, dhRes.getItems().size());
      assertTrue(dhRes.getTechnicalDetails().contains("stopReason=EMPTY_PAGE"));

      List<String> ibUrls = new ArrayList<>();
      IngatlanBazarPortalEngine ibEngine = new IngatlanBazarPortalEngine();
      PortalScrapeResult ibRes = ibEngine.scrape(criteria, url -> {
        ibUrls.add(url);
        if (url.contains("&page=2")) {
          return "{\"hits\":[]}";
        }
        return ibJsonPage("IB-101", "/ingatlan/debrecen-101", 44_000_000L, 49);
      }, 4);
      assertEquals(PortalScrapeStatus.SUCCESS, ibRes.getStatus());
      assertEquals(2, ibUrls.size(), "IB must stop on empty page 2 and not request page 3");
      assertEquals(1, ibRes.getItems().size());
      assertTrue(ibRes.getTechnicalDetails().contains("stopReason=EMPTY_PAGE"));
    }

    @Test
    @DisplayName("Költözzbe: egyoldalas találati lista (.pagination elem nélkül) nem kér le 2. oldalt")
    void koltozzbeSinglePageWithoutPaginationElementDoesNotFetchSecondPage() {
      List<String> requestedUrls = new ArrayList<>();
      PortalScrapeResult result = new KoltozzbePortalEngine().scrape(criteria, url -> {
        requestedUrls.add(url);
        return Jsoup.parse(koltozzbePageHtml("101", 43, 49, null), url);
      }, 3);

      assertEquals(PortalScrapeStatus.SUCCESS, result.getStatus());
      assertEquals(1, requestedUrls.size(), "Single page without pagination links must not fetch page 2");
      assertEquals(1, result.getItems().size());
      assertTrue(result.getTechnicalDetails().contains("stopReason=NO_NEXT_PAGE"));
    }
  }

  @Nested
  @DisplayName("3. Végtelen ciklus és ismétlődő oldal / URL / token elleni védelem")
  class InfiniteLoopAndRepeatedPageGuardTests {

    @Test
    @DisplayName("Ismétlődő oldaltartalom (2. oldal ugyanazokat a hirdetéseket adja) -> REPEATED_PAGE_CONTENT")
    void repeatedPageContentStopsEarlyWithoutFetchingFurtherPages() {
      String identicalDhHtml = dhPageHtml("LK000101", 45, 52, "LK000102", 49, 58);
      List<String> dhUrls = new ArrayList<>();
      PortalScrapeResult dhRes = new DunaHousePortalEngine().scrape(criteria, url -> {
        dhUrls.add(url);
        return Jsoup.parse(identicalDhHtml, url);
      }, 5);

      assertEquals(PortalScrapeStatus.SUCCESS, dhRes.getStatus());
      assertEquals(2, dhUrls.size(), "Must stop at page 2 when page 2 has 0 new listings");
      assertEquals(2, dhRes.getItems().size());
      assertTrue(dhRes.getTechnicalDetails().contains("stopReason=REPEATED_PAGE_CONTENT"));

      String identicalKbHtml = koltozzbePageHtml("101", 45, 52, 99);
      List<String> kbUrls = new ArrayList<>();
      PortalScrapeResult kbRes = new KoltozzbePortalEngine().scrape(criteria, url -> {
        kbUrls.add(url);
        return Jsoup.parse(identicalKbHtml, url);
      }, 5);

      assertEquals(PortalScrapeStatus.SUCCESS, kbRes.getStatus());
      assertEquals(2, kbUrls.size(), "Költözzbe must stop at page 2 when all listings are duplicates");
      assertEquals(1, kbRes.getItems().size());
      assertTrue(kbRes.getTechnicalDetails().contains("stopReason=REPEATED_PAGE_CONTENT"));
    }

    @Test
    @DisplayName("Ismétlődő oldal-URL / hibás lapozó token vagy önmagára mutató rel=next nem okoz végtelen ciklust")
    void duplicatePageUrlOrSelfPointingNextLinkStopsImmediately() {
      DunaHouseUrlBuilder brokenUrlBuilder = new DunaHouseUrlBuilder() {
        @Override
        public String buildPageUrl(String baseSearchUrl, int page) {
          return baseSearchUrl;
        }
      };
      List<String> requestedUrls = new ArrayList<>();
      DunaHouseHtmlStrategy strategy = new DunaHouseHtmlStrategy(
          brokenUrlBuilder,
          new DunaHouseRawParser(),
          url -> {
            requestedUrls.add(url);
            return Jsoup.parse(dhPageHtml("LK000101", 45, 52), url);
          },
          5);

      RawExtractionResult result = strategy.extract(criteria);
      assertEquals(PortalScrapeStatus.SUCCESS, result.status());
      assertEquals(1, requestedUrls.size(), "Must not fetch the same page URL twice");
      assertTrue(result.technicalDetails().contains("stopReason=DUPLICATE_PAGE_URL"));

      KoltozzbeRawParser kbParser = new KoltozzbeRawParser();
      Document selfPointingDoc = Jsoup.parse(
          "<html><body>"
              + "<a rel='next' href='?page=1'>Következő</a>"
              + "<div class='pagination'><li class='next'><a href='#'>Next</a></li></div>"
              + "</body></html>");
      assertFalse(kbParser.hasNextPage(selfPointingDoc, 1),
          "rel=next pointing to current page (?page=1) or '#' must return false");
    }
  }

  @Nested
  @DisplayName("4. Oldalak közötti duplikált hirdetésazonosítók és URL-ek kiszűrése adatvesztés nélkül")
  class CrossPageDeduplicationTests {

    @Test
    @DisplayName("Portálon belüli oldalak közötti URL-variánsok (?ref=..., #fragment) és azonos nativeId-k kiszűrése")
    void crossPageDuplicateIdsAndTrackingUrlsAreDeduplicatedWithoutLosingDistinctListings() {
      OtthonCentrumPortalEngine ocEngine = new OtthonCentrumPortalEngine();
      PortalScrapeResult ocResult = ocEngine.scrape(criteria, url -> {
        if (url.endsWith("?page=2")) {
          String html = "<html><body>"
              + ocCardHtml("/ingatlanok/H500101?utm_source=list&ref=page2#top", "H500101", 44_000_000L, 50)
              + ocCardHtml("/ingatlanok/H500202", "H500202", 53_000_000L, 62)
              + "</body></html>";
          return Jsoup.parse(html, url);
        }
        String html = "<html><body>"
            + ocCardHtml("/ingatlanok/H500101?ref=page1", "H500101", 44_000_000L, 50)
            + ocCardHtml("/ingatlanok/H500102", "H500102", 48_000_000L, 55)
            + "</body></html>";
        return Jsoup.parse(html, url);
      }, 2);

      assertEquals(PortalScrapeStatus.SUCCESS, ocResult.getStatus());
      assertEquals(3, ocResult.getItems().size(),
          "H500101 appearing on both page 1 and page 2 with different query/fragment must be deduplicated");

      IngatlanBazarPortalEngine ibEngine = new IngatlanBazarPortalEngine();
      PortalScrapeResult ibResult = ibEngine.scrape(criteria, url -> {
        if (url.contains("&page=2")) {
          return ibJsonPage(
              "IB-SAME-ID", "/ingatlan/debrecen-masik-slug-101", 44_000_000L, 49,
              "IB-202", "/ingatlan/debrecen-202", 55_000_000L, 62);
        }
        return ibJsonPage(
            "IB-SAME-ID", "/ingatlan/debrecen-eredeti-slug-101", 44_000_000L, 49,
            "IB-102", "/ingatlan/debrecen-102", 48_000_000L, 54);
      }, 2);

      assertEquals(PortalScrapeStatus.SUCCESS, ibResult.getStatus());
      assertEquals(3, ibResult.getItems().size(),
          "Same nativeId (IB-SAME-ID) with different seoUrl across pages must be deduplicated");
    }

    @Test
    @DisplayName("DeduplicationEngine: többoldalas portál-találatok egyesítésekor a duplikáltak összevonásra kerülnek")
    void deduplicationEnginePreservesDistinctMultiPageListingsAndMergesDuplicates() {
      DeduplicationEngine deduplicationEngine = new DeduplicationEngine(null);

      IngatlanItem page1ItemA = TestFixtures.validResidentialItem(
          "LK000101", "Debrecen", 45_000_000L, 52.0, 2.0);
      page1ItemA.setTitle("Debrecen Nagyerdő lakás A");
      page1ItemA.setLocation("Debrecen, Nagyerdő");
      page1ItemA.setUrl("https://dh.hu/ingatlan/LK000101/debrecen-a");

      IngatlanItem page1ItemB = TestFixtures.validResidentialItem(
          "LK000102", "Debrecen", 62_000_000L, 74.0, 3.0);
      page1ItemB.setTitle("Debrecen Belváros lakás B");
      page1ItemB.setLocation("Debrecen, Belváros");
      page1ItemB.setUrl("https://dh.hu/ingatlan/LK000102/debrecen-b");

      IngatlanItem page2DuplicateOfA = TestFixtures.validResidentialItem(
          "LK000101", "Debrecen", 45_000_000L, 52.0, 2.0);
      page2DuplicateOfA.setTitle("Debrecen Nagyerdő lakás A");
      page2DuplicateOfA.setLocation("Debrecen, Nagyerdő");
      page2DuplicateOfA.setUrl("https://dh.hu/ingatlan/LK000101/debrecen-a");

      IngatlanItem page2ItemC = TestFixtures.validResidentialItem(
          "H500303", "Debrecen", 78_000_000L, 92.0, 4.0);
      page2ItemC.setSource(TestFixtures.validPortalSource("oc", "Otthon Centrum"));
      page2ItemC.setTitle("Debrecen Egyetemváros lakás C");
      page2ItemC.setLocation("Debrecen, Egyetemváros");
      page2ItemC.setUrl("https://www.oc.hu/ingatlanok/H500303");

      List<IngatlanItem> consolidated = deduplicationEngine.deduplicate(
          List.of(page1ItemA, page1ItemB, page2DuplicateOfA, page2ItemC));

      assertEquals(3, consolidated.size(), "3 distinct physical properties must remain after deduplication");
    }
  }

  @Nested
  @DisplayName("5. Köztes oldalhiba (timeout / 403 / 429 / kivétel) és teljes hiba megkülönböztetése")
  class IntermediatePageFailureVsTotalFailureTests {

    @Test
    @DisplayName("1. oldal sikeres, 2. oldal hiba: részleges eredmény megmarad, 1. oldal hiba esetén FAILED")
    void intermediatePageFailurePreservesPartialResultsAndDistinguishesFromTotalFailure() {
      DunaHousePortalEngine dhEngine = new DunaHousePortalEngine();

      PortalScrapeResult partialOnNull = dhEngine.scrape(criteria, url -> {
        if (url.endsWith("/oldal-2")) {
          return null;
        }
        return Jsoup.parse(dhPageHtml("LK000101", 45, 52, "LK000102", 49, 58), url);
      }, 3);

      assertEquals(PortalScrapeStatus.SUCCESS, partialOnNull.getStatus());
      assertEquals(2, partialOnNull.getItems().size(),
          "Page 1 listings must be preserved when page 2 returns null");
      assertTrue(partialOnNull.getTechnicalDetails().contains("pages=1"));
      assertTrue(partialOnNull.getTechnicalDetails().contains("stopReason=INTERMEDIATE_PAGE_FETCH_FAILED"));

      PortalScrapeResult partialOnException = dhEngine.scrape(criteria, url -> {
        if (url.endsWith("/oldal-2")) {
          throw new RuntimeException("HTTP 429 Too Many Requests / SocketTimeoutException");
        }
        return Jsoup.parse(dhPageHtml("LK000101", 45, 52), url);
      }, 3);

      assertEquals(PortalScrapeStatus.SUCCESS, partialOnException.getStatus());
      assertEquals(1, partialOnException.getItems().size(),
          "Page 1 listings must be preserved when page 2 throws an exception");
      assertTrue(partialOnException.getTechnicalDetails().contains("pages=1"));
      assertTrue(partialOnException.getTechnicalDetails().contains("stopReason=INTERMEDIATE_PAGE_EXCEPTION"));

      PortalScrapeResult totalFailureOnPageOneNull = dhEngine.scrape(criteria, url -> null, 3);
      assertEquals(PortalScrapeStatus.FAILED, totalFailureOnPageOneNull.getStatus(),
          "Page 1 null response must be FAILED, not EMPTY");

      JofogasPortalEngine jfEngine = new JofogasPortalEngine();
      PortalScrapeResult jfTotalFailure = jfEngine.scrape(criteria, url -> null, 2);
      assertEquals(PortalScrapeStatus.FAILED, jfTotalFailure.getStatus(),
          "Jófogás Page 1 null response must be FAILED, not EMPTY");

      KoltozzbeHtmlStrategy kbHtmlStrategy = new KoltozzbeHtmlStrategy(
          new KoltozzbeUrlBuilder(), new KoltozzbeRawParser(), url -> null, 2);
      assertEquals(PortalScrapeStatus.FAILED, kbHtmlStrategy.extract(criteria).status(),
          "Költözzbe HTML Page 1 null response must be FAILED, not EMPTY");

      IngatlanBazarApiStrategy ibStrategy = new IngatlanBazarApiStrategy(
          new IngatlanBazarQueryBuilder(),
          new IngatlanBazarRawParser(),
          url -> {
            if (url.contains("&page=2")) {
              throw new RuntimeException("HTTP 503 Service Unavailable");
            }
            return ibJsonPage("IB-101", "/ingatlan/debrecen-101", 44_000_000L, 49);
          },
          3);
      RawExtractionResult ibPartial = ibStrategy.extract(criteria);
      assertEquals(PortalScrapeStatus.SUCCESS, ibPartial.status());
      assertEquals(1, ibPartial.listings().size());
      assertTrue(ibPartial.technicalDetails().contains("stopReason=INTERMEDIATE_PAGE_EXCEPTION"));
    }
  }

  @Nested
  @DisplayName("6. Strukturált naplózás ellenőrzése (oldalszám + leállási ok, nyers payload nélkül)")
  class StructuredLoggingVerificationTests {

    @Test
    @DisplayName("A lapozás végén a napló tartalmazza a feldolgozott oldalak számát és a leállási okot")
    void logsProcessedPageCountAndStopReasonWithoutRawPayload() {
      Logger dhLogger = (Logger) LoggerFactory.getLogger(
          DunaHouseHtmlStrategy.class.getSimpleName());
      Level previousSlf4jLevel = dhLogger.getLevel();
      AppLogger.LogLevel previousAppLevel = AppLogger.getGlobalLevel();
      dhLogger.setLevel(Level.INFO);
      AppLogger.setGlobalLevel(AppLogger.LogLevel.INFO);
      ListAppender<ILoggingEvent> appender = new ListAppender<>();
      appender.start();
      dhLogger.addAppender(appender);

      String secretRawHtmlMarker = "SECRET_RAW_HTML_PAYLOAD_DO_NOT_LOG_987654321";
      try {
        DunaHouseHtmlStrategy strategy = new DunaHouseHtmlStrategy(
            new DunaHouseUrlBuilder(),
            new DunaHouseRawParser(),
            url -> {
              if (url.endsWith("/oldal-2")) {
                return Jsoup.parse("<html><body><!-- " + secretRawHtmlMarker + " --></body></html>", url);
              }
              return Jsoup.parse(
                  "<html><body><!-- " + secretRawHtmlMarker + " -->"
                      + dhCardHtml("LK000101", 45, 52)
                      + "</body></html>",
                  url);
            },
            3);

        RawExtractionResult result = strategy.extract(criteria);
        assertEquals(PortalScrapeStatus.SUCCESS, result.status());

        List<String> messages = appender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .toList();

        assertTrue(messages.stream().anyMatch(
            msg -> msg.contains("feldolgozott oldalak=2") && msg.contains("leállás oka=EMPTY_PAGE")),
            "Log must record processed page count (2) and stop reason (EMPTY_PAGE)");
        assertTrue(messages.stream().noneMatch(msg -> msg.contains(secretRawHtmlMarker)),
            "Log must never dump raw HTML payload");
      } finally {
        dhLogger.detachAppender(appender);
        dhLogger.setLevel(previousSlf4jLevel);
        AppLogger.setGlobalLevel(previousAppLevel);
      }
    }
  }

  private static String dhPageHtml(String id1, int priceM1, int area1) {
    return "<html><body>" + dhCardHtml(id1, priceM1, area1) + "</body></html>";
  }

  private static String dhPageHtml(
      String id1, int priceM1, int area1, String id2, int priceM2, int area2) {
    return "<html><body>"
        + dhCardHtml(id1, priceM1, area1)
        + dhCardHtml(id2, priceM2, area2)
        + "</body></html>";
  }

  private static String dhCardHtml(String id, int priceMillion, int area) {
    return "<a href='/ingatlan/" + id + "/elado-lakas-debrecen'>"
        + "Eladó lakás | 4024 Debrecen, Piac utca | " + priceMillion + " M Ft | " + area + " m2 | 2 szoba"
        + "</a>";
  }

  private static String ocPageHtml(String id1, long priceHuf1, int area1) {
    return "<html><body>" + ocCardHtml("/ingatlanok/" + id1, id1, priceHuf1, area1) + "</body></html>";
  }

  private static String ocPageHtml(
      String id1, long priceHuf1, int area1, String id2, long priceHuf2, int area2) {
    return "<html><body>"
        + ocCardHtml("/ingatlanok/" + id1, id1, priceHuf1, area1)
        + ocCardHtml("/ingatlanok/" + id2, id2, priceHuf2, area2)
        + "</body></html>";
  }

  private static String ocCardHtml(String href, String id, long priceHuf, int area) {
    String seoJson = "{\"ecommerce\":{\"items\":[{\"location_city\":\"Debrecen\",\"price\":" + priceHuf + "}]}}";
    return "<div class='js-list-item' data-controller='seo' data-seo-select-item-value='" + seoJson + "'>"
        + "<a href='" + href + "'>"
        + "<h6>Debrecen lakás " + id + "</h6>"
        + "<span>" + (priceHuf / 1_000_000L) + " M Ft " + area + " m2 2 szoba</span>"
        + "</a></div>";
  }

  private static String jofogasPageHtml(String id1, int priceM1, int area1) {
    return "<html><body>" + jofogasCardHtml(id1, priceM1, area1) + "</body></html>";
  }

  private static String jofogasPageHtml(
      String id1, int priceM1, int area1, String id2, int priceM2, int area2) {
    return "<html><body>"
        + jofogasCardHtml(id1, priceM1, area1)
        + jofogasCardHtml(id2, priceM2, area2)
        + "</body></html>";
  }

  private static String jofogasCardHtml(String id, int priceMillion, int area) {
    return "<a href='https://ingatlan.jofogas.hu/hajdu-bihar/debrecen/Elado_lakas_" + id + ".htm'>"
        + "<h3>Debrecen tégla lakás " + id + "</h3>"
        + "<span>" + priceMillion + " M Ft " + area + " m2 2 szoba Debrecen</span>"
        + "</a>";
  }

  private static String koltozzbePageHtml(String id, int priceMillion, int area, Integer nextPage) {
    String pagination = nextPage != null
        ? "<div class='pagination'><a rel='next' href='?page=" + nextPage + "'>Következő</a></div>"
        : "";
    return "<html><body>"
        + "<div class='listing-card'>"
        + "<a class='listing-item-link' href='/elado-lakas-debrecen/h" + id + "' title='Debrecen lakás " + id + "'>"
        + "<span class='item-price'>" + priceMillion + " M Ft</span>"
        + "<span class='listing-parameters'>" + area + " m2 2 szoba Debrecen</span>"
        + "</a></div>"
        + pagination
        + "</body></html>";
  }

  private static String koltozzbeJsonLdPageHtml(String id, long priceHuf, Integer nextPage) {
    String pagination = nextPage != null
        ? "<div class='pagination'><a href='?page=" + nextPage + "'>" + nextPage + "</a></div>"
        : "";
    String jsonLd = "{"
        + "\"@context\":\"https://schema.org\","
        + "\"@type\":\"ItemList\","
        + "\"itemListElement\":[{"
        + "\"@type\":\"ListItem\","
        + "\"item\":{"
        + "\"url\":\"https://koltozzbe.hu/elado-lakas-debrecen/h" + id + "\","
        + "\"name\":\"Debrecen lakás " + id + " 55 m2 2 szoba\","
        + "\"description\":\"Debrecen 55 m2 2 szoba\","
        + "\"offers\":{\"price\":" + priceHuf + "}"
        + "}}]}";
    return "<html><head><script type='application/ld+json'>" + jsonLd + "</script></head><body>"
        + pagination
        + "</body></html>";
  }

  private static String ibJsonPage(String id1, String seoUrl1, long price1, int area1) {
    return "{\"hits\":[" + ibHitJson(id1, seoUrl1, price1, area1) + "]}";
  }

  private static String ibJsonPage(
      String id1, String seoUrl1, long price1, int area1,
      String id2, String seoUrl2, long price2, int area2) {
    return "{\"hits\":["
        + ibHitJson(id1, seoUrl1, price1, area1) + ","
        + ibHitJson(id2, seoUrl2, price2, area2)
        + "]}";
  }

  private static String ibHitJson(String id, String seoUrl, long price, int area) {
    return "{"
        + "\"propertyId\":\"" + id + "\","
        + "\"seoUrl\":\"" + seoUrl + "\","
        + "\"title\":\"Debrecen eladó lakás " + id + "\","
        + "\"priceHUF\":" + price + ","
        + "\"area\":" + area + ","
        + "\"rooms\":2,"
        + "\"settlement\":\"Debrecen\""
        + "}";
  }
}
