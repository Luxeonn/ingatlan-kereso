package ingatlanok.service.impl;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;

/**
 * Created by Pusinszky on 2017. 06. 17..
 */
public abstract class IngatlanStart {

    public abstract void init();

    public abstract int getCardNumber(Element resultPageSize);

    public Document getUrlContent(int page, String url) {
        try {
            String s = url + page;
            return Jsoup.connect(s).validateTLSCertificates(false).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
