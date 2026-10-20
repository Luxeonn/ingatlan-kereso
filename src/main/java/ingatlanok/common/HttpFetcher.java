package ingatlanok.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import org.brotli.dec.BrotliInputStream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import ingatlanok.core.resilience.PortalCircuitBreaker;
import ingatlanok.core.resilience.PortalRateLimiter;

/** Shared HTTP/2 client with pooled connections and portal-specific resilience controls. */
public final class HttpFetcher {

  private static final AppLogger LOGGER = AppLogger.getLogger(HttpFetcher.class);
  private static final List<String> APPROVED_PREFIXES =
      List.of(
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
          "https://www.ingatlan.com/");

  private static final HttpClient CLIENT =
      HttpClient.newBuilder()
          .version(HttpClient.Version.HTTP_2)
          .executor(Executors.newVirtualThreadPerTaskExecutor())
          .connectTimeout(Duration.ofMillis(15000))
          .followRedirects(HttpClient.Redirect.NEVER)
          .build();
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
    String html =
        fetch(
            targetUrl,
            timeoutMs,
            "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
    return html == null ? null : Jsoup.parse(html, targetUrl);
  }

  public static String fetchJson(String targetUrl) {
    return fetchJson(targetUrl, DEFAULT_TIMEOUT_MS);
  }

  public static String fetchJson(String targetUrl, int timeoutMs) {
    return fetch(targetUrl, timeoutMs, "application/json, text/plain, */*");
  }

  private static String fetch(String targetUrl, int timeoutMs, String accept) {
    if (targetUrl == null || targetUrl.isBlank() || timeoutMs <= 0) {
      return null;
    }
    String currentUrl = targetUrl.trim();
    for (int redirects = 0; redirects <= 5; redirects++) {
      if (!isApprovedUrl(currentUrl)) {
        LOGGER.warn("A portál URL nem engedélyezett: {}", currentUrl);
        return null;
      }
      URI uri;
      try {
        uri = URI.create(currentUrl);
      } catch (IllegalArgumentException exception) {
        LOGGER.warn("Érvénytelen külső URL: {}", currentUrl);
        return null;
      }
      String host = uri.getHost();
      if (host == null) {
        LOGGER.warn("Érvénytelen külső URL (hiányzó host): {}", currentUrl);
        return null;
      }
      String normalizedHost = host.toLowerCase(Locale.ROOT);
      String circuitKey = normalizedHost + ":" + (uri.getPort() > 0 ? uri.getPort() : 443);
      if (Boolean.getBoolean("app.test-mode")) {
        circuitKey += uri.getPath();
      }
      if (!CIRCUIT_BREAKER.allowRequest(circuitKey)) {
        LOGGER.warn("A(z) {} portál circuit breakere nyitva van", normalizedHost);
        return null;
      }
      try {
        RATE_LIMITER.acquire(normalizedHost);
        HttpRequest request =
            HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(Math.min(timeoutMs, DEFAULT_TIMEOUT_MS)))
                .header("User-Agent", DEFAULT_USER_AGENT)
                .header("Accept", accept)
                .header("Accept-Language", "hu-HU,hu;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header(
                    "sec-ch-ua", "\"Google Chrome\";v=\"133\", \"Chromium\";v=\"133\"")
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-ch-ua-platform", "\"Windows\"")
                .GET()
                .build();
        HttpResponse<byte[]> response =
            CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
        int status = response.statusCode();
        if (status >= 300 && status < 400) {
          String location = response.headers().firstValue("location").orElse(null);
          if (location == null || redirects == 5) {
            CIRCUIT_BREAKER.recordFailure(circuitKey);
            return null;
          }
          currentUrl = uri.resolve(location).toASCIIString();
          continue;
        }
        if (status < 200 || status >= 300) {
          CIRCUIT_BREAKER.recordFailure(circuitKey);
          LOGGER.warn("HTTP státuszkód hiba [{}] -> URL: {}", status, currentUrl);
          return null;
        }
        CIRCUIT_BREAKER.recordSuccess(circuitKey);
        return decode(response.body(), response.headers().firstValue("content-encoding").orElse(""));
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        CIRCUIT_BREAKER.recordFailure(circuitKey);
        return null;
      } catch (IOException | RuntimeException exception) {
        CIRCUIT_BREAKER.recordFailure(circuitKey);
        LOGGER.warn("Hálózati hiba a portál lekérésekor [{}]: {}", currentUrl, exception.getMessage());
        return null;
      }
    }
    return null;
  }

  private static boolean isApprovedUrl(String url) {
    if (url == null || url.isBlank()) {
      return false;
    }
    if (Boolean.getBoolean("app.test-mode")
        && (url.startsWith("http://127.0.0.1:") || url.startsWith("http://localhost:"))) {
      return true;
    }
    for (String prefix : APPROVED_PREFIXES) {
      if (url.startsWith(prefix) || url.equals(prefix.substring(0, prefix.length() - 1))) {
        return true;
      }
    }
    return false;
  }

  private static String decode(byte[] body, String encoding) throws IOException {
    byte[] bytes = body;
    if ("gzip".equalsIgnoreCase(encoding)) {
      try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(body))) {
        bytes = gzip.readAllBytes();
      }
    } else if ("deflate".equalsIgnoreCase(encoding)) {
      try (InflaterInputStream deflate = new InflaterInputStream(new ByteArrayInputStream(body))) {
        bytes = deflate.readAllBytes();
      }
    } else if ("br".equalsIgnoreCase(encoding)) {
      try (BrotliInputStream brotli = new BrotliInputStream(new ByteArrayInputStream(body))) {
        bytes = brotli.readAllBytes();
      }
    }
    return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
  }
}
