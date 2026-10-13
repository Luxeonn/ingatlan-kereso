package ingatlanok.scraper;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.persistence.IngatlanRepository;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Scraper implementáció az Ingatlanbazár (ingatlanbazar.hu) portálhoz.
 * Megjegyzés: A portál jelenleg Angular SPA-t használ, a hirdetések kliensoldalon töltődnek be.
 */
public class IngatlanBazarScraper extends BaseScraper implements IngatlanScraper {

    private static final String BASE_URL = "https://www.ingatlanbazar.hu";
    private static final String SEARCH_ROUTE = "/ingatlan-Debrecen-Elado-Lakas";
    public static final String URL = BASE_URL + SEARCH_ROUTE;

    @Override
    public String getPortalId() {
        return "ingatlanbazar";
    }

    @Override
    public String getName() {
        return "Ingatlanbazár";
    }

    @Override
    public void init() {
        System.out.println("Ingatlanbazár keresés: " + URL);
    }

    @Override
    public int getCardNumber(Element resultPageSize) {
        return 0;
    }

    @Override
    public PortalScrapeResult scrape(SearchCriteria criteria) {
        long startTime = System.currentTimeMillis();
        init();
        List<IngatlanItem> items = new ArrayList<>();

        try {
            Document document = getDocument(URL);
            if (document == null) {
                long elapsed = System.currentTimeMillis() - startTime;
                return PortalScrapeResult.failure(getPortalId(), getName(), "Nem sikerült letölteni a kezdőoldalt", elapsed);
            }

            // Linkek keresése ha van statikus tartalom
            Elements links = document.select("a[href*=/ingatlan-]");
            for (Element a : links) {
                String href = a.attr("href");
                if (href != null && !href.isEmpty()) {
                    String link = href.startsWith("http") ? href : BASE_URL + (href.startsWith("/") ? "" : "/") + href;
                    if (!IngatlanRepository.getInstance().containsUrl(link)) {
                        // Statikus link jelenléte esetén
                    }
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("Ingatlanbazár: Az oldal kliensoldali JavaScript renderelést (Angular SPA) használ, statikus HTML-ből nem nyerhető ki link.");
            return PortalScrapeResult.spaFallback(getPortalId(), getName(), URL, elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            return PortalScrapeResult.failure(getPortalId(), getName(), e.getMessage(), elapsed);
        }
    }
}
