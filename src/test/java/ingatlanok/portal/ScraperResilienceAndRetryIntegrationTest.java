package ingatlanok.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.sun.net.httpserver.HttpServer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.common.HttpFetcher;
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
import ingatlanok.model.SearchExecutionStatus;
import ingatlanok.model.SearchResult;
import ingatlanok.model.raw.RawExtractionResult;
import ingatlanok.persistence.InMemoryListingStorage;
import ingatlanok.portal.dh.DunaHouseHtmlStrategy;
import ingatlanok.portal.dh.DunaHouseRawParser;
import ingatlanok.portal.dh.DunaHouseUrlBuilder;
import ingatlanok.portal.ingatlanbazar.IngatlanBazarApiStrategy;
import ingatlanok.portal.ingatlanbazar.IngatlanBazarQueryBuilder;
import ingatlanok.portal.ingatlanbazar.IngatlanBazarRawParser;
import ingatlanok.portal.ingatlancom.IngatlanComHtmlStrategy;
import ingatlanok.portal.ingatlancom.IngatlanComRawParser;
import ingatlanok.portal.ingatlancom.IngatlanComUrlBuilder;
import ingatlanok.portal.jofogas.JofogasHtmlStrategy;
import ingatlanok.portal.jofogas.JofogasRawParser;
import ingatlanok.portal.jofogas.JofogasUrlBuilder;
import ingatlanok.portal.koltozzbe.KoltozzbeHtmlStrategy;
import ingatlanok.portal.koltozzbe.KoltozzbeJsonLdStrategy;
import ingatlanok.portal.koltozzbe.KoltozzbeRawParser;
import ingatlanok.portal.koltozzbe.KoltozzbeUrlBuilder;

class ScraperResilienceAndRetryIntegrationTest {

  private static HttpServer testServer;
  private static int testPort;

  private static final AtomicInteger ATTEMPTS_403 = new AtomicInteger();
  private static final AtomicInteger ATTEMPTS_429_RECOVER = new AtomicInteger();
  private static final AtomicInteger ATTEMPTS_429_FAIL = new AtomicInteger();
  private static final AtomicInteger ATTEMPTS_503_RECOVER = new AtomicInteger();
  private static final AtomicInteger ATTEMPTS_500_FAIL = new AtomicInteger();
  private static final AtomicInteger ATTEMPTS_502_FAIL = new AtomicInteger();
  private static final AtomicInteger ATTEMPTS_504_FAIL = new AtomicInteger();
  private static final AtomicInteger ATTEMPTS_TIMEOUT = new AtomicInteger();
  private static final AtomicInteger ATTEMPTS_REDIRECT_A = new AtomicInteger();
  private static final AtomicInteger ATTEMPTS_REDIRECT_B = new AtomicInteger();

  @BeforeAll
  static void startServer() throws IOException {
    System.setProperty("app.test-mode", "true");
    testServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    testServer.setExecutor(Executors.newCachedThreadPool());
    testPort = testServer.getAddress().getPort();

    testServer.createContext("/status-403", exchange -> {
      ATTEMPTS_403.incrementAndGet();
      byte[] resp = "<html><head><title>403 Forbidden</title></head></html>".getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(403, resp.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(resp);
      }
    });

    testServer.createContext("/status-429-recover", exchange -> {
      int count = ATTEMPTS_429_RECOVER.incrementAndGet();
      if (count == 1) {
        exchange.getResponseHeaders().set("Retry-After", "1");
        exchange.sendResponseHeaders(429, -1);
        exchange.close();
      } else {
        byte[] resp = "{\"status\":\"recovered\"}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, resp.length);
        try (OutputStream os = exchange.getResponseBody()) {
          os.write(resp);
        }
      }
    });

    testServer.createContext("/status-429-fail", exchange -> {
      ATTEMPTS_429_FAIL.incrementAndGet();
      exchange.getResponseHeaders().set("Retry-After", "2");
      exchange.sendResponseHeaders(429, -1);
      exchange.close();
    });

    testServer.createContext("/status-503-recover", exchange -> {
      int count = ATTEMPTS_503_RECOVER.incrementAndGet();
      if (count == 1) {
        exchange.sendResponseHeaders(503, -1);
        exchange.close();
      } else {
        byte[] resp = "<html><head><title>Recovered Page</title></head><body><h1>OK</h1></body></html>"
            .getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, resp.length);
        try (OutputStream os = exchange.getResponseBody()) {
          os.write(resp);
        }
      }
    });

