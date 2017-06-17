import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

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
        String urlContent = getUrlContent(1,url);
        Document document = Jsoup.parse(urlContent);
        Element resultPageSize = document.select("span.results__number__count").first();
        numberOfCard = Integer.valueOf(resultPageSize.ownText());
    }

    public Set<String> getUrls() {
        return null;
    }
}
