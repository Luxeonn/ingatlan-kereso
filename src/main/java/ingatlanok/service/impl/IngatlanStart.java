package ingatlanok.service.impl;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;

/**
 * Közös absztrakt ősosztály ingatlan scraper szolgáltatásokhoz.
 */
public abstract class IngatlanStart {

    protected static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    protected static final int TIMEOUT_MS = 15000;

    public abstract void init();

    public abstract int getCardNumber(Element resultPageSize);

    public Document getUrlContent(int page, String url) {
        String fullUrl = url.contains("?") ? url + "&page=" + page : url + (url.endsWith("/") ? "" : "/") + "oldal-" + page;
        return getDocument(fullUrl);
    }

    public Document getDocument(String targetUrl) {
        try {
            return Jsoup.connect(targetUrl)
                    .userAgent(USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "hu-HU,hu;q=0.9,en-US;q=0.8,en;q=0.7")
                    .followRedirects(true)
                    .timeout(TIMEOUT_MS)
                    .get();
        } catch (IOException e) {
            System.err.println("Hiba a letöltés során [" + targetUrl + "]: " + e.getMessage());
            return null;
        }
    }

}
