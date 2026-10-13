package ingatlanok.common;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public final class HttpFetcher {

    private static final AppLogger LOGGER = AppLogger.getLogger(HttpFetcher.class);

    public static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    public static final int DEFAULT_TIMEOUT_MS = 15000;

    private HttpFetcher() {

    }

    public static Document fetchDocument(String targetUrl) {
        return fetchDocument(targetUrl, DEFAULT_TIMEOUT_MS);
    }

    public static Document fetchDocument(String targetUrl, int timeoutMs) {
        if (targetUrl == null || targetUrl.isBlank()) {
            return null;
        }
        long startTime = System.currentTimeMillis();
        try {
            LOGGER.debug("HTTP GET Document indítása: {}", targetUrl);
            Document document = Jsoup.connect(targetUrl)
                    .userAgent(DEFAULT_USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "hu-HU,hu;q=0.9,en-US;q=0.8,en;q=0.7")
                    .followRedirects(true)
                    .timeout(timeoutMs)
                    .get();

            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.debug("HTTP GET Document sikeres [{} ms] (cím: '{}'): {}", elapsed, document.title(), targetUrl);
            return document;
        } catch (HttpStatusException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.warn("HTTP státuszkód hiba [{}] az oldal letöltésekor [{} ms] -> URL: {} ({})",
                    e.getStatusCode(), elapsed, targetUrl, e.getMessage());
            return null;
        } catch (IOException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.error("Hálózati I/O hiba a letöltés során [{} ms] -> URL: {} ({})", elapsed, targetUrl, e.getMessage());
            return null;
        }
    }

    public static String fetchJson(String targetUrl) {
        return fetchJson(targetUrl, DEFAULT_TIMEOUT_MS);
    }

    public static String fetchJson(String targetUrl, int timeoutMs) {
        if (targetUrl == null || targetUrl.isBlank()) {
            return null;
        }
        long startTime = System.currentTimeMillis();
        try {
            LOGGER.debug("HTTP GET JSON indítása: {}", targetUrl);
            String body = Jsoup.connect(targetUrl)
                    .userAgent(DEFAULT_USER_AGENT)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Language", "hu-HU,hu;q=0.9,en-US;q=0.8,en;q=0.7")
                    .ignoreContentType(true)
                    .followRedirects(true)
                    .timeout(timeoutMs)
                    .execute()
                    .body();

            long elapsed = System.currentTimeMillis() - startTime;
            int length = body != null ? body.length() : 0;
            LOGGER.debug("HTTP GET JSON sikeres [{} ms, {} bájt] -> URL: {}", elapsed, length, targetUrl);
            return body;
        } catch (HttpStatusException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.warn("HTTP státuszkód hiba [{}] JSON letöltésekor [{} ms] -> URL: {} ({})",
                    e.getStatusCode(), elapsed, targetUrl, e.getMessage());
            return null;
        } catch (IOException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.error("Hálózati I/O hiba JSON letöltéskor [{} ms] -> URL: {} ({})", elapsed, targetUrl, e.getMessage());
            return null;
        }
    }
}
