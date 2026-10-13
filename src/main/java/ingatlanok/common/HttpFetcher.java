package ingatlanok.common;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public final class HttpFetcher {

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
        try {
            return Jsoup.connect(targetUrl)
                    .userAgent(DEFAULT_USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "hu-HU,hu;q=0.9,en-US;q=0.8,en;q=0.7")
                    .followRedirects(true)
                    .timeout(timeoutMs)
                    .get();
        } catch (IOException e) {
            System.err.println("Hiba a letöltés során [" + targetUrl + "]: " + e.getMessage());
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
        try {
            return Jsoup.connect(targetUrl)
                    .userAgent(DEFAULT_USER_AGENT)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Language", "hu-HU,hu;q=0.9,en-US;q=0.8,en;q=0.7")
                    .ignoreContentType(true)
                    .followRedirects(true)
                    .timeout(timeoutMs)
                    .execute()
                    .body();
        } catch (IOException e) {
            System.err.println("Hiba a JSON letöltés során [" + targetUrl + "]: " + e.getMessage());
            return null;
        }
    }
}
