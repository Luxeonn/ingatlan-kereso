package ingatlanok.scraper;

import ingatlanok.common.HttpFetcher;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Közös absztrakt ősosztály ingatlan scraper szolgáltatásokhoz.
 * A hálózati kommunikációt és letöltést a közös HttpFetcher modulon keresztül végzi.
 */
public abstract class BaseScraper {

    public abstract void init();

    public abstract int getCardNumber(Element resultPageSize);

    public Document getUrlContent(int page, String url) {
        String fullUrl = url.contains("?") ? url + "&page=" + page : url + (url.endsWith("/") ? "" : "/") + "oldal-" + page;
        return getDocument(fullUrl);
    }

    public Document getDocument(String targetUrl) {
        return HttpFetcher.fetchDocument(targetUrl);
    }
}
