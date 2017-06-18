package ingatlanok.service.impl;

import ingatlanok.persistence.IngatlanDataBaseMock;
import ingatlanok.service.Ingatlan;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Pusinszky on 2017. 06. 05..
 */
public class IngatlanBazar extends IngatlanStart implements Ingatlan {

    private static final String BASE_URL = "https://www.ingatlanbazar.hu";
    private static final String START_ROUTE = "/ingatlan-Debrecen-Elado-Lakas-9-11-millio-Ft";
    private static final String ROUTE_1 = "?property__2=3&property_location%5B0%5D=1435";
    private static final String ROUTE_PRICE = "&price_min=9&price_max=11";
    private static final String FILTER_1 = "&property_imageonly=on";
    private static final String FILTER_2 = "&property_build_date=&searchtext=&list-order=0";
    private static final String END_ROUTE = "&page=";
    public static String url = BASE_URL + START_ROUTE + ROUTE_1 + ROUTE_PRICE + FILTER_1 + FILTER_2+ END_ROUTE;
    private int numberOfCard;

    public void init() {
        System.out.println("URL : " + url);
        numberOfCard = 1;
    }

    public int getCardNumber(Element resultPageSize) {
        int result = 1;
        try {
            result = Integer.valueOf(resultPageSize.ownText());
        } catch (NumberFormatException e) {
        }
        return result;
    }

    public Set<String> getUrls() {
        init();
        Set<String> result = new HashSet<String>();
        int page = numberOfCard / 10;
        page = page == 0 ? 1 : page;
        for (int i = 1; i <= page; i++) {
            Document document = getUrlContent(i, url);
            Elements cards = document.getElementsByClass("span10 property-description");
            for (Element div : cards) {
                Elements child = div.select("a[href]");
                String link = BASE_URL + child.attr("href");
                if (IngatlanDataBaseMock.getUrls().contains(link)) {
                } else {
                    result.add(link);
                }
            }
        }
        System.out.println("Ingatlan.com Új URL száma : " + result.size());
        return result;
    }
}