    testServer.createContext("/status-500-fail", exchange -> {
      ATTEMPTS_500_FAIL.incrementAndGet();
      exchange.sendResponseHeaders(500, -1);
      exchange.close();
    });

    testServer.createContext("/status-502-fail", exchange -> {
      ATTEMPTS_502_FAIL.incrementAndGet();
      exchange.sendResponseHeaders(502, -1);
      exchange.close();
    });

    testServer.createContext("/status-504-fail", exchange -> {
      ATTEMPTS_504_FAIL.incrementAndGet();
      exchange.sendResponseHeaders(504, -1);
      exchange.close();
    });

    testServer.createContext("/slow-timeout", exchange -> {
      ATTEMPTS_TIMEOUT.incrementAndGet();
      try {
        Thread.sleep(600);
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
      }
      exchange.sendResponseHeaders(200, -1);
      exchange.close();
    });

    testServer.createContext("/loop-a", exchange -> {
      ATTEMPTS_REDIRECT_A.incrementAndGet();
      exchange.getResponseHeaders().set("Location", "/loop-b");
      exchange.sendResponseHeaders(302, -1);
      exchange.close();
    });

    testServer.createContext("/loop-b", exchange -> {
      ATTEMPTS_REDIRECT_B.incrementAndGet();
      exchange.getResponseHeaders().set("Location", "/loop-a");
      exchange.sendResponseHeaders(302, -1);
      exchange.close();
    });

    testServer.createContext("/empty-body", exchange -> {
      exchange.sendResponseHeaders(200, 0);
      exchange.close();
    });

