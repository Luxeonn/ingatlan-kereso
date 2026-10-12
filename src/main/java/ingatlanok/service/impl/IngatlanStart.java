package ingatlanok.service.impl;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;

/**
 * Created by Pusinszky on 2017. 06. 17..
 */
public abstract class IngatlanStart {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final int TIMEOUT_MS = 15000;

    public abstract void init();

    public abstract int getCardNumber(Element resultPageSize);

    public Document getUrlContent(int page, String url) {
        try {
            String s = url + page;
            return Jsoup.connect(s)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
