package ingatlanok;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.env.Environment;

import ingatlanok.common.JsonSupport;
import ingatlanok.core.SearchExecutionEngine;
import ingatlanok.core.resilience.PortalCircuitBreaker;
import ingatlanok.core.resilience.PortalRateLimiter;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.PortalScrapeStatus;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;
import ingatlanok.persistence.ConfigStorage;
import ingatlanok.persistence.OffsetPageRequest;

class ArchitectureConsolidationTest {

  @Test
  void jacksonSupportParsesPortalJson() {
    assertEquals(42, JsonSupport.parse("{\"value\":42}").get("value").asInt());
  }

  @Test
  void applicationReadyUsesLoopbackDisplayAddress() {
    Environment environment = mock(Environment.class);
    when(environment.getProperty("server.port", "8080")).thenReturn("9090");
    when(environment.getProperty("server.address", "127.0.0.1")).thenReturn("0.0.0.0");
    System.setProperty("open.browser", "false");
    try {
      new IngatlanApplication(environment).onApplicationReady();
    } finally {
      System.clearProperty("open.browser");
    }
  }

  @Test
  void cliRunnerExecutesSearchWithSavedCriteria() throws Exception {
    SearchExecutionEngine engine = mock(SearchExecutionEngine.class);
    ConfigStorage configStorage = mock(ConfigStorage.class);
    SearchCriteria criteria = new SearchCriteria();
    SearchResult result = new SearchResult();
    result.setTotalFound(1);
    result.setNewCount(1);
    when(configStorage.getLastCriteria()).thenReturn(criteria);
    when(engine.executeSearch(criteria)).thenReturn(result);

    new CliSearchRunner(engine, configStorage).run(new DefaultApplicationArguments("--cli"));

    verify(engine).executeSearch(criteria);
  }

  @Test
  void circuitBreakerOpensAfterThreeFailuresAndClosesOnSuccess() {
    PortalCircuitBreaker breaker = new PortalCircuitBreaker();
    String portal = "breaker-test";

    assertTrue(breaker.allowRequest(portal));
    breaker.recordFailure(portal);
    breaker.recordFailure(portal);
    breaker.recordFailure(portal);

    assertFalse(breaker.allowRequest(portal));
    breaker.recordSuccess(portal);
    assertTrue(breaker.allowRequest(portal));
  }

  @Test
  void circuitBreakerAllowsOnlyOneHalfOpenProbe() throws ReflectiveOperationException {
    PortalCircuitBreaker breaker = new PortalCircuitBreaker();
    String portal = "half-open-test";
    breaker.recordFailure(portal);
    breaker.recordFailure(portal);
    breaker.recordFailure(portal);

    Field statesField = PortalCircuitBreaker.class.getDeclaredField("states");
    statesField.setAccessible(true);
    Map<?, ?> states = (Map<?, ?>) statesField.get(breaker);
    Object state = states.get(portal);
    Field openedAtField = state.getClass().getDeclaredField("openedAt");
    openedAtField.setAccessible(true);
    openedAtField.setLong(state, System.currentTimeMillis() - 61_000);

    assertTrue(breaker.allowRequest(portal));
    assertFalse(breaker.allowRequest(portal));
  }

  @Test
  void rateLimiterAcceptsConfiguredAndInvalidRates() throws InterruptedException {
    PortalRateLimiter limiter = new PortalRateLimiter();
    System.setProperty("scraper.rate.test.portal", "20");
    System.setProperty("scraper.rate.invalid.portal", "not-a-number");
    System.setProperty("scraper.rate.wait.portal", "1");
    try {
      limiter.acquire("test.portal");
      limiter.acquire("invalid.portal");
      limiter.acquire("wait.portal");
      limiter.acquire("wait.portal");
      limiter.acquire("jofogas.hu");
    } finally {
      System.clearProperty("scraper.rate.test.portal");
      System.clearProperty("scraper.rate.invalid.portal");
      System.clearProperty("scraper.rate.wait.portal");
    }
  }

  @Test
  void offsetPageRequestPreservesOffsetAndSupportsNavigation() {
    OffsetPageRequest request = new OffsetPageRequest(7, 5, null);

    assertEquals(1, request.getPageNumber());
    assertEquals(7, request.getOffset());
    assertTrue(request.hasPrevious());
    assertEquals(12, request.next().getOffset());
    assertEquals(2, request.previousOrFirst().getOffset());
    assertEquals(0, request.first().getOffset());
    assertEquals(15, request.withPage(3).getOffset());
    assertFalse(new OffsetPageRequest(0, 5, null).hasPrevious());
    assertThrows(IllegalArgumentException.class, () -> new OffsetPageRequest(-1, 5, null));
    assertThrows(IllegalArgumentException.class, () -> new OffsetPageRequest(0, 0, null));
    assertThrows(IllegalArgumentException.class, () -> request.withPage(-1));
  }

  @Test
  void portalScrapeResultFactoriesAndAccessorsPreserveScrapeDetails() {
    IngatlanItem item = new IngatlanItem();
    PortalScrapeResult success = PortalScrapeResult.success("demo", "Demo", List.of(item), 12);
    assertEquals(PortalScrapeStatus.SUCCESS, success.getStatus());
    assertEquals(1, success.getItems().size());
    assertEquals(200, success.getHttpStatusCode());

    PortalScrapeResult empty = PortalScrapeResult.success("demo", "Demo", List.of(), 13);
    assertEquals(PortalScrapeStatus.EMPTY, empty.getStatus());
    assertEquals(PortalScrapeStatus.EMPTY, PortalScrapeResult.empty("demo", "Demo", "/", 14).getStatus());
    assertEquals(
        PortalScrapeStatus.BOT_PROTECTED,
        PortalScrapeResult.botProtected("demo", "Demo", "/", 15).getStatus());
    assertEquals(
        PortalScrapeStatus.SPA_FALLBACK,
        PortalScrapeResult.spaFallback("demo", "Demo", "/", 16).getStatus());
    assertEquals(
        500, PortalScrapeResult.failure("demo", "Demo", "error", 17).getHttpStatusCode());
    assertEquals(
        429,
        PortalScrapeResult.failure("demo", "Demo", "error", 18, 429, "/").getHttpStatusCode());

    success.setPortalId("other");
    success.setPortalName("Other");
    success.setStatus(PortalScrapeStatus.FAILED);
    success.setItems(null);
    success.setFallbackUrl("/fallback");
    success.setMessage("message");
    success.setExecutionTimeMs(42);
    success.setHttpStatusCode(503);
    success.setTechnicalDetails("details");
    assertEquals("other", success.getPortalId());
    assertEquals("Other", success.getPortalName());
    assertNull(success.getItems());
    assertEquals("/fallback", success.getFallbackUrl());
    assertEquals("message", success.getMessage());
    assertEquals(42, success.getExecutionTimeMs());
    assertEquals(503, success.getHttpStatusCode());
    assertEquals("details", success.getTechnicalDetails());
  }
}
