package ingatlanok.service.impl;

import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

/**
 * Created by Pusinszky on 2017. 06. 17..
 */
public abstract class IngatlanStart {

    public abstract void init();

    public abstract int getCardNumber(Element resultPageSize);

    public String getUrlContent(int page, String url) {
        String out = null;
        try {
            String s = url + page;
            out = new Scanner(new URL(s).openStream(), "UTF-8").useDelimiter("\\A").next();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out;
    }

}
