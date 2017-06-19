package ingatlanok.service.impl;

import ingatlanok.persistence.IngatlanDataBaseMock;
import ingatlanok.service.Ingatlan;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Pusinszky on 2017. 06. 05..
 */
public class OtthonCentrum extends IngatlanStart implements Ingatlan {

    private static final String BASE_URL = "https://www.oc.hu";
    private static final String START_ROUTE = "/ingatlanok/lista/oldalszam:12/rendezes:relevance/felhasznalas:elado";
    private static final String ROUTE_1 = "/tipus:flat/hely-ertek:debrecen/hely-id:debrecen,";
    private static final String ROUTE_PRICE = "/ar:9~11";
    private static final String ROUTE_2 = "/netto-alapterulet:35~/szobak:2~3/emelet:2,24/szobak:2~3/emelet:2,24";
    private static final String END_ROUTE = "?page=";
    public static String url = BASE_URL + START_ROUTE + ROUTE_1 + ROUTE_PRICE + ROUTE_2 + END_ROUTE;
    private int numberOfCard;

    public void init() {
        System.out.println("URL : " + url);
        Document document = getUrlContent(1, url);
        Element resultPageSize = document.select("span.atEstateList-totalCount").first();
        numberOfCard = Integer.parseInt(resultPageSize.getAllElements().tagName("strong").get(1).html());
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
        int page = numberOfCard / 12;
        page = page == 0 ? 1 : page;
        for (int i = 1; i <= page; i++) {
            Document document = getUrlContent(i, url);
            Elements cards = document.getElementsByClass("col-md-4 col-sm-6 col-s-6 col-xs-12 ");
            cards.addAll(document.getElementsByClass("col-md-4 col-sm-6 col-s-6 col-xs-12 row-breaker-tablet"));
            cards.addAll(document.getElementsByClass("col-md-4 col-sm-6 col-s-6 col-xs-12 row-breaker"));
            for (Element div : cards) {
                Elements child = div.select("a[href]");
                String link = BASE_URL + child.attr("href");
                if (IngatlanDataBaseMock.getUrls().contains(link)) {
                } else {
                    result.add(link);
                }
            }
        }
        System.out.println("Otthon centrum Új URL száma : " + result.size());
        return result;
    }
}
