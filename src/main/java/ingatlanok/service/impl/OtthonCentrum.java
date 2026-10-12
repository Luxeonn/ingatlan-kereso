package ingatlanok.service.impl;

import ingatlanok.persistence.IngatlanDataBaseMock;
import ingatlanok.service.Ingatlan;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashSet;
import java.util.Set;

/**
 * Scraper implementáció az Otthon Centrum (oc.hu) portálhoz.
 */
public class OtthonCentrum extends IngatlanStart implements Ingatlan {

    private static final String BASE_URL = "https://www.oc.hu";
    private static final String SEARCH_ROUTE = "/ingatlanok/kereses?realestate%5Bertekesites%5D=elado&realestate%5Bjelleg%5D=lakas&debrecen=on";
    public static final String URL = BASE_URL + SEARCH_ROUTE;

    @Override
    public void init() {
        System.out.println("Otthon Centrum keresés: " + URL);
    }

    @Override
    public int getCardNumber(Element resultPageSize) {
        return 12;
    }

    @Override
    public Set<String> getUrls() {
        init();
        Set<String> result = new HashSet<>();

        Document document = getDocument(URL);
        if (document == null) {
            System.err.println("Nem sikerült letölteni az Otthon Centrum keresési oldalt: " + URL);
            return result;
        }

        // Hirdetés linkek keresése a kártyákból és az ingatlan hivatkozásokból
        Elements links = document.select("a[href*=/ingatlanok/]");
        for (Element a : links) {
            String href = a.attr("href");
            if (href == null || href.isEmpty()) {
                continue;
            }

            // Kizárjuk a navigációs linkeket (/ingatlanok/lista, /ingatlanok/kereses, stb.)
            if (href.matches(".*/ingatlanok/[A-Za-z0-9]+$")) {
                String fullLink = href.startsWith("http") ? href : BASE_URL + (href.startsWith("/") ? "" : "/") + href;
                if (!IngatlanDataBaseMock.getUrls().contains(fullLink)) {
                    result.add(fullLink);
                }
            }
        }

        System.out.println("oc.hu Új URL száma : " + result.size());
        return result;
    }
}
