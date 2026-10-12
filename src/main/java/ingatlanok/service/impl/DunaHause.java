package ingatlanok.service.impl;

import ingatlanok.persistence.IngatlanDataBaseMock;
import ingatlanok.service.Ingatlan;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashSet;
import java.util.Set;

/**
 * Scraper implementáció a Duna House (dh.hu) portálhoz.
 */
public class DunaHause extends IngatlanStart implements Ingatlan {

    private static final String BASE_URL = "https://dh.hu";
    private static final String SEARCH_ROUTE = "/elado-ingatlan/lakas/hajdu-bihar/debrecen";
    public static final String URL = BASE_URL + SEARCH_ROUTE;

    @Override
    public void init() {
        System.out.println("Duna House keresés: " + URL);
    }

    @Override
    public int getCardNumber(Element resultPageSize) {
        return 20;
    }

    @Override
    public Set<String> getUrls() {
        init();
        Set<String> result = new HashSet<>();

        // Az első 2 oldalt olvassuk be a friss találatokhoz
        int maxPages = 2;
        for (int i = 1; i <= maxPages; i++) {
            String pageUrl = (i == 1) ? URL : URL + "/oldal-" + i;
            Document document = getDocument(pageUrl);
            if (document == null) {
                System.err.println("Nem sikerült letölteni a Duna House oldalt: " + pageUrl);
                continue;
            }

            // Kártyák linkjei a meta vagy a tagekből
            Elements metaUrls = document.select("meta[itemprop=url]");
            for (Element meta : metaUrls) {
                String href = meta.attr("content");
                processLink(href, result);
            }

            Elements anchorLinks = document.select("a[href*=/ingatlan/]");
            for (Element a : anchorLinks) {
                String href = a.attr("href");
                processLink(href, result);
            }
        }

        System.out.println("dh.hu Új URL száma : " + result.size());
        return result;
    }

    private void processLink(String href, Set<String> result) {
        if (href == null || href.trim().isEmpty()) {
            return;
        }
        String fullLink = href.startsWith("http") ? href : BASE_URL + (href.startsWith("/") ? "" : "/") + href;
        // Csak konkrét hirdetés oldalakat gyűjtsünk (amiben szerepel ingatlan azonosító)
        if (fullLink.matches(".*/ingatlan/[A-Za-z0-9]+/.*") || fullLink.matches(".*/ingatlan/[A-Za-z0-9]+$")) {
            if (!IngatlanDataBaseMock.getUrls().contains(fullLink)) {
                result.add(fullLink);
            }
        }
    }
}
