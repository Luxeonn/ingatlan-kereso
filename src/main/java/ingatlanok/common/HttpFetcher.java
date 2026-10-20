package ingatlanok.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
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
  private static final Set<String> APPROVED_HOSTS =
      Set.of("dh.hu", "oc.hu", "jofogas.hu", "koltozzbe.hu", "ingatlanbazar.hu", "ingatlan.com");
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
    URI uri;
    try {
      uri = URI.create(targetUrl);
    } catch (IllegalArgumentException exception) {
      LOGGER.warn("Érvénytelen külső URL: {}", targetUrl);
      return null;
    }
    for (int redirects = 0; redirects <= 5; redirects++) {
      String host = uri.getHost();
      if (host == null) {
        LOGGER.warn("Érvénytelen külső URL (hiányzó host): {}", uri);
        return null;
      }
      String normalizedHost = host.toLowerCase(Locale.ROOT);
      String approvedHost = getApprovedHost(normalizedHost);
      if (approvedHost == null) {
        LOGGER.warn("A portál domainje nem engedélyezett: {}", normalizedHost);
        return null;
      }
      boolean testMode =
          Boolean.getBoolean("app.test-mode")
              && (approvedHost.equals("127.0.0.1") || approvedHost.equals("localhost"));
      if (!testMode && !"https".equalsIgnoreCase(uri.getScheme())) {
        LOGGER.warn("A portál protokollja nem engedélyezett: {}", uri.getScheme());
        return null;
      }

      String scheme = testMode ? uri.getScheme().toLowerCase(Locale.ROOT) : "https";
      int port = uri.getPort();
      String rawPath = uri.getRawPath() == null || uri.getRawPath().isBlank() ? "/" : uri.getRawPath();
      String rawQuery = uri.getRawQuery();
      URI safeUri =
          URI.create(
              scheme
                  + "://"
                  + approvedHost
                  + (port > 0 ? ":" + port : "")
                  + rawPath
                  + (rawQuery != null && !rawQuery.isBlank() ? "?" + rawQuery : ""));

      String circuitKey = approvedHost + ":" + (port > 0 ? port : 443);
      if (testMode) {
        circuitKey += rawPath;
      }
      if (!CIRCUIT_BREAKER.allowRequest(circuitKey)) {
        LOGGER.warn("A(z) {} portál circuit breakere nyitva van", approvedHost);
        return null;
      }
      try {
        RATE_LIMITER.acquire(approvedHost);
        HttpRequest request =
            HttpRequest.newBuilder(safeUri)
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
          URI resolved = safeUri.resolve(location);
          if (resolved.getHost() == null) {
            CIRCUIT_BREAKER.recordFailure(circuitKey);
            return null;
          }
          uri = resolved;
          continue;
        }
        if (status < 200 || status >= 300) {
          CIRCUIT_BREAKER.recordFailure(circuitKey);
          LOGGER.warn("HTTP státuszkód hiba [{}] -> URL: {}", status, safeUri);
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
        LOGGER.warn("Hálózati hiba a portál lekérésekor [{}]: {}", safeUri, exception.getMessage());
        return null;
      }
    }
    return null;
  }

  private static String getApprovedHost(String host) {
    if (host == null || host.isBlank()) {
      return null;
    }
    if (Boolean.getBoolean("app.test-mode")
        && (host.equals("127.0.0.1") || host.equals("localhost"))) {
      return host;
    }
    if (APPROVED_HOSTS.contains(host)) {
      return host;
    }
    for (String domain : APPROVED_HOSTS) {
      if (host.endsWith("." + domain)) {
        return host;
      }
    }
    return null;
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
