import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Pusinszky on 2017. 06. 05..
 */
public class IngatlanCom extends IngatlanStart implements Ingatlan {

    private static final String BASE_URL = "http://ingatlan.com";
    private static final String START_ROUTE = "/szukites/elado+lakas+csak-kepes+debrecen+";
    private static final String ROUTE_PRICE = "9-11-mFt";
    private static final String END_ROUTE = "+35-m2-felett?page=";
    public static String url = BASE_URL + START_ROUTE + ROUTE_PRICE + END_ROUTE;
    private int numberOfCard;

    public void init() {
        System.out.println("URL : " + url);
        String urlContent = getUrlContent(1, url);
        Document document = Jsoup.parse(urlContent);
        Element resultPageSize = document.select("span.results__number__count").first();
        numberOfCard = getCardNumber(resultPageSize);
    }

    public int getCardNumber(Element resultPageSize) {
        int result = 0;
        try {
            result = Integer.valueOf(resultPageSize.ownText());
        }catch (NumberFormatException e){
        }
        return result;
    }

    public Set<String> getUrls() {
        init();
        Set<String> result = new HashSet<String>();
        int page = numberOfCard / 20;
        for (int i = 1; i < page; i++) {
            String urlContent = getUrlContent(i,url);
            Document document = Jsoup.parse(urlContent);
            Elements cards = document.select("div.listing__card");
            for (Element div : cards) {
                Elements child = div.select("a[href].listing__link");
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
