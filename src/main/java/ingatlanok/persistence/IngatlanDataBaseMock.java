package ingatlanok.persistence;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Pusinszky on 2017. 06. 17..
 */
public class IngatlanDataBaseMock {

    public static void addUrl(String url) {
        if (url != null && !IngatlanRepository.getInstance().containsUrl(url)) {
            // Keep in sync with repository
        }
    }

    public static Set<String> getUrls() {
        return IngatlanRepository.getInstance().getAllUrls();
    }
}
