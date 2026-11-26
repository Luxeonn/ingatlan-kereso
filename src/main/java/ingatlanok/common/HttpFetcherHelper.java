package ingatlanok.common;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;

/** Pure header, response, status, and URI helpers for {@link HttpFetcher}. */
@Slf4j
final class HttpFetcherHelper {

  private HttpFetcherHelper() {}

  static Connection applyHeaders(Connection connection, boolean isJson, String postBody) {
    if (postBody != null) {
      return connection
          .method(Connection.Method.POST)
          .requestBody(postBody)
          .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
          .header("Accept-Language", "hu-HU,hu;q=0.9,en-US;q=0.8,en;q=0.7")
          .header("Content-Type", "application/json; charset=UTF-8")
          .header("X-Requested-With", "XMLHttpRequest")
          .ignoreContentType(true);
    }
    if (isJson) {
      return connection
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
          .ignoreContentType(true);
    }
    return connection
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
        .header("upgrade-insecure-requests", "1");
  }

  static Document parseDocumentResponse(
      Connection.Response response, long startTime, String targetUrl, String method) {
    if (response == null) {
      return null;
    }
    try {
      Document document = response.parse();
      long elapsed = System.currentTimeMillis() - startTime;
      log.info(
          "HTTP {} Document sikeres [{} ms] (cím: '{}'): {}",
          method,
          elapsed,
          document.title(),
          targetUrl);
      return document;
    } catch (IOException e) {
      log.error(
          "Hiba a letöltött HTML Document feldolgozásakor -> URL: {} ({})",
          targetUrl,
          e.getMessage());
      return null;
    }
  }

  static String extractJsonBody(
      Connection.Response response, long startTime, String targetUrl) {
    if (response == null) {
      return null;
    }
    String body = response.body();
    long elapsed = System.currentTimeMillis() - startTime;
    int length = body != null ? body.length() : 0;
    log.info("HTTP GET JSON sikeres [{} ms, {} bájt] -> URL: {}", elapsed, length, targetUrl);
    return body;
  }

  static boolean isRetryableStatus(int statusCode) {
    return (statusCode >= 500 && statusCode < 600) || statusCode == 429;
  }

  static boolean isRedirectLoop(Exception e) {
    if (e == null || e.getMessage() == null) {
      return false;
    }
    String msg = e.getMessage().toLowerCase(Locale.ROOT);
    return msg.contains("too many redirects") || msg.contains("redirect loop");
  }

  static long parseRetryAfterMs(String retryAfterHeader, long defaultDelayMs) {
    if (retryAfterHeader == null || retryAfterHeader.isBlank()) {
      return defaultDelayMs;
    }
    try {
      long seconds = Long.parseLong(retryAfterHeader.trim());
      if (seconds > 0) {
        return Math.min(seconds * 1000L, 5000L);
      }
    } catch (NumberFormatException ignored) {
      // not an integer
    }
    return defaultDelayMs;
  }

  static String extractHost(String targetUrl) {
    try {
      URI uri = URI.create(targetUrl.trim());
      String host = uri.getHost();
      return host != null ? host.toLowerCase(Locale.ROOT) : "default";
    } catch (Exception e) {
      return "default";
    }
  }

  static String resolveCircuitKey(String targetUrl) {
    try {
      URI uri = URI.create(targetUrl.trim());
      String host = uri.getHost() != null ? uri.getHost().toLowerCase(Locale.ROOT) : "default";
      return host + ":" + (uri.getPort() > 0 ? uri.getPort() : 443);
    } catch (Exception e) {
      return targetUrl;
    }
  }
}
