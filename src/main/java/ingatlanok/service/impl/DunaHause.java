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
 * Created by Pusinszky on 2017. 06. 17..
 */
public class DunaHause extends IngatlanStart implements Ingatlan {

    private static final String BASE_URL = "https://dh.hu";
    private static final String START_ROUTE = "/elado-ingatlan/lakas/hajdu-bihar-megye/debrecen/-/";
    private static final String ROUTE_PRICE = "9-11-mFt";
    private static final String END_ROUTE = "/35-m2-tol/1-3-szoba/oldal-";
    public static String url = BASE_URL + START_ROUTE + ROUTE_PRICE + END_ROUTE;

    private int numberOfCard;

    public void init() {
        System.out.println("URL : " + url);
        String urlContent = getUrlContent(1, url);
        Document document = Jsoup.parse(urlContent);
        Element resultPageSize = document.select("div.selected-regions-title").first();
        numberOfCard = getCardNumber(resultPageSize);
    }

    public Set<String> getUrls() {
        init();
        Set<String> result = new HashSet<String>();
        int page = numberOfCard / 10;
        page = page == 0 ? 1 : page;
        for (int i = 1; i <= page; i++) {
            String urlContent = getUrlContent(i, url);
            Document document = Jsoup.parse(urlContent);
            Elements cards = document.select("div.picBox");
            for (Element div : cards) {
                Elements child = div.select("a[href].listEstateWithPicOnPicture");
                if (child.isEmpty()) {
                    child = div.select("a[href].listEstateWithoutPicOnPicture");
                }
                String link = BASE_URL + child.attr("href");
                if (IngatlanDataBaseMock.getUrls().contains(link)) {
                } else {
                    result.add(link);
                }
            }
        }
        System.out.println("dh.hu Új URL száma : " + result.size());
        return result;
    }

    public int getCardNumber(Element resultPageSize) {
        int result = 0;
        String text = resultPageSize.ownText();
        String[] split = text.split(" ");
        for (String i : split) {
            try {
                result = Integer.parseInt(i);
                break;
            } catch (NumberFormatException e) {
            }
        }
        return result;
    }
}
