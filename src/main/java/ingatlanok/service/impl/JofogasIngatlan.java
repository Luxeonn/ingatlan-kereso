package ingatlanok.service.impl;

import ingatlanok.persistence.IngatlanDataBaseMock;
import ingatlanok.service.Ingatlan;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashSet;
import java.util.Set;

/**
 * Scraper implementáció a Jófogás Ingatlan (jofogas.hu) portálhoz.
 */
public class JofogasIngatlan extends IngatlanStart implements Ingatlan {

    private static final String BASE_URL = "https://ingatlan.jofogas.hu";
    private static final String SEARCH_URL = "https://ingatlan.jofogas.hu/hajdu-bihar/ingatlan?q=debrecen+elado+lakas";

    @Override
    public void init() {
        System.out.println("Jófogás Ingatlan keresés: " + SEARCH_URL);
    }

    @Override
    public int getCardNumber(Element resultPageSize) {
        return 25;
    }

    @Override
    public Set<String> getUrls() {
        init();
        Set<String> result = new HashSet<>();

        Document document = getDocument(SEARCH_URL);
        if (document == null) {
            System.err.println("Nem sikerült letölteni a Jófogás keresési oldalt: " + SEARCH_URL);
            return result;
        }

        // Hirdetés linkek keresése (.htm kiterjesztéssel rendelkező ingatlan linkek)
        Elements links = document.select("a[href*=/hajdu_bihar/]");
        for (Element a : links) {
            String href = a.attr("href");
            if (href == null || href.isEmpty()) {
                continue;
            }

            if (href.contains("jofogas.hu") && href.endsWith(".htm")) {
                // Csak az ingatlan hirdetés linkjeit rögzítsük
                if (!IngatlanDataBaseMock.getUrls().contains(href)) {
                    result.add(href);
                }
            }
        }

        System.out.println("jofogas.hu Új URL száma : " + result.size());
        return result;
    }
}
