package ingatlanok;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ingatlanok.core.resilience.PortalCircuitBreaker;
import ingatlanok.core.resilience.PortalRateLimiter;
import ingatlanok.persistence.OffsetPageRequest;

class ArchitectureConsolidationTest {

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
  void rateLimiterAcceptsConfiguredAndInvalidRates() throws InterruptedException {
    PortalRateLimiter limiter = new PortalRateLimiter();
    System.setProperty("scraper.rate.test.portal", "20");
    System.setProperty("scraper.rate.invalid.portal", "not-a-number");
    try {
      limiter.acquire("test.portal");
      limiter.acquire("invalid.portal");
    } finally {
      System.clearProperty("scraper.rate.test.portal");
      System.clearProperty("scraper.rate.invalid.portal");
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
}
