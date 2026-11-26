package ingatlanok.common;

import java.io.IOException;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import ingatlanok.common.resilience.PortalCircuitBreaker;
import ingatlanok.common.resilience.PortalRateLimiter;

/** Centralized resilient HTTP fetcher with domain whitelisting, rate limiting, and circuit breaking. */
@Slf4j
public final class HttpFetcher {

  public static final String DEFAULT_USER_AGENT =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)"
          + " Chrome/133.0.0.0 Safari/537.36";
  public static final int DEFAULT_TIMEOUT_MS = 15000;
  public static final int DEFAULT_MAX_RETRIES = 2;
  public static final long DEFAULT_RETRY_DELAY_MS = 200L;

  @FunctionalInterface
  public interface Sleeper {
    void sleep(long millis) throws InterruptedException;
  }

  private enum Action {
    RETURN_RESPONSE,
    RETURN_NULL,
    RETRY
  }

  private static final Sleeper DEFAULT_SLEEPER = Thread::sleep;
  private static volatile Sleeper sleeper = DEFAULT_SLEEPER;

  private static final PortalCircuitBreaker CIRCUIT_BREAKER = new PortalCircuitBreaker();
  private static final PortalRateLimiter RATE_LIMITER = new PortalRateLimiter();
  private static final ThreadLocal<Integer> LAST_STATUS_CODE = new ThreadLocal<>();
  private static final Set<String> ALLOWED_HOST_PREFIXES = Set.of(
      "https://dh.hu/",
      "https://www.dh.hu/",
      "https://oc.hu/",
      "https://www.oc.hu/",
      "https://jofogas.hu/",
      "https://www.jofogas.hu/",
      "https://ingatlan.jofogas.hu/",
      "https://koltozzbe.hu/",
      "https://www.koltozzbe.hu/",
      "https://ingatlanbazar.hu/",
      "https://www.ingatlanbazar.hu/",
      "https://ingatlan.com/",
      "https://www.ingatlan.com/"
  );

  private HttpFetcher() {}

  public static void setSleeperForTesting(Sleeper testSleeper) {
    sleeper = testSleeper != null ? testSleeper : DEFAULT_SLEEPER;
  }

  public static void resetResilienceStateForTests() {
    CIRCUIT_BREAKER.reset();
    RATE_LIMITER.reset();
    LAST_STATUS_CODE.remove();
  }

  public static Integer getLastStatusCode() {
    return LAST_STATUS_CODE.get();
  }

  public static int resolveMaxRetries() {
    return Math.max(0, Integer.getInteger("scraper.http.max-retries", DEFAULT_MAX_RETRIES));
  }

  public static long resolveRetryDelayMs() {
    String prop = System.getProperty("scraper.http.retry-delay-ms");
    if (prop != null) {
      try {
        return Math.max(0L, Long.parseLong(prop.trim()));
      } catch (NumberFormatException ignored) {
        // use fallback
      }
    }
    return Boolean.getBoolean("app.test-mode") ? 0L : DEFAULT_RETRY_DELAY_MS;
  }

  public static Document fetchDocument(String targetUrl) {
    return fetchDocument(targetUrl, DEFAULT_TIMEOUT_MS);
  }

  public static Document fetchDocument(String targetUrl, int timeoutMs) {
    if (!isApproved(targetUrl, timeoutMs)) {
      return null;
    }
    long startTime = System.currentTimeMillis();
    log.debug("HTTP GET Document indítása: {}", targetUrl);
    Connection.Response response = executeWithRetry(targetUrl, timeoutMs, false, null);
    return HttpFetcherHelper.parseDocumentResponse(response, startTime, targetUrl, "GET");
  }

  public static Document postJsonDocument(String targetUrl, String jsonBody) {
    return postJsonDocument(targetUrl, jsonBody, DEFAULT_TIMEOUT_MS);
  }

  public static Document postJsonDocument(String targetUrl, String jsonBody, int timeoutMs) {
    if (!isApproved(targetUrl, timeoutMs)) {
      return null;
    }
    long startTime = System.currentTimeMillis();
    log.debug("HTTP POST JSON Document indítása: {}", targetUrl);
    Connection.Response response = executeWithRetry(targetUrl, timeoutMs, false, jsonBody);
    return HttpFetcherHelper.parseDocumentResponse(response, startTime, targetUrl, "POST");
  }

  public static String fetchJson(String targetUrl) {
    return fetchJson(targetUrl, DEFAULT_TIMEOUT_MS);
  }

  public static String fetchJson(String targetUrl, int timeoutMs) {
    if (!isApproved(targetUrl, timeoutMs)) {
      return null;
    }
    long startTime = System.currentTimeMillis();
    log.debug("HTTP GET JSON indítása: {}", targetUrl);
    Connection.Response response = executeWithRetry(targetUrl, timeoutMs, true, null);
    return HttpFetcherHelper.extractJsonBody(response, startTime, targetUrl);
  }

  private static Connection.Response executeWithRetry(
      String targetUrl, int timeoutMs, boolean isJson, String postBody) {
    LAST_STATUS_CODE.remove();
    String circuitKey = HttpFetcherHelper.resolveCircuitKey(targetUrl);
    if (!CIRCUIT_BREAKER.allowRequest(circuitKey)) {
      log.warn("A(z) {} circuit breakere nyitva van", circuitKey);
      return null;
    }
    String host = HttpFetcherHelper.extractHost(targetUrl);
    int maxRetries = resolveMaxRetries();
    long retryDelayMs = resolveRetryDelayMs();
    boolean outcomeRecorded = false;

    try {
      for (int attempt = 0; attempt <= maxRetries; attempt++) {
        if (!acquirePermit(host, circuitKey)) {
          outcomeRecorded = true;
          return null;
        }
        try {
          Connection.Response response =
              buildConnection(targetUrl, timeoutMs, isJson, postBody).execute();
          Action action =
              processResponse(response, attempt, maxRetries, retryDelayMs, targetUrl, circuitKey);
          if (action == Action.RETURN_RESPONSE) {
            outcomeRecorded = true;
            return response;
          }
          if (action == Action.RETURN_NULL) {
            outcomeRecorded = true;
            return null;
          }
        } catch (IOException | RuntimeException e) {
          if (!handleException(e, attempt, maxRetries, retryDelayMs, targetUrl, circuitKey)) {
            outcomeRecorded = true;
            return null;
          }
        }
      }
      return null;
    } finally {
      if (!outcomeRecorded) {
        CIRCUIT_BREAKER.releaseProbeIfActive(circuitKey);
      }
    }
  }

  private static boolean acquirePermit(String host, String circuitKey) {
    try {
      RATE_LIMITER.acquire(host);
      return true;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      CIRCUIT_BREAKER.recordFailure(circuitKey);
      return false;
    }
  }

  private static Action processResponse(
      Connection.Response response,
      int attempt,
      int maxRetries,
      long retryDelayMs,
      String targetUrl,
      String circuitKey) {
    int statusCode = response.statusCode();
    LAST_STATUS_CODE.set(statusCode);
    if (statusCode >= 200 && statusCode < 300) {
      CIRCUIT_BREAKER.recordSuccess(circuitKey);
      return Action.RETURN_RESPONSE;
    }
    if (statusCode == 403) {
      CIRCUIT_BREAKER.recordFailure(circuitKey);
      log.warn("HTTP státuszkód hiba [{}] az oldal letöltésekor -> URL: {}", statusCode, targetUrl);
      return Action.RETURN_NULL;
    }
    if (statusCode == 404 || statusCode < 400) {
      CIRCUIT_BREAKER.releaseProbeIfActive(circuitKey);
      log.warn("HTTP státuszkód hiba [{}] az oldal letöltésekor -> URL: {}", statusCode, targetUrl);
      return Action.RETURN_NULL;
    }
    if (attempt < maxRetries && HttpFetcherHelper.isRetryableStatus(statusCode)) {
      long waitMs = statusCode == 429
          ? HttpFetcherHelper.parseRetryAfterMs(response.header("Retry-After"), retryDelayMs)
          : retryDelayMs;
      log.warn(
          "Átmeneti HTTP [{}] hiba, újrapróbálkozás ({}/{})... URL: {}",
          statusCode, attempt + 1, maxRetries, targetUrl);
      if (!sleepDelay(waitMs)) {
        CIRCUIT_BREAKER.recordFailure(circuitKey);
        return Action.RETURN_NULL;
      }
      return Action.RETRY;
    }
    CIRCUIT_BREAKER.recordFailure(circuitKey);
    log.warn(
        "HTTP [{}] hiba, próbálkozások kimerültek ({}) -> URL: {}",
        statusCode, attempt + 1, targetUrl);
    return Action.RETURN_NULL;
  }

  private static boolean handleException(
      Exception e,
      int attempt,
      int maxRetries,
      long retryDelayMs,
      String targetUrl,
      String circuitKey) {
    if (HttpFetcherHelper.isRedirectLoop(e)) {
      CIRCUIT_BREAKER.recordFailure(circuitKey);
      log.warn("Átirányítási hurok detektálva -> URL: {} ({})", targetUrl, e.getMessage());
      return false;
    }
    if (attempt < maxRetries) {
      log.warn(
          "Átmeneti hálózati I/O hiba, újrapróbálkozás ({}/{})... URL: {} ({})",
          attempt + 1, maxRetries, targetUrl, e.getMessage());
      if (!sleepDelay(retryDelayMs)) {
        CIRCUIT_BREAKER.recordFailure(circuitKey);
        return false;
      }
      return true;
    }
    CIRCUIT_BREAKER.recordFailure(circuitKey);
    log.error("Hálózati I/O hiba a letöltés során -> URL: {} ({})", targetUrl, e.getMessage());
    return false;
  }

  private static Connection buildConnection(
      String targetUrl, int timeoutMs, boolean isJson, String postBody) {
    Connection connection = Jsoup.connect(targetUrl)
        .userAgent(DEFAULT_USER_AGENT)
        .followRedirects(true)
        .ignoreHttpErrors(true)
        .timeout(timeoutMs);
    return HttpFetcherHelper.applyHeaders(connection, isJson, postBody);
  }

  private static boolean sleepDelay(long delayMs) {
    if (Boolean.getBoolean("app.test-mode") && sleeper == DEFAULT_SLEEPER) {
      return !Thread.currentThread().isInterrupted();
    }
    if (delayMs <= 0) {
      return !Thread.currentThread().isInterrupted();
    }
    try {
      sleeper.sleep(delayMs);
      return true;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  private static boolean isApproved(String targetUrl, int timeoutMs) {
    if (targetUrl == null || targetUrl.isBlank() || timeoutMs <= 0) {
      return false;
    }
    String url = targetUrl.trim();
    if (isAllowedLocalTestUrl(url)) {
      return true;
    }
    return ALLOWED_HOST_PREFIXES.stream().anyMatch(url::startsWith);
  }

  private static boolean isAllowedLocalTestUrl(String url) {
    return Boolean.getBoolean("app.test-mode")
        && (url.startsWith("http://127.0.0.1:") || url.startsWith("http://localhost:"));
  }
}