    testServer.createContext("/malformed-json", exchange -> {
      byte[] resp = "{ not valid json: 123 ".getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, resp.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(resp);
      }
    });

    testServer.start();
  }

  @AfterAll
  static void stopServer() {
    System.clearProperty("app.test-mode");
    if (testServer != null) {
      testServer.stop(0);
    }
  }

  @BeforeEach
  void resetCounters() {
    ATTEMPTS_403.set(0);
    ATTEMPTS_429_RECOVER.set(0);
    ATTEMPTS_429_FAIL.set(0);
    ATTEMPTS_503_RECOVER.set(0);
    ATTEMPTS_500_FAIL.set(0);
    ATTEMPTS_502_FAIL.set(0);
    ATTEMPTS_504_FAIL.set(0);
    ATTEMPTS_TIMEOUT.set(0);
    ATTEMPTS_REDIRECT_A.set(0);
    ATTEMPTS_REDIRECT_B.set(0);
    HttpFetcher.setSleeperForTesting(null);
    HttpFetcher.resetResilienceStateForTests();
  }

  private String getBaseUrl() {
    return "http://127.0.0.1:" + testPort;
  }

  @Test
  @DisplayName("HTTP 403 Forbidden: Nem indul kontrollálatlan retry ciklus, pontosan 1 hívás történik")
  void testHttp403ForbiddenNoRetry() {
    Document doc = HttpFetcher.fetchDocument(getBaseUrl() + "/status-403");
    assertNull(doc);
    assertEquals(1, ATTEMPTS_403.get(), "403 esetén nem szabad újrapróbálkozni, pontosan 1 kísérlet kell");
  }

  @Test
  @DisplayName("HTTP 429 Retry-After: Sikeressé váló kérés retry-val lefut és visszatér az eredménnyel")
  void testHttp429TooManyRequestsRecover() {
    String json = HttpFetcher.fetchJson(getBaseUrl() + "/status-429-recover");
    assertNotNull(json);
    assertTrue(json.contains("recovered"));
    assertEquals(2, ATTEMPTS_429_RECOVER.get(), "Az első 429 után a 2. kísérletnek sikeresnek kell lennie");
  }

  @Test
  @DisplayName("HTTP 429 Tartós hiba: A retry darabszáma korlátos, kimerülés után null-lal tér vissza")
  void testHttp429TooManyRequestsExhausted() {
    String json = HttpFetcher.fetchJson(getBaseUrl() + "/status-429-fail");
    assertNull(json);
    assertEquals(3, ATTEMPTS_429_FAIL.get(), "A retry-k száma korlátos kell legyen (1 kezdő + 2 retry)");
  }

  @Test
  @DisplayName("HTTP 503 Átmeneti szerverhiba: 2. próbálkozásra helyreáll és visszatér a Documentummal")
  void testHttp503TransientFailureRecovers() {
    Document doc = HttpFetcher.fetchDocument(getBaseUrl() + "/status-503-recover");
    assertNotNull(doc);
    assertEquals("Recovered Page", doc.title());
    assertEquals(2, ATTEMPTS_503_RECOVER.get(), "503 után újrapróbálkozott és 2. kísérletre sikeres lett");
  }

  @Test
  @DisplayName("HTTP 500 Tartós szerverhiba: Korlátos retry után leáll és null-lal tér vissza")
  void testHttp500PersistentFailureStopsAtMaxRetries() {
    Document doc = HttpFetcher.fetchDocument(getBaseUrl() + "/status-500-fail");
    assertNull(doc);
    assertEquals(3, ATTEMPTS_500_FAIL.get(), "A retry-k száma korlátos kell legyen 500 esetén (3 próbálkozás)");
  }

  @Test
  @DisplayName("HTTP 502 és 504 Hibák: Mindkettő átmeneti hibaként retry-t kap és korlátos")
  void testHttp502And504GatewayErrors() {
    assertNull(HttpFetcher.fetchDocument(getBaseUrl() + "/status-502-fail"));
    assertEquals(3, ATTEMPTS_502_FAIL.get());

    assertNull(HttpFetcher.fetchDocument(getBaseUrl() + "/status-504-fail"));
    assertEquals(3, ATTEMPTS_504_FAIL.get());
  }

  @Test
  @DisplayName("SocketTimeout: Időtúllépés esetén korlátos retry aktiválódik")
  void testConnectOrReadTimeoutRetriesBounded() {
    Document doc = HttpFetcher.fetchDocument(getBaseUrl() + "/slow-timeout", 200);
    assertNull(doc);
    assertTrue(ATTEMPTS_TIMEOUT.get() >= 2 && ATTEMPTS_TIMEOUT.get() <= 3,
        "Timeout esetén korlátos retry történik");
  }

  @Test
  @DisplayName("Redirect hurok: Váratlan redirect loop esetén nem indul végtelen retry")
  void testRedirectLoopFailsWithoutRetrying() {
    Document doc = HttpFetcher.fetchDocument(getBaseUrl() + "/loop-a");
    assertNull(doc);
  }

  @Test
  @DisplayName("Üres és hibás JSON választest: Nem dob kezeletlen kivételt, kecsesen kezeli")
  void testEmptyAndMalformedResponsesHandledCleanly() {
    String emptyJson = HttpFetcher.fetchJson(getBaseUrl() + "/empty-body");
    assertNotNull(emptyJson);
    assertTrue(emptyJson.isEmpty());

    String malformed = HttpFetcher.fetchJson(getBaseUrl() + "/malformed-json");
    assertNotNull(malformed);
    assertTrue(malformed.contains("not valid json"));

    IngatlanBazarRawParser parser = new IngatlanBazarRawParser();
    assertTrue(parser.parseRawListings(emptyJson).isEmpty());
    assertTrue(parser.parseRawListings(malformed).isEmpty());
  }

  @Test
  @DisplayName("Időzítéses teszt: Kontrollált Sleeper rögzíti a Retry-After késleltetést valós alvás nélkül")
  void testControlledSleeperCapturesRetryAfterDelay() {
    AtomicLong recordedDelay = new AtomicLong();
    HttpFetcher.setSleeperForTesting(recordedDelay::set);
    try {
      String json = HttpFetcher.fetchJson(getBaseUrl() + "/status-429-recover");
      assertNotNull(json);
      assertEquals(1000L, recordedDelay.get(), "A Retry-After: 1 fejlécből pontosan 1000ms alvást kell kérnie");
    } finally {
      HttpFetcher.setSleeperForTesting(null);
    }
  }

  @Test
  @DisplayName("CAPTCHA / Cloudflare challenge: Nem indul retry, a stratégia BOT_PROTECTED státusszal zár")
  void testCaptchaChallengeProducesBotProtectedWithoutRetry() {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");

    Document challengeDoc = Jsoup.parse(
        "<html><head><title>Just a moment...</title></head>"
            + "<body><div id=\"challenge-running\">Cloudflare Turnstile</div></body></html>");

    IngatlanComHtmlStrategy strategy = new IngatlanComHtmlStrategy(
        new IngatlanComUrlBuilder(), new IngatlanComRawParser(), url -> challengeDoc);

    RawExtractionResult result = strategy.extract(criteria);
    assertEquals(PortalScrapeStatus.BOT_PROTECTED, result.status());
    assertTrue(result.message().contains("Cloudflare WAF"));
  }

  @Test
  @DisplayName("Portál stratégiák: Letöltési hiba esetén FAILED státuszt adnak vissza, nem EMPTY-t")
  void testStrategiesReturnFailedOnDownloadError() {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");

    DunaHouseHtmlStrategy dhStrategy = new DunaHouseHtmlStrategy(
        new DunaHouseUrlBuilder(), new DunaHouseRawParser(), url -> null);
    RawExtractionResult dhResult = dhStrategy.extract(criteria);
    assertEquals(PortalScrapeStatus.FAILED, dhResult.status(),
        "Duna House-nak letöltési hiba esetén FAILED státuszt kell adnia");

    JofogasHtmlStrategy jfStrategy = new JofogasHtmlStrategy(
        new JofogasUrlBuilder(), new JofogasRawParser(), url -> null);
    RawExtractionResult jfResult = jfStrategy.extract(criteria);
    assertEquals(PortalScrapeStatus.FAILED, jfResult.status(),
        "Jófogásnak letöltési hiba esetén FAILED státuszt kell adnia");

    KoltozzbeHtmlStrategy kbHtmlStrategy = new KoltozzbeHtmlStrategy(
        new KoltozzbeUrlBuilder(), new KoltozzbeRawParser(), url -> null);
    RawExtractionResult kbHtmlResult = kbHtmlStrategy.extract(criteria);
    assertEquals(PortalScrapeStatus.FAILED, kbHtmlResult.status(),
        "Költözzbe HTML-nek letöltési hiba esetén FAILED státuszt kell adnia");

    KoltozzbeJsonLdStrategy kbJsonStrategy = new KoltozzbeJsonLdStrategy(
        new KoltozzbeUrlBuilder(), new KoltozzbeRawParser(), url -> null);
    RawExtractionResult kbJsonResult = kbJsonStrategy.extract(criteria);
    assertEquals(PortalScrapeStatus.FAILED, kbJsonResult.status(),
        "Költözzbe JSON-LD-nek letöltési hiba esetén FAILED státuszt kell adnia");

    IngatlanBazarApiStrategy ibStrategy = new IngatlanBazarApiStrategy(
        new IngatlanBazarQueryBuilder(), new IngatlanBazarRawParser(), url -> null);
    RawExtractionResult ibResult = ibStrategy.extract(criteria);
    assertEquals(PortalScrapeStatus.FAILED, ibResult.status(),
        "Ingatlanbazárnak letöltési hiba esetén FAILED státuszt kell adnia");
  }

  @Test
  @DisplayName(
      "Részleges siker: Egy portál hibája nem törli a másik portál érvényes találatait, státusz PARTIAL_SUCCESS")
  void testPartialSuccessRetainsValidItems() {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCities(List.of("Debrecen"));
    criteria.setPortals(List.of("good", "fail"));
    criteria.setMinPriceMillion(40);
    criteria.setMaxPriceMillion(70);
    criteria.setMinArea(null);
    criteria.setMaxArea(null);

    PortalEngine goodEngine = new PortalEngine() {
      @Override
      public PortalDescriptor getDescriptor() {
        return new PortalDescriptor(
            "good", "Good Portal", "#123456", "https://good.hu", true, List.of("lakas"), true);
      }

      @Override
      public PortalScrapeResult scrape(SearchCriteria searchCriteria) {
        IngatlanItem item = new IngatlanItem(
            "1",
            PortalSource.of("good"),
            "https://good.hu/1",
            "Szép lakás",
            new PriceInfo("50 M Ft", 50_000_000L, 50.0),
            searchCriteria.getCity(),
            new PropertyDetails("60 m²", 60.0, 2.0, 833_333L),
            "2026-10-14",
            true);
        return PortalScrapeResult.success("good", "Good Portal", List.of(item), 100);
      }
    };

    PortalEngine failingEngine = new PortalEngine() {
      @Override
      public PortalDescriptor getDescriptor() {
        return new PortalDescriptor(
            "fail", "Failing Portal", "#654321", "https://fail.hu", true, List.of("lakas"), true);
      }

      @Override
      public PortalScrapeResult scrape(SearchCriteria searchCriteria) {
        return PortalScrapeResult.failure("fail", "Failing Portal", "HTTP 500 hiba", 50, 500, "https://fail.hu");
      }
    };

    InMemoryListingStorage storage = new InMemoryListingStorage();
    MarketAnalyticsEngine analytics = new MarketAnalyticsEngine();
    SearchExecutionEngine search = new SearchExecutionEngine(
        new PortalEngineRegistry(List.of(goodEngine, failingEngine)),
        storage, storage, new PropertyFilterEngine(), new DeduplicationEngine(storage), analytics);

    SearchResult result = search.executeSearch(criteria);
    assertNotNull(result);
    assertEquals(1, result.getItems().size(), "A sikeres portál találatának meg kell maradnia");
    assertEquals("https://good.hu/1", result.getItems().get(0).getUrl());
    assertEquals(SearchExecutionStatus.PARTIAL_SUCCESS, result.getExecutionStatus(),
        "Részleges siker státusznak kell lennie, ha legalább egy portál sikeres és egy hibás");

    boolean hasFailedPortal = result.getPortalResults().stream()
        .anyMatch(p -> "fail".equals(p.getPortalId()) && p.getStatus() == PortalScrapeStatus.FAILED);
    assertTrue(hasFailedPortal, "A hibás portál FAILED státusszal kell szerepeljen");
  }

  @Test
  @DisplayName("Teljes sikertelenség: Minden portál hibája esetén a státusz TOTAL_FAILURE és 0 találat")
  void testTotalFailureWhenAllPortalsFail() {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCities(List.of("Debrecen"));
    criteria.setPortals(List.of("dh", "oc"));

    PortalEngine engineA = new PortalEngine() {
      @Override
      public PortalDescriptor getDescriptor() {
        return new PortalDescriptor("dh", "Duna House", "#008638", "https://dh.hu", true, List.of("lakas"), true);
      }

      @Override
      public PortalScrapeResult scrape(SearchCriteria searchCriteria) {
        return PortalScrapeResult.failure("dh", "Duna House", "Connection timeout", 50, 504, "https://dh.hu");
      }
    };

    PortalEngine engineB = new PortalEngine() {
      @Override
      public PortalDescriptor getDescriptor() {
        return new PortalDescriptor("oc", "Otthon Centrum", "#ea580c", "https://oc.hu", true, List.of("lakas"), true);
      }

      @Override
      public PortalScrapeResult scrape(SearchCriteria searchCriteria) {
        return PortalScrapeResult.botProtected("oc", "Otthon Centrum", "https://oc.hu", 30);
      }
    };

    InMemoryListingStorage storage = new InMemoryListingStorage();
    MarketAnalyticsEngine analytics = new MarketAnalyticsEngine();
    SearchExecutionEngine search = new SearchExecutionEngine(
        new PortalEngineRegistry(List.of(engineA, engineB)),
        storage, storage, new PropertyFilterEngine(), new DeduplicationEngine(storage), analytics);

    SearchResult result = search.executeSearch(criteria);
    assertNotNull(result);
    assertEquals(0, result.getItems().size());
    assertEquals(SearchExecutionStatus.TOTAL_FAILURE, result.getExecutionStatus(),
        "Minden portál hibája esetén TOTAL_FAILURE státuszt kell kapni");
  }

  @Test
  @DisplayName("Teljes siker: Minden portál sikeres lefutása esetén SUCCESS státusz")
  void testFullSuccessWhenAllPortalsSucceed() {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCities(List.of("Debrecen"));
    criteria.setPortals(List.of("dh"));

    PortalEngine engineA = new PortalEngine() {
      @Override
      public PortalDescriptor getDescriptor() {
        return new PortalDescriptor("dh", "Duna House", "#008638", "https://dh.hu", true, List.of("lakas"), true);
      }

      @Override
      public PortalScrapeResult scrape(SearchCriteria searchCriteria) {
        return PortalScrapeResult.success("dh", "Duna House", Collections.emptyList(), 20);
      }
    };

    InMemoryListingStorage storage = new InMemoryListingStorage();
    MarketAnalyticsEngine analytics = new MarketAnalyticsEngine();
    SearchExecutionEngine search = new SearchExecutionEngine(
        new PortalEngineRegistry(List.of(engineA)),
        storage, storage, new PropertyFilterEngine(), new DeduplicationEngine(storage), analytics);

    SearchResult result = search.executeSearch(criteria);
    assertNotNull(result);
    assertEquals(SearchExecutionStatus.SUCCESS, result.getExecutionStatus(),
        "Minden portál sikere esetén SUCCESS státusznak kell lennie");
  }
}
