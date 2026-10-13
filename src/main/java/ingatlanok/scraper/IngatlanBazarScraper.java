package ingatlanok.scraper;

import ingatlanok.persistence.IngatlanRepository;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashSet;
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
    public Set<String> getUrls() {
        init();
        Set<String> result = new HashSet<>();

        Document document = getDocument(URL);
        if (document == null) {
            System.out.println("Ingatlanbazár: A lekérés meghiúsult.");
            return result;
        }

        // Linkek keresése ha van statikus tartalom
        Elements links = document.select("a[href*=/ingatlan-]");
        for (Element a : links) {
            String href = a.attr("href");
            if (href != null && !href.isEmpty()) {
                String link = href.startsWith("http") ? href : BASE_URL + (href.startsWith("/") ? "" : "/") + href;
                if (!IngatlanRepository.getInstance().containsUrl(link)) {
                    result.add(link);
                }
            }
        }

        if (result.isEmpty()) {
            System.out.println("Ingatlanbazár: Az oldal kliensoldali JavaScript renderelést (Angular SPA) használ, statikus HTML-ből nem nyerhető ki link.");
        } else {
            System.out.println("Ingatlanbazár Új URL száma : " + result.size());
        }

        return result;
    }
}
