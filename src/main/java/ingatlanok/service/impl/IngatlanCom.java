package ingatlanok.service.impl;

import ingatlanok.persistence.IngatlanDataBaseMock;
import ingatlanok.service.Ingatlan;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashSet;
import java.util.Set;

/**
 * Scraper implementáció az Ingatlan.com portálhoz.
 * Megjegyzés: A portál jelenleg szigorú Cloudflare bot-védelmet alkalmaz.
 */
public class IngatlanCom extends IngatlanStart implements Ingatlan {

    private static final String BASE_URL = "https://ingatlan.com";
    private static final String SEARCH_ROUTE = "/lista/elado+lakas+debrecen";
    public static final String URL = BASE_URL + SEARCH_ROUTE;

    @Override
    public String getName() {
        return "Ingatlan.com";
    }

    @Override
    public void init() {
        System.out.println("Ingatlan.com keresés: " + URL);
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
            System.out.println("Ingatlan.com: A lekérés meghiúsult (Cloudflare bot-védelem aktív, átugorva).");
            return result;
        }

        Elements cards = document.select("a[href*=/ingatlan/]");
        for (Element a : cards) {
            String href = a.attr("href");
            if (href != null && !href.isEmpty()) {
                String link = href.startsWith("http") ? href : BASE_URL + (href.startsWith("/") ? "" : "/") + href;
                if (!IngatlanDataBaseMock.getUrls().contains(link)) {
                    result.add(link);
                }
            }
        }

        System.out.println("Ingatlan.com Új URL száma : " + result.size());
        return result;
    }
}
