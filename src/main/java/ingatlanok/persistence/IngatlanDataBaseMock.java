package ingatlanok.persistence;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Pusinszky on 2017. 06. 17..
 */
public class IngatlanDataBaseMock {


    private static Set<String> urls = new HashSet<String>();

    public static void addUrl(String url){
        urls.add(url);
    }

    public static Set<String> getUrls(){
        return urls;
    }
}
