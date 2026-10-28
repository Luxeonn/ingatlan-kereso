package ingatlanok.common;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import ingatlanok.core.resilience.PortalCircuitBreaker;
import ingatlanok.core.resilience.PortalRateLimiter;

/** Centralized resilient HTTP fetcher with domain whitelisting, rate limiting, and circuit breaking. */
public final class HttpFetcher {

  private static final AppLogger LOGGER = AppLogger.getLogger(HttpFetcher.class);
  private static final PortalCircuitBreaker CIRCUIT_BREAKER = new PortalCircuitBreaker();
  private static final PortalRateLimiter RATE_LIMITER = new PortalRateLimiter();

  public static final String DEFAULT_USER_AGENT =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)"
          + " Chrome/133.0.0.0 Safari/537.36";
  public static final int DEFAULT_TIMEOUT_MS = 15000;

  private HttpFetcher() {}

  public static Document fetchDocument(String targetUrl) {
    return fetchDocument(targetUrl, DEFAULT_TIMEOUT_MS);
  }

  public static Document fetchDocument(String targetUrl, int timeoutMs) {
    if (!isApproved(targetUrl, timeoutMs)) {
      return null;
    }
    String circuitKey = resolveCircuitKey(targetUrl);
    if (!CIRCUIT_BREAKER.allowRequest(circuitKey)) {
      LOGGER.warn("A(z) {} circuit breakere nyitva van", circuitKey);
      return null;
    }
    String host = extractHost(targetUrl);
    try {
      RATE_LIMITER.acquire(host);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      CIRCUIT_BREAKER.recordFailure(circuitKey);
      return null;
    }

    long startTime = System.currentTimeMillis();
    try {
      LOGGER.debug("HTTP GET Document indítása: {}", targetUrl);
      Document document =
          Jsoup.connect(targetUrl)
              .userAgent(DEFAULT_USER_AGENT)
              .header(
                  "Accept",
                  "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
              .header("Accept-Language", "hu-HU,hu;q=0.9,en-US;q=0.8,en;q=0.7")
              .header("Accept-Encoding", "gzip, deflate")
              .header(
                  "sec-ch-ua",
                  "\"Not(A:Brand\";v=\"99\", \"Google Chrome\";v=\"133\", \"Chromium\";v=\"133\"")
              .header("sec-ch-ua-mobile", "?0")
              .header("sec-ch-ua-platform", "\"Windows\"")
              .header("sec-fetch-dest", "document")
              .header("sec-fetch-mode", "navigate")
              .header("sec-fetch-site", "none")
              .header("sec-fetch-user", "?1")
              .header("upgrade-insecure-requests", "1")
              .followRedirects(true)
              .timeout(timeoutMs)
              .get();

      CIRCUIT_BREAKER.recordSuccess(circuitKey);
      long elapsed = System.currentTimeMillis() - startTime;
      LOGGER.info(
          "HTTP GET Document sikeres [{} ms] (cím: '{}'): {}",
          elapsed,
          document.title(),
          targetUrl);
      return document;
    } catch (HttpStatusException e) {
      CIRCUIT_BREAKER.recordFailure(circuitKey);
      long elapsed = System.currentTimeMillis() - startTime;
      LOGGER.warn(
          "HTTP státuszkód hiba [{}] az oldal letöltésekor [{} ms] -> URL: {} ({})",
          e.getStatusCode(),
          elapsed,
          targetUrl,
          e.getMessage());
      return null;
    } catch (IOException | RuntimeException e) {
      CIRCUIT_BREAKER.recordFailure(circuitKey);
      long elapsed = System.currentTimeMillis() - startTime;
      LOGGER.error(
          "Hálózati I/O hiba a letöltés során [{} ms] -> URL: {} ({})",
          elapsed,
          targetUrl,
          e.getMessage());
      return null;
    }
  }

  public static String fetchJson(String targetUrl) {
    return fetchJson(targetUrl, DEFAULT_TIMEOUT_MS);
  }

  public static String fetchJson(String targetUrl, int timeoutMs) {
    if (!isApproved(targetUrl, timeoutMs)) {
      return null;
    }
    String circuitKey = resolveCircuitKey(targetUrl);
    if (!CIRCUIT_BREAKER.allowRequest(circuitKey)) {
      LOGGER.warn("A(z) {} circuit breakere nyitva van", circuitKey);
      return null;
    }
    String host = extractHost(targetUrl);
    try {
      RATE_LIMITER.acquire(host);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      CIRCUIT_BREAKER.recordFailure(circuitKey);
      return null;
    }

    long startTime = System.currentTimeMillis();
    try {
      LOGGER.debug("HTTP GET JSON indítása: {}", targetUrl);
      String body =
          Jsoup.connect(targetUrl)
              .userAgent(DEFAULT_USER_AGENT)
              .header("Accept", "application/json, text/plain, */*")
              .header("Accept-Language", "hu-HU,hu;q=0.9,en-US;q=0.8,en;q=0.7")
              .header("Accept-Encoding", "gzip, deflate")
              .header(
                  "sec-ch-ua",
                  "\"Not(A:Brand\";v=\"99\", \"Google Chrome\";v=\"133\", \"Chromium\";v=\"133\"")
              .header("sec-ch-ua-mobile", "?0")
              .header("sec-ch-ua-platform", "\"Windows\"")
              .header("sec-fetch-dest", "empty")
              .header("sec-fetch-mode", "cors")
              .header("sec-fetch-site", "same-origin")
              .ignoreContentType(true)
              .followRedirects(true)
              .timeout(timeoutMs)
              .execute()
              .body();

      CIRCUIT_BREAKER.recordSuccess(circuitKey);
      long elapsed = System.currentTimeMillis() - startTime;
      int length = body != null ? body.length() : 0;
      LOGGER.info("HTTP GET JSON sikeres [{} ms, {} bájt] -> URL: {}", elapsed, length, targetUrl);
      return body;
    } catch (HttpStatusException e) {
      CIRCUIT_BREAKER.recordFailure(circuitKey);
      long elapsed = System.currentTimeMillis() - startTime;
      LOGGER.warn(
          "HTTP státuszkód hiba [{}] JSON letöltésekor [{} ms] -> URL: {} ({})",
          e.getStatusCode(),
          elapsed,
          targetUrl,
          e.getMessage());
      return null;
    } catch (IOException | RuntimeException e) {
      CIRCUIT_BREAKER.recordFailure(circuitKey);
      long elapsed = System.currentTimeMillis() - startTime;
      LOGGER.error(
          "Hálózati I/O hiba JSON letöltéskor [{} ms] -> URL: {} ({})",
          elapsed,
          targetUrl,
          e.getMessage());
      return null;
    }
  }

  private static boolean isApproved(String targetUrl, int timeoutMs) {
    if (targetUrl == null || targetUrl.isBlank() || timeoutMs <= 0) {
      return false;
    }
    String url = targetUrl.trim();
    if (Boolean.getBoolean("app.test-mode")
        && (url.startsWith("http://127.0.0.1:") || url.startsWith("http://localhost:"))) {
      return true;
    }
    return url.startsWith("https://dh.hu/")
        || url.startsWith("https://www.dh.hu/")
        || url.startsWith("https://oc.hu/")
        || url.startsWith("https://www.oc.hu/")
        || url.startsWith("https://jofogas.hu/")
        || url.startsWith("https://www.jofogas.hu/")
        || url.startsWith("https://ingatlan.jofogas.hu/")
        || url.startsWith("https://koltozzbe.hu/")
        || url.startsWith("https://www.koltozzbe.hu/")
        || url.startsWith("https://ingatlanbazar.hu/")
        || url.startsWith("https://www.ingatlanbazar.hu/")
        || url.startsWith("https://ingatlan.com/")
        || url.startsWith("https://www.ingatlan.com/");
  }

  private static String extractHost(String targetUrl) {
    try {
      URI uri = URI.create(targetUrl.trim());
      String host = uri.getHost();
      return host != null ? host.toLowerCase(Locale.ROOT) : "default";
    } catch (Exception e) {
      return "default";
    }
  }

  private static String resolveCircuitKey(String targetUrl) {
    try {
      URI uri = URI.create(targetUrl.trim());
      String host = uri.getHost() != null ? uri.getHost().toLowerCase(Locale.ROOT) : "default";
      String key = host + ":" + (uri.getPort() > 0 ? uri.getPort() : 443);
      if (Boolean.getBoolean("app.test-mode") && uri.getPath() != null) {
        key += uri.getPath();
      }
      return key;
    } catch (Exception e) {
      return targetUrl;
    }
  }
}
